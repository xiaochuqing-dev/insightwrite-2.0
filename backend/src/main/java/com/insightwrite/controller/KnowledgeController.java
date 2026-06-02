package com.insightwrite.controller;

import com.insightwrite.service.KnowledgeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping("/knowledge")
    public ResponseEntity<?> getEntries(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(knowledgeService.getEntries(category, search, page, size));
    }

    @GetMapping("/knowledge/{id}")
    public ResponseEntity<?> getEntryDetail(@PathVariable Integer id,
                                            HttpServletRequest request) {
        return ResponseEntity.ok(knowledgeService.getEntryDetail(id, ApiResponses.currentUserId(request)));
    }
}
