package com.insightwrite.repository;

import com.insightwrite.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Integer> {

    Page<Article> findByCategoryOrderByCreatedAtDesc(String category, Pageable pageable);

    Page<Article> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
