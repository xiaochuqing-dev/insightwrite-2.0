package com.insightwrite.repository;

import com.insightwrite.entity.EmailVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Integer> {

    // 查最新一条未使用的有效验证码
    Optional<EmailVerificationCode> findTopByEmailAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String email, String purpose, LocalDateTime now);

    // 标记某邮箱所有未使用验证码为已使用（防止一个验证码反复用）
    @Modifying
    @Query("UPDATE EmailVerificationCode c SET c.used = true WHERE c.email = ?1 AND c.used = false")
    void invalidateAllForEmail(String email);

    @Modifying
    @Query("UPDATE EmailVerificationCode c SET c.used = true WHERE c.email = ?1 AND c.purpose = ?2 AND c.used = false")
    void invalidateUnusedForEmailAndPurpose(String email, String purpose);
}
