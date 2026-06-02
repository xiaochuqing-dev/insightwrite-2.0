package com.insightwrite.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthAttemptLimiter {

    private static final int MAX_LOGIN_FAILURES = 5;
    private static final Duration LOGIN_BLOCK_DURATION = Duration.ofMinutes(15);
    private static final int MAX_SEND_CODE_REQUESTS = 10;
    private static final Duration SEND_CODE_WINDOW = Duration.ofMinutes(10);

    private final ConcurrentHashMap<String, AttemptState> loginAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CodeRequestState> sendCodeRequests = new ConcurrentHashMap<>();

    public void checkLoginAllowed(String email, String clientKey) {
        String key = key(email, clientKey);
        AttemptState state = loginAttempts.get(key);
        if (state == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (state.blockedUntil != null && state.blockedUntil.isAfter(now)) {
            throw new RuntimeException("登录尝试次数过多，请稍后再试");
        }
        if (state.blockedUntil != null) {
            loginAttempts.remove(key);
        }
    }

    public void recordLoginFailure(String email, String clientKey) {
        String key = key(email, clientKey);
        LocalDateTime now = LocalDateTime.now();
        loginAttempts.compute(key, (ignored, state) -> {
            AttemptState current = state == null ? new AttemptState() : state;
            current.failures += 1;
            current.lastFailureAt = now;
            if (current.failures >= MAX_LOGIN_FAILURES) {
                current.blockedUntil = now.plus(LOGIN_BLOCK_DURATION);
            }
            return current;
        });
    }

    public void resetLoginFailures(String email, String clientKey) {
        loginAttempts.remove(key(email, clientKey));
    }

    public void checkSendCodeAllowed(String clientKey) {
        String key = clientKey == null || clientKey.isBlank() ? "unknown" : clientKey.trim();
        LocalDateTime now = LocalDateTime.now();
        sendCodeRequests.compute(key, (ignored, state) -> {
            CodeRequestState current = state;
            if (current == null || current.windowStartedAt.plus(SEND_CODE_WINDOW).isBefore(now)) {
                current = new CodeRequestState();
                current.windowStartedAt = now;
            }
            current.requests += 1;
            if (current.requests > MAX_SEND_CODE_REQUESTS) {
                throw new RuntimeException("验证码发送过于频繁，请稍后再试");
            }
            return current;
        });
    }

    private String key(String email, String clientKey) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String normalizedClient = clientKey == null || clientKey.isBlank() ? "unknown" : clientKey.trim();
        return normalizedEmail + "|" + normalizedClient;
    }

    private static class AttemptState {
        private int failures;
        private LocalDateTime lastFailureAt;
        private LocalDateTime blockedUntil;
    }

    private static class CodeRequestState {
        private int requests;
        private LocalDateTime windowStartedAt;
    }
}
