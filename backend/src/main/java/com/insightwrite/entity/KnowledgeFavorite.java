package com.insightwrite.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(
        name = "knowledge_favorites",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "knowledge_entry_id"})
)
public class KnowledgeFavorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "knowledge_entry_id", nullable = false)
    private KnowledgeEntry knowledgeEntry;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
