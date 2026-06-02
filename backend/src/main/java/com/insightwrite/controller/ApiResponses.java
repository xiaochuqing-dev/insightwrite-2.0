package com.insightwrite.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

import java.util.Map;

final class ApiResponses {

    private ApiResponses() {
    }

    static Integer currentUserId(HttpServletRequest request) {
        return (Integer) request.getAttribute("userId");
    }

    static ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(401).body(Map.of("error", "未登录"));
    }
}
