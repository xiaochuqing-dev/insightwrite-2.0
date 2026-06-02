package com.insightwrite.service;

import com.insightwrite.entity.Article;
import com.insightwrite.repository.ArticleRepository;
import com.insightwrite.util.PageUtils;
import com.insightwrite.util.PreviewUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;

    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public Map<String, Object> getArticles(String category, int page, int size) {
        Page<Article> pg;
        if (category != null && !category.isBlank()) {
            pg = articleRepository.findByCategoryOrderByCreatedAtDesc(category, PageRequest.of(page - 1, size));
        } else {
            pg = articleRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page - 1, size));
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (Article a : pg.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("title", a.getTitle());
            item.put("category", a.getCategory());
            item.put("source", a.getSource());
            item.put("word_count", a.getWordCount());
            item.put("view_count", a.getViewCount());
            item.put("preview", PreviewUtils.truncate(a.getContent()));
            item.put("created_at", a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);
            items.add(item);
        }

        return PageUtils.response(pg, page, items);
    }

    public Map<String, Object> getArticleDetail(Integer id) {
        Article a = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在"));
        a.setViewCount(a.getViewCount() != null ? a.getViewCount() + 1 : 1);
        articleRepository.save(a);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", a.getId());
        result.put("title", a.getTitle());
        result.put("content", a.getContent());
        result.put("category", a.getCategory());
        result.put("source", a.getSource());
        result.put("word_count", a.getWordCount());
        result.put("view_count", a.getViewCount());
        result.put("created_at", a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);
        return result;
    }
}
