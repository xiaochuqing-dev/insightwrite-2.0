package com.insightwrite.controller;

import com.insightwrite.service.ArticleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping("/articles")
    public ResponseEntity<?> getArticles(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(articleService.getArticles(category, page, size));
    }

    @GetMapping("/articles/{id}")
    public ResponseEntity<?> getArticleDetail(@PathVariable Integer id) {
        return ResponseEntity.ok(articleService.getArticleDetail(id));
    }
}
