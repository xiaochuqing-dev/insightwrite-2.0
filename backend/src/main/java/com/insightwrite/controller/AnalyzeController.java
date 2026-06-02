package com.insightwrite.controller;

import com.insightwrite.entity.AnalysisTask;
import com.insightwrite.service.AnalyzeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AnalyzeController {

    private final AnalyzeService analyzeService;

    public AnalyzeController(AnalyzeService analyzeService) {
        this.analyzeService = analyzeService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> submitAnalyze(@RequestBody Map<String, Object> body,
                                           HttpServletRequest request) {
        Integer userId = ApiResponses.currentUserId(request);
        if (userId == null) {
            return ApiResponses.unauthorized();
        }

        String essayText = asString(body.get("essay_text"), "");
        String mode = asString(body.get("mode"), "basic");
        String customReq = asString(body.get("custom_requirement"), "");
        String topic = asString(body.get("topic"), "");
        boolean generateEssay = Boolean.parseBoolean(asString(body.get("generate_essay"), "true"));
        Map<String, Object> analysisPreferences = asMap(body.get("analysis_preferences"));

        if (essayText == null || essayText.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "文章内容不能为空"));
        }

        AnalysisTask task = analyzeService.submitAnalysis(userId, essayText, mode, customReq, topic, generateEssay, analysisPreferences);
        return ResponseEntity.ok(Map.of(
                "task_id", task.getId(),
                "status", task.getStatus()
        ));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of();
    }

    private String asString(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    @GetMapping("/analyze/{taskId}/status")
    public ResponseEntity<?> getTaskStatus(@PathVariable Integer taskId,
                                           HttpServletRequest request) {
        Integer userId = ApiResponses.currentUserId(request);
        if (userId == null) {
            return ApiResponses.unauthorized();
        }
        return ResponseEntity.ok(analyzeService.getTaskStatus(taskId, userId));
    }

    @GetMapping("/credits/costs")
    public ResponseEntity<?> getCreditCosts() {
        return ResponseEntity.ok(analyzeService.getCreditCosts());
    }
}
