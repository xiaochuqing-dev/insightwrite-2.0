package com.insightwrite.repository;

import com.insightwrite.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Integer> {

    Optional<Favorite> findByUserIdAndTaskId(Integer userId, Integer taskId);

    Page<Favorite> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    boolean existsByUserIdAndTaskId(Integer userId, Integer taskId);
}
