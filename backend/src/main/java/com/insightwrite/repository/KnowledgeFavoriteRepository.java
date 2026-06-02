package com.insightwrite.repository;

import com.insightwrite.entity.KnowledgeFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KnowledgeFavoriteRepository extends JpaRepository<KnowledgeFavorite, Integer> {

    Optional<KnowledgeFavorite> findByUserIdAndKnowledgeEntryId(Integer userId, Integer knowledgeEntryId);

    Page<KnowledgeFavorite> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    boolean existsByUserIdAndKnowledgeEntryId(Integer userId, Integer knowledgeEntryId);
}
