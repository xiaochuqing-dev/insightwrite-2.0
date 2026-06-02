package com.insightwrite.service;

import com.insightwrite.dto.*;
import com.insightwrite.entity.User;
import com.insightwrite.repository.UserRepository;
import com.insightwrite.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthService {

    private static final String LOGIN_FAILED_MESSAGE = "邮箱或密码错误";
    private static final String REGISTER_FAILED_MESSAGE = "注册信息无效或验证码错误";
    private static final String RESET_FAILED_MESSAGE = "密码重置请求无效或已过期";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final AuthAttemptLimiter authAttemptLimiter;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       EmailService emailService,
                       AuthAttemptLimiter authAttemptLimiter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.authAttemptLimiter = authAttemptLimiter;
    }

    // 注册
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException(REGISTER_FAILED_MESSAGE);
        }

        // 校验验证码
        emailService.validateCode(req.getEmail(), req.getCode(), "register");

        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setCredits(600);
        user.setCreatedAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user);
        return buildResponse(user, token);
    }

    // 登录
    @Transactional
    public AuthResponse login(LoginRequest req) {
        return login(req, null);
    }

    @Transactional
    public AuthResponse login(LoginRequest req, String clientKey) {
        if (authAttemptLimiter != null) {
            authAttemptLimiter.checkLoginAllowed(req.getEmail(), clientKey);
        }

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> {
                    recordLoginFailure(req.getEmail(), clientKey);
                    return new RuntimeException(LOGIN_FAILED_MESSAGE);
                });

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            recordLoginFailure(req.getEmail(), clientKey);
            throw new RuntimeException(LOGIN_FAILED_MESSAGE);
        }

        if (authAttemptLimiter != null) {
            authAttemptLimiter.resetLoginFailures(req.getEmail(), clientKey);
        }
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user);
        return buildResponse(user, token);
    }

    // 重置密码
    @Transactional
    public void resetPassword(ForgotPasswordRequest req) {
        emailService.validateCode(req.getEmail(), req.getCode(), "reset_password");

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException(RESET_FAILED_MESSAGE));

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setTokenVersion((user.getTokenVersion() != null ? user.getTokenVersion() : 0) + 1);
        userRepository.save(user);
    }

    // 获取用户信息
    public Map<String, Object> getUserInfo(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Map<String, Object> info = new java.util.LinkedHashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("email", user.getEmail());
        info.put("credits", user.getCredits());
        info.put("avatar", user.getAvatar());
        info.put("created_at", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        info.put("last_login_at", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null);
        return info;
    }

    // 更新昵称
    public void updateUsername(Integer userId, String username) {
        if (username == null || username.isBlank() || username.length() < 2 || username.length() > 20) {
            throw new RuntimeException("昵称长度需为 2-20 个字符");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setUsername(username.trim());
        userRepository.save(user);
    }

    public Map<String, Object> updateAvatar(Integer userId, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setAvatar(avatarUrl);
        userRepository.save(user);
        return Map.of("avatar", avatarUrl);
    }

    public void validateSendCodeRequest(String email, String purpose) {
        validateSendCodeRequest(email, purpose, null);
    }

    public void validateSendCodeRequest(String email, String purpose, String clientKey) {
        shouldSendVerificationCode(email, purpose, clientKey);
    }

    public boolean shouldSendVerificationCode(String email, String purpose, String clientKey) {
        String normalizedPurpose = normalizePurpose(purpose);
        if (!"register".equals(normalizedPurpose) && !"reset_password".equals(normalizedPurpose)) {
            throw new RuntimeException("验证码用途不正确");
        }
        if (authAttemptLimiter != null) {
            authAttemptLimiter.checkSendCodeAllowed(clientKey);
        }
        boolean exists = userRepository.existsByEmail(email);
        if ("register".equals(normalizedPurpose)) {
            return !exists;
        }
        return exists;
    }

    private void recordLoginFailure(String email, String clientKey) {
        if (authAttemptLimiter != null) {
            authAttemptLimiter.recordLoginFailure(email, clientKey);
        }
    }

    private String normalizePurpose(String purpose) {
        if ("forgot_password".equals(purpose)) {
            return "reset_password";
        }
        return purpose;
    }

    private AuthResponse buildResponse(User user, String token) {
        AuthResponse.UserInfo info = new AuthResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getCredits(),
                user.getAvatar()
        );
        return new AuthResponse(token, info);
    }
}
