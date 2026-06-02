package com.insightwrite.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "analysis_tasks")
public class AnalysisTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "essay_text", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String essayText;

    @Column(name = "mode", length = 50)
    private String mode;

    @Column(name = "custom_requirement", length = 200)
    private String customRequirement;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "result_text", columnDefinition = "MEDIUMTEXT")
    private String resultText;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "is_favorite")
    private Boolean isFavorite;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
