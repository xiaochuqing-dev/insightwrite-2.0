package com.insightwrite.repository;

import com.insightwrite.entity.AnalysisTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnalysisTaskRepository extends JpaRepository<AnalysisTask, Integer> {

    Optional<AnalysisTask> findByIdAndUser_Id(Integer id, Integer userId);

    // 查某用户的所有分析记录
    Page<AnalysisTask> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    // 查某用户已完成的记录
    Page<AnalysisTask> findByUserIdAndStatusOrderByCreatedAtDesc(Integer userId, String status, Pageable pageable);

    // 查某用户的收藏
    Page<AnalysisTask> findByUserIdAndIsFavoriteTrueOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    // 统计排队中的任务数（用于前端显示"前面还有 X 个任务"）
    long countByStatus(String status);
}
