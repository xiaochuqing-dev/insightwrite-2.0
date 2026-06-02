package com.insightwrite.service;

import com.insightwrite.entity.AnalysisTask;
import com.insightwrite.entity.Favorite;
import com.insightwrite.entity.KnowledgeEntry;
import com.insightwrite.entity.KnowledgeFavorite;
import com.insightwrite.entity.User;
import com.insightwrite.repository.AnalysisTaskRepository;
import com.insightwrite.repository.FavoriteRepository;
import com.insightwrite.repository.KnowledgeEntryRepository;
import com.insightwrite.repository.KnowledgeFavoriteRepository;
import com.insightwrite.util.PageUtils;
import com.insightwrite.util.PreviewUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final AnalysisTaskRepository analysisTaskRepository;
    private final KnowledgeEntryRepository knowledgeEntryRepository;
    private final KnowledgeFavoriteRepository knowledgeFavoriteRepository;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           AnalysisTaskRepository analysisTaskRepository,
                           KnowledgeEntryRepository knowledgeEntryRepository,
                           KnowledgeFavoriteRepository knowledgeFavoriteRepository) {
        this.favoriteRepository = favoriteRepository;
        this.analysisTaskRepository = analysisTaskRepository;
        this.knowledgeEntryRepository = knowledgeEntryRepository;
        this.knowledgeFavoriteRepository = knowledgeFavoriteRepository;
    }

    public Map<String, Object> getFavorites(Integer userId, int page, int size) {
        Page<Favorite> pg = favoriteRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page - 1, size));

        List<Map<String, Object>> items = new ArrayList<>();
        for (Favorite f : pg.getContent()) {
            AnalysisTask task = f.getTask();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "analysis");
            item.put("task_id", task.getId());
            item.put("mode", task.getMode());
            item.put("word_count", task.getWordCount());
            item.put("preview", PreviewUtils.truncate(task.getEssayText()));
            item.put("created_at", f.getCreatedAt() != null ? f.getCreatedAt().toString() : null);
            items.add(item);
        }

        return PageUtils.response(pg, page, items);
    }

    public Map<String, Object> getKnowledgeFavorites(Integer userId, int page, int size) {
        Page<KnowledgeFavorite> pg = knowledgeFavoriteRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page - 1, size));

        List<Map<String, Object>> items = new ArrayList<>();
        for (KnowledgeFavorite f : pg.getContent()) {
            KnowledgeEntry entry = f.getKnowledgeEntry();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "knowledge");
            item.put("knowledge_id", entry.getId());
            item.put("title", entry.getTitle());
            item.put("category", entry.getCategory());
            item.put("tags", entry.getTags());
            item.put("preview", PreviewUtils.truncate(entry.getContent()));
            item.put("created_at", f.getCreatedAt() != null ? f.getCreatedAt().toString() : null);
            items.add(item);
        }

        return PageUtils.response(pg, page, items);
    }

    public Map<String, Object> addAnalysisFavorite(Integer userId, Integer taskId) {
        AnalysisTask task = analysisTaskRepository.findByIdAndUser_Id(taskId, userId)
                .orElseThrow(() -> new RuntimeException("分析记录不存在"));

        Optional<Favorite> existing = favoriteRepository.findByUserIdAndTaskId(userId, taskId);
        if (existing.isEmpty()) {
            Favorite f = new Favorite();
            User user = new User();
            user.setId(userId);
            f.setUser(user);
            f.setTask(task);
            f.setCreatedAt(LocalDateTime.now());
            favoriteRepository.save(f);
        }
        task.setIsFavorite(true);
        analysisTaskRepository.save(task);
        return Map.of("is_favorite", true);
    }

    public Map<String, Object> removeAnalysisFavorite(Integer userId, Integer taskId) {
        AnalysisTask task = analysisTaskRepository.findByIdAndUser_Id(taskId, userId)
                .orElseThrow(() -> new RuntimeException("分析记录不存在"));

        favoriteRepository.findByUserIdAndTaskId(userId, taskId).ifPresent(favoriteRepository::delete);
        task.setIsFavorite(false);
        analysisTaskRepository.save(task);
        return Map.of("is_favorite", false);
    }

    public Map<String, Object> addKnowledgeFavorite(Integer userId, Integer knowledgeId) {
        KnowledgeEntry entry = knowledgeEntryRepository.findById(knowledgeId)
                .orElseThrow(() -> new RuntimeException("知识条目不存在"));

        if (!knowledgeFavoriteRepository.existsByUserIdAndKnowledgeEntryId(userId, knowledgeId)) {
            KnowledgeFavorite f = new KnowledgeFavorite();
            User user = new User();
            user.setId(userId);
            f.setUser(user);
            f.setKnowledgeEntry(entry);
            f.setCreatedAt(LocalDateTime.now());
            knowledgeFavoriteRepository.save(f);
        }
        return Map.of("is_favorite", true);
    }

    public Map<String, Object> removeKnowledgeFavorite(Integer userId, Integer knowledgeId) {
        knowledgeFavoriteRepository.findByUserIdAndKnowledgeEntryId(userId, knowledgeId)
                .ifPresent(knowledgeFavoriteRepository::delete);
        return Map.of("is_favorite", false);
    }
}
