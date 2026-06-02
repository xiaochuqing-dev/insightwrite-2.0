package com.insightwrite.repository;

import com.insightwrite.entity.CreditTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, Integer> {

    Page<CreditTransaction> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
}
