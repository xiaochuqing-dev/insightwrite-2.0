package com.insightwrite.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "email", nullable = false, length = 120)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 256)
    private String passwordHash;

    @Column(name = "avatar")
    private String avatar;

    @Column(name = "credits")
    private Integer credits;

    @Column(name = "credit_cycle_start")
    private LocalDateTime creditCycleStart;

    @Column(name = "last_credit_refill")
    private LocalDateTime lastCreditRefill;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
