package com.insightwrite.controller;

import com.insightwrite.service.HistoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Integer userId = ApiResponses.currentUserId(request);
        if (userId == null) {
            return ApiResponses.unauthorized();
        }
        return ResponseEntity.ok(historyService.getHistory(userId, page, size));
    }

    @GetMapping("/history/{taskId}")
    public ResponseEntity<?> getHistoryDetail(@PathVariable Integer taskId,
                                              HttpServletRequest request) {
        Integer userId = ApiResponses.currentUserId(request);
        if (userId == null) {
            return ApiResponses.unauthorized();
        }
        return ResponseEntity.ok(historyService.getHistoryDetail(taskId, userId));
    }
}
