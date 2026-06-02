package com.insightwrite.service;

import com.insightwrite.entity.AnalysisTask;
import com.insightwrite.entity.CreditTransaction;
import com.insightwrite.entity.User;
import com.insightwrite.repository.AnalysisTaskRepository;
import com.insightwrite.repository.CreditTransactionRepository;
import com.insightwrite.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AnalyzeService {
    public static final int TOPIC_MAX_CHARS = 160;
    public static final int TOPIC_MAX_WORDS = 30;
    public static final int CUSTOM_REQUIREMENT_MAX_CHARS = 120;
    public static final Map<String, Integer> MODE_WORD_LIMITS = Map.of(
            "basic", 800,
            "quality", 900,
            "deep", 1200
    );

    private final AnalysisTaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CreditTransactionRepository creditRepository;
    private final DeepSeekService deepSeekService;
    private final AsyncAnalysisRunner asyncRunner;

    @Value("${insightwrite.credits.costs.basic}")
    private int costBasic;
    @Value("${insightwrite.credits.costs.quality}")
    private int costQuality;
    @Value("${insightwrite.credits.costs.deep}")
    private int costDeep;

    public AnalyzeService(AnalysisTaskRepository taskRepository,
                          UserRepository userRepository,
                          CreditTransactionRepository creditRepository,
                          DeepSeekService deepSeekService,
                          AsyncAnalysisRunner asyncRunner) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.creditRepository = creditRepository;
        this.deepSeekService = deepSeekService;
        this.asyncRunner = asyncRunner;
    }

    public Map<String, Integer> getCreditCosts() {
        return Map.of("basic", costBasic, "quality", costQuality, "deep", costDeep);
    }

    public int validateInput(String mode, String essayText, String topic, String customRequirement) {
        String normalizedMode = MODE_WORD_LIMITS.containsKey(mode) ? mode : "basic";
        String text = essayText == null ? "" : essayText.trim();
        if (text.isBlank()) {
            throw new IllegalArgumentException("文章内容不能为空");
        }

        int wordCount = countWords(text);
        int limit = MODE_WORD_LIMITS.get(normalizedMode);
        if (wordCount > limit) {
            throw new IllegalArgumentException("当前模式最多支持 " + limit + " 词，请精简后再提交");
        }

        String normalizedTopic = topic == null ? "" : topic.trim();
        if (normalizedTopic.length() > TOPIC_MAX_CHARS || countWords(normalizedTopic) > TOPIC_MAX_WORDS) {
            throw new IllegalArgumentException("标题/作文题目最多 " + TOPIC_MAX_WORDS + " 词或 " + TOPIC_MAX_CHARS + " 个字符");
        }

        String normalizedCustom = customRequirement == null ? "" : customRequirement.trim();
        if (normalizedCustom.length() > CUSTOM_REQUIREMENT_MAX_CHARS) {
            throw new IllegalArgumentException("自定义分析重点最多 " + CUSTOM_REQUIREMENT_MAX_CHARS + " 个字符");
        }

        return wordCount;
    }

    private int countWords(String value) {
        String text = value == null ? "" : value.trim();
        return text.isBlank() ? 0 : text.split("\\s+").length;
    }

    public AnalysisTask submitAnalysis(Integer userId, String essayText, String mode, String customRequirement, String topic, boolean generateEssay) {
        return submitAnalysis(userId, essayText, mode, customRequirement, topic, generateEssay, Map.of());
    }

    @Transactional
    public AnalysisTask submitAnalysis(Integer userId, String essayText, String mode, String customRequirement, String topic,
                                       boolean generateEssay, Map<String, Object> analysisPreferences) {
        int wordCount = validateInput(mode, essayText, topic, customRequirement);
        int cost = getCreditCosts().getOrDefault(mode, 50);

        int deducted = userRepository.deductCreditsIfEnough(userId, cost);
        if (deducted == 0) {
            User current = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            int credits = current.getCredits() != null ? current.getCredits() : 0;
            throw new RuntimeException("积分不足，本次分析需要 " + cost + " 积分，当前剩余 " + credits + " 积分");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        CreditTransaction tx = new CreditTransaction();
        tx.setUser(user);
        tx.setAmount(-cost);
        tx.setReason("deduct_" + mode);
        tx.setBalanceAfter(user.getCredits());
        tx.setCreatedAt(LocalDateTime.now());
        creditRepository.save(tx);

        // 创建任务
        AnalysisTask task = new AnalysisTask();
        task.setUser(user);
        task.setEssayText(essayText);
        task.setMode(mode);
        task.setCustomRequirement(customRequirement != null ? customRequirement : "");
        task.setStatus("pending");
        task.setIsFavorite(false);
        task.setWordCount(wordCount);
        task.setCreatedAt(LocalDateTime.now());
        taskRepository.save(task);

        Runnable startAsync = () -> asyncRunner.run(task.getId(), topic, generateEssay, analysisPreferences);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    startAsync.run();
                }
            });
        } else {
            startAsync.run();
        }

        return task;
    }

    // 查询状态（前端轮询）
    public Map<String, Object> getTaskStatus(Integer taskId, Integer userId) {
        AnalysisTask task = taskRepository.findByIdAndUser_Id(taskId, userId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", task.getStatus());
        resp.put("mode", task.getMode());
        resp.put("created_at", task.getCreatedAt() != null ? task.getCreatedAt().toString() : null);
        resp.put("is_favorite", task.getIsFavorite());
        resp.put("word_count", task.getWordCount());

        if ("completed".equals(task.getStatus())) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("original_text", task.getEssayText());
            result.put("revised_text", deepSeekService.stripAnnotationBlock(task.getResultText()));

            // 从已保存的 result_text 解析标注，不调 API
            List<Map<String, Object>> anns = deepSeekService.parseAnnotationsOnly(
                    task.getEssayText(), task.getResultText());
            result.put("annotations", anns);

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("word_count", task.getWordCount());
            summary.put("changed_count", anns.size());
            result.put("summary", summary);

            resp.put("result", result);
        } else if ("failed".equals(task.getStatus())) {
            resp.put("error_message", task.getErrorMessage());
        }

        return resp;
    }
}
