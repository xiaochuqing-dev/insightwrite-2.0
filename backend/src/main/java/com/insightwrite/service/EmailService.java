package com.insightwrite.service;

import com.insightwrite.entity.EmailVerificationCode;
import com.insightwrite.repository.EmailVerificationCodeRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class EmailService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String LEGACY_CODE_MARKER = "HASHED";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(10);

    private final JavaMailSender mailSender;
    private final EmailVerificationCodeRepository codeRepository;
    private final PasswordEncoder codeEncoder;
    private final String mailUsername;

    public EmailService(JavaMailSender mailSender,
                        EmailVerificationCodeRepository codeRepository,
                        PasswordEncoder codeEncoder,
                        @Value("${spring.mail.username}") String mailUsername) {
        this.mailSender = mailSender;
        this.codeRepository = codeRepository;
        this.codeEncoder = codeEncoder;
        this.mailUsername = mailUsername;
    }

    @Transactional
    public void sendVerificationCode(String email, String purpose) {
        String normalizedPurpose = normalizePurpose(purpose);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMinuteAgo = now.minusSeconds(60);
        boolean tooFrequent = codeRepository
                .findTopByEmailAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        email, normalizedPurpose, oneMinuteAgo)
                .isPresent();
        if (tooFrequent) {
            throw new RuntimeException("发送过于频繁，请稍后再试");
        }

        String code = generateCode();
        codeRepository.invalidateUnusedForEmailAndPurpose(email, normalizedPurpose);

        EmailVerificationCode record = new EmailVerificationCode();
        record.setEmail(email);
        record.setCode(LEGACY_CODE_MARKER);
        record.setCodeHash(codeEncoder.encode(code));
        record.setPurpose(normalizedPurpose);
        record.setExpiresAt(now.plusMinutes(10));
        record.setUsed(false);
        record.setFailedAttempts(0);
        record.setLockedUntil(null);
        record.setCreatedAt(now);
        codeRepository.save(record);

        sendMail(email, code, normalizedPurpose);
    }

    @Transactional
    public void validateCode(String email, String code, String purpose) {
        String normalizedPurpose = normalizePurpose(purpose);
        LocalDateTime now = LocalDateTime.now();

        EmailVerificationCode record = codeRepository
                .findTopByEmailAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        email, normalizedPurpose, now)
                .orElseThrow(() -> new RuntimeException("验证码未发送或已过期"));

        if (record.getLockedUntil() != null && record.getLockedUntil().isAfter(now)) {
            throw new RuntimeException("验证码错误次数过多，请稍后再试");
        }

        if (record.getCodeHash() == null || record.getCodeHash().isBlank()) {
            record.setUsed(true);
            codeRepository.save(record);
            throw new RuntimeException("验证码未发送或已过期");
        }

        if (!codeEncoder.matches(code, record.getCodeHash())) {
            int failedAttempts = safeAttempts(record) + 1;
            record.setFailedAttempts(failedAttempts);
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                record.setLockedUntil(now.plus(LOCK_DURATION));
            }
            codeRepository.save(record);
            throw new RuntimeException("验证码错误");
        }

        record.setUsed(true);
        record.setFailedAttempts(0);
        record.setLockedUntil(null);
        codeRepository.save(record);
    }

    private String normalizePurpose(String purpose) {
        if ("forgot_password".equals(purpose)) {
            return "reset_password";
        }
        return purpose;
    }

    private int safeAttempts(EmailVerificationCode record) {
        return record.getFailedAttempts() != null ? record.getFailedAttempts() : 0;
    }

    private String generateCode() {
        int code = RANDOM.nextInt(900000) + 100000;
        return String.valueOf(code);
    }

    private void sendMail(String to, String code, String purpose) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            String subject;
            String content;
            if ("register".equals(purpose)) {
                subject = "InsightWrite 注册验证码";
                content = "<p>您好：</p>"
                        + "<p>您的注册验证码是：<strong style='font-size:24px;color:#2563eb;'>" + code + "</strong></p>"
                        + "<p>有效期为 10 分钟，请勿告诉他人。</p>"
                        + "<p>InsightWrite 写作助手</p>";
            } else {
                subject = "InsightWrite 密码重置验证码";
                content = "<p>您好：</p>"
                        + "<p>您的密码重置验证码是：<strong style='font-size:24px;color:#2563eb;'>" + code + "</strong></p>"
                        + "<p>有效期为 10 分钟，请勿告诉他人。</p>"
                        + "<p>InsightWrite 写作助手</p>";
            }

            helper.setFrom(mailUsername, "InsightWrite写作助手");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }
}
