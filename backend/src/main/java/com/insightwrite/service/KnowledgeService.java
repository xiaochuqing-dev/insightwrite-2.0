package com.insightwrite.service;

import com.insightwrite.entity.KnowledgeEntry;
import com.insightwrite.repository.KnowledgeFavoriteRepository;
import com.insightwrite.repository.KnowledgeEntryRepository;
import com.insightwrite.util.PageUtils;
import com.insightwrite.util.PreviewUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class KnowledgeService {

    private final KnowledgeEntryRepository knowledgeEntryRepository;
    private final KnowledgeFavoriteRepository knowledgeFavoriteRepository;

    public KnowledgeService(KnowledgeEntryRepository knowledgeEntryRepository,
                            KnowledgeFavoriteRepository knowledgeFavoriteRepository) {
        this.knowledgeEntryRepository = knowledgeEntryRepository;
        this.knowledgeFavoriteRepository = knowledgeFavoriteRepository;
    }

    public Map<String, Object> getEntries(String category, String search, int page, int size) {
        Page<KnowledgeEntry> pg;
        boolean hasCategory = category != null && !category.isBlank();
        boolean hasSearch = search != null && !search.isBlank();

        if (hasCategory && hasSearch) {
            pg = knowledgeEntryRepository.findByCategoryAndSearch(
                    category, search, PageRequest.of(page - 1, size));
        } else if (hasCategory) {
            pg = knowledgeEntryRepository.findByCategoryOrderByCreatedAtDesc(
                    category, PageRequest.of(page - 1, size));
        } else if (hasSearch) {
            pg = knowledgeEntryRepository.findByTitleContainingOrTagsContainingOrderByCreatedAtDesc(
                    search, search, PageRequest.of(page - 1, size));
        } else {
            pg = knowledgeEntryRepository.findAll(
                    PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (KnowledgeEntry e : pg.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", e.getId());
            item.put("category", e.getCategory());
            item.put("title", e.getTitle());
            item.put("tags", e.getTags());
            item.put("view_count", e.getViewCount());
            item.put("preview", PreviewUtils.truncate(e.getContent()));
            item.put("created_at", e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
            items.add(item);
        }

        return PageUtils.response(pg, page, items);
    }

    public Map<String, Object> getEntryDetail(Integer id) {
        return getEntryDetail(id, null);
    }

    public Map<String, Object> getEntryDetail(Integer id, Integer userId) {
        KnowledgeEntry e = knowledgeEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("知识条目不存在"));
        e.setViewCount(e.getViewCount() != null ? e.getViewCount() + 1 : 1);
        knowledgeEntryRepository.save(e);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", e.getId());
        result.put("category", e.getCategory());
        result.put("title", e.getTitle());
        result.put("content", e.getContent());
        result.put("tags", e.getTags());
        result.put("view_count", e.getViewCount());
        result.put("is_favorite", userId != null && knowledgeFavoriteRepository.existsByUserIdAndKnowledgeEntryId(userId, id));
        result.put("created_at", e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        return result;
    }
}
