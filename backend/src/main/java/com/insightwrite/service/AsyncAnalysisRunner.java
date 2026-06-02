package com.insightwrite.service;

import com.insightwrite.entity.AnalysisTask;
import com.insightwrite.entity.CreditTransaction;
import com.insightwrite.entity.User;
import com.insightwrite.repository.AnalysisTaskRepository;
import com.insightwrite.repository.CreditTransactionRepository;
import com.insightwrite.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AsyncAnalysisRunner {

    private static final Logger log = LoggerFactory.getLogger(AsyncAnalysisRunner.class);

    private final AnalysisTaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CreditTransactionRepository creditRepository;
    private final DeepSeekService deepSeekService;

    @Value("${insightwrite.credits.costs.basic}")
    private int costBasic;
    @Value("${insightwrite.credits.costs.quality}")
    private int costQuality;
    @Value("${insightwrite.credits.costs.deep}")
    private int costDeep;

    public AsyncAnalysisRunner(AnalysisTaskRepository taskRepository,
                               UserRepository userRepository,
                               CreditTransactionRepository creditRepository,
                               DeepSeekService deepSeekService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.creditRepository = creditRepository;
        this.deepSeekService = deepSeekService;
    }

    @Async("analysisExecutor")
    public void run(Integer taskId, String topic, boolean generateEssay) {
        run(taskId, topic, generateEssay, Map.of());
    }

    @Async("analysisExecutor")
    public void run(Integer taskId, String topic, boolean generateEssay, Map<String, Object> analysisPreferences) {
        log.info("Background analysis started taskId={}", taskId);
        try {
            doProcess(taskId, topic, generateEssay, analysisPreferences);
        } catch (Exception e) {
            log.error("processTask failed taskId={}: {}", taskId, e.getMessage(), e);
            try {
                AnalysisTask task = taskRepository.findById(taskId).orElse(null);
                if (task != null && !"completed".equals(task.getStatus())) {
                    task.setStatus("failed");
                    task.setErrorMessage("分析服务异常: " + e.getMessage());
                    taskRepository.save(task);
                }
            } catch (Exception ignored) {
            }
        }
    }

    void doProcess(Integer taskId, String topic, boolean generateEssay, Map<String, Object> analysisPreferences) {
        AnalysisTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            log.error("Task not found taskId={}", taskId);
            return;
        }

        task.setStatus("processing");
        task = taskRepository.save(task);
        log.info("Task processing taskId={}", taskId);

        try {
            DeepSeekService.AnalysisResult result = deepSeekService.analyze(
                    task.getEssayText(), task.getMode(), task.getCustomRequirement(),
                    topic, generateEssay, analysisPreferences);

            task = taskRepository.findById(taskId).orElse(null);
            if (task == null) {
                return;
            }

            task.setStatus("completed");
            task.setResultText(result.fullText);
            task.setCompletedAt(LocalDateTime.now());
            taskRepository.save(task);
            log.info("Analysis completed taskId={}, annotations={}", taskId, result.annotations.size());

        } catch (Exception e) {
            log.error("Analysis failed taskId={}: {}", taskId, e.getMessage());

            task = taskRepository.findById(taskId).orElse(null);
            if (task == null) {
                return;
            }

            task.setStatus("failed");
            task.setErrorMessage(e.getMessage());
            taskRepository.save(task);

            User user = task.getUser();
            if (user != null) {
                Map<String, Integer> costs = Map.of("basic", costBasic, "quality", costQuality, "deep", costDeep);
                int cost = costs.getOrDefault(task.getMode(), 50);
                userRepository.addCredits(user.getId(), cost);
                User refreshedUser = userRepository.findById(user.getId()).orElse(user);

                CreditTransaction tx = new CreditTransaction();
                tx.setUser(refreshedUser);
                tx.setAmount(cost);
                tx.setReason("refund_" + task.getMode() + "_task_" + taskId);
                tx.setBalanceAfter(refreshedUser.getCredits());
                tx.setCreatedAt(LocalDateTime.now());
                creditRepository.save(tx);
                log.info("Credits refunded userId={}, amount={}", user.getId(), cost);
            }
        }
    }
}
