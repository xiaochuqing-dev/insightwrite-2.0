package com.insightwrite.repository;

import com.insightwrite.entity.KnowledgeEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KnowledgeEntryRepository extends JpaRepository<KnowledgeEntry, Integer> {

    Page<KnowledgeEntry> findByCategoryOrderByCreatedAtDesc(String category, Pageable pageable);

    Page<KnowledgeEntry> findByTitleContainingOrTagsContainingOrderByCreatedAtDesc(
            String title, String tags, Pageable pageable);

    @Query("SELECT k FROM KnowledgeEntry k WHERE " +
           "k.category = :category AND (k.title LIKE %:search% OR k.tags LIKE %:search%) " +
           "ORDER BY k.createdAt DESC")
    Page<KnowledgeEntry> findByCategoryAndSearch(@Param("category") String category,
                                                  @Param("search") String search,
                                                  Pageable pageable);
}
