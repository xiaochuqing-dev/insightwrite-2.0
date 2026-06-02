package com.insightwrite.service;

import com.insightwrite.entity.AnalysisTask;
import com.insightwrite.repository.AnalysisTaskRepository;
import com.insightwrite.util.PageUtils;
import com.insightwrite.util.PreviewUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class HistoryService {

    private final AnalysisTaskRepository analysisTaskRepository;

    public HistoryService(AnalysisTaskRepository analysisTaskRepository) {
        this.analysisTaskRepository = analysisTaskRepository;
    }

    public Map<String, Object> getHistory(Integer userId, int page, int size) {
        Page<AnalysisTask> pg = analysisTaskRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page - 1, size));

        List<Map<String, Object>> items = new ArrayList<>();
        for (AnalysisTask task : pg.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", task.getId());
            item.put("mode", task.getMode());
            item.put("word_count", task.getWordCount());
            item.put("status", task.getStatus());
            item.put("is_favorite", task.getIsFavorite());
            item.put("preview", PreviewUtils.truncate(task.getEssayText()));
            item.put("created_at", task.getCreatedAt() != null ? task.getCreatedAt().toString() : null);
            items.add(item);
        }

        return PageUtils.response(pg, page, items);
    }

    public Map<String, Object> getHistoryDetail(Integer taskId, Integer userId) {
        AnalysisTask task = analysisTaskRepository.findByIdAndUser_Id(taskId, userId)
                .orElseThrow(() -> new RuntimeException("分析记录不存在"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", task.getId());
        result.put("status", task.getStatus());
        result.put("mode", task.getMode());
        result.put("word_count", task.getWordCount());
        result.put("essay_text", task.getEssayText());
        result.put("custom_requirement", task.getCustomRequirement());
        result.put("result_text", task.getResultText());
        result.put("error_message", task.getErrorMessage());
        result.put("is_favorite", task.getIsFavorite());
        result.put("created_at", task.getCreatedAt() != null ? task.getCreatedAt().toString() : null);
        result.put("completed_at", task.getCompletedAt() != null ? task.getCompletedAt().toString() : null);
        return result;
    }
}
