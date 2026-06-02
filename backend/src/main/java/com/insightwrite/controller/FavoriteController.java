package com.insightwrite.controller;

import com.insightwrite.service.FavoriteService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping("/favorites")
    public ResponseEntity<?> getFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Integer userId = ApiResponses.currentUserId(request);
        if (userId == null) {
            return ApiResponses.unauthorized();
        }
        return ResponseEntity.ok(favoriteService.getFavorites(userId, page, size));
    }

    @GetMapping("/favorites/knowledge")
    public ResponseEntity<?> getKnowledgeFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Integer userId = ApiResponses.currentUserId(request);
        if (userId == null) {
            return ApiResponses.unauthorized();
        }
        return ResponseEntity.ok(favoriteService.getKnowledgeFavorites(userId, page, size));
    }

    @PostMapping("/history/{taskId}/favorite")
    public ResponseEntity<?> toggleFavoriteOn(@PathVariable Integer taskId,
                                              HttpServletRequest request) {
        Integer userId = ApiResponses.currentUserId(request);
        if (userId == null) {
            return ApiResponses.unauthorized();
        }
        return ResponseEntity.ok(favoriteService.addAnalysisFavorite(userId, taskId));
    }

    @DeleteMapping("/history/{taskId}/favorite")
    public ResponseEntity<?> toggleFavoriteOff(@PathVariable Integer taskId,
                                               HttpServletRequest request) {
        Integer userId = ApiResponses.currentUserId(request);
        if (userId == null) {
            return ApiResponses.unauthorized();
        }
        return ResponseEntity.ok(favoriteService.removeAnalysisFavorite(userId, taskId));
    }

    @PostMapping("/knowledge/{knowledgeId}/favorite")
    public ResponseEntity<?> favoriteKnowledge(@PathVariable Integer knowledgeId,
                                               HttpServletRequest request) {
        Integer userId = ApiResponses.currentUserId(request);
        if (userId == null) {
            return ApiResponses.unauthorized();
        }
        return ResponseEntity.ok(favoriteService.addKnowledgeFavorite(userId, knowledgeId));
    }

    @DeleteMapping("/knowledge/{knowledgeId}/favorite")
    public ResponseEntity<?> unfavoriteKnowledge(@PathVariable Integer knowledgeId,
                                                 HttpServletRequest request) {
        Integer userId = ApiResponses.currentUserId(request);
        if (userId == null) {
            return ApiResponses.unauthorized();
        }
        return ResponseEntity.ok(favoriteService.removeKnowledgeFavorite(userId, knowledgeId));
    }
}
