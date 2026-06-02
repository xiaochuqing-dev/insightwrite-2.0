package com.insightwrite.controller;

import com.insightwrite.dto.*;
import com.insightwrite.service.AuthService;
import com.insightwrite.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    public AuthController(AuthService authService, EmailService emailService) {
        this.authService = authService;
        this.emailService = emailService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req,
                                   HttpServletRequest request) {
        return ResponseEntity.ok(authService.login(req, clientKey(request)));
    }

    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@Valid @RequestBody SendCodeRequest req,
                                      HttpServletRequest request) {
        boolean shouldSend = authService.shouldSendVerificationCode(req.getEmail(), req.getPurpose(), clientKey(request));
        if (shouldSend) {
            emailService.sendVerificationCode(req.getEmail(), req.getPurpose());
        }
        return ResponseEntity.ok(Map.of("message", "验证码已发送"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(Map.of("message", "密码已重置"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(HttpServletRequest request) {
        Integer userId = ApiResponses.currentUserId(request);
        if (userId == null) {
            return ApiResponses.unauthorized();
        }
        return ResponseEntity.ok(authService.getUserInfo(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@RequestBody Map<String, String> body,
                                      HttpServletRequest request) {
        Integer userId = ApiResponses.currentUserId(request);
        if (userId == null) {
            return ApiResponses.unauthorized();
        }
        authService.updateUsername(userId, body.get("username"));
        return ResponseEntity.ok(Map.of("message", "更新成功"));
    }

    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file,
                                          HttpServletRequest request) throws IOException {
        Integer userId = ApiResponses.currentUserId(request);
        if (userId == null) {
            return ApiResponses.unauthorized();
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请选择头像图片"));
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("error", "头像图片不能超过 2MB"));
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        Set<String> allowedTypes = Set.of("image/jpeg", "image/pjpeg", "image/png", "image/webp");
        if (!allowedTypes.contains(contentType)) {
            return ResponseEntity.badRequest().body(Map.of("error", "仅支持 JPG、PNG 或 WebP 图片"));
        }

        String ext = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        Path uploadDir = Path.of("uploads", "avatars").toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
        String filename = "user-" + userId + "-" + UUID.randomUUID() + ext;
        Path target = uploadDir.resolve(filename);
        try (InputStream input = file.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return ResponseEntity.ok(authService.updateAvatar(userId, "/uploads/avatars/" + filename));
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
