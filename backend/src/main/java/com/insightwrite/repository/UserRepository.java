package com.insightwrite.repository;

import com.insightwrite.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.credits = u.credits - :amount WHERE u.id = :userId AND u.credits >= :amount")
    int deductCreditsIfEnough(@Param("userId") Integer userId, @Param("amount") int amount);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.credits = COALESCE(u.credits, 0) + :amount WHERE u.id = :userId")
    int addCredits(@Param("userId") Integer userId, @Param("amount") int amount);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.creditCycleStart = :now, u.lastCreditRefill = :now WHERE u.id = :userId AND u.creditCycleStart IS NULL")
    int initializeCreditCycleIfMissing(@Param("userId") Integer userId, @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.credits = :dailyCredits, u.creditCycleStart = :now, u.lastCreditRefill = :now WHERE u.id = :userId AND u.creditCycleStart <= :cycleCutoff")
    int resetCreditCycleIfDue(@Param("userId") Integer userId,
                              @Param("dailyCredits") int dailyCredits,
                              @Param("now") LocalDateTime now,
                              @Param("cycleCutoff") LocalDateTime cycleCutoff);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.credits = COALESCE(u.credits, 0) + :dailyCredits, u.lastCreditRefill = :now WHERE u.id = :userId AND (u.lastCreditRefill IS NULL OR u.lastCreditRefill < :todayStart)")
    int refillDailyCreditsIfDue(@Param("userId") Integer userId,
                                @Param("dailyCredits") int dailyCredits,
                                @Param("now") LocalDateTime now,
                                @Param("todayStart") LocalDateTime todayStart);
}
