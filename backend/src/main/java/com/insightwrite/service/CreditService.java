package com.insightwrite.service;

import com.insightwrite.entity.CreditTransaction;
import com.insightwrite.entity.User;
import com.insightwrite.repository.CreditTransactionRepository;
import com.insightwrite.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CreditService {

    private final UserRepository userRepository;
    private final CreditTransactionRepository creditRepository;

    @Value("${insightwrite.credits.daily}")
    private int dailyCredits;

    public CreditService(UserRepository userRepository,
                         CreditTransactionRepository creditRepository) {
        this.userRepository = userRepository;
        this.creditRepository = creditRepository;
    }

    @Transactional
    public Map<String, Object> getBalance(Integer userId) {
        User user = refreshCreditsIfNeeded(userId);
        LocalDateTime tomorrow = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT);

        return Map.of(
                "credits", (Object) user.getCredits(),
                "daily_reset_at", tomorrow.toString()
        );
    }

    public Map<String, Object> getTransactions(Integer userId, int page, int size) {
        Page<CreditTransaction> pg = creditRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page - 1, size));

        List<Map<String, Object>> items = new ArrayList<>();
        for (CreditTransaction tx : pg.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", tx.getId());
            item.put("amount", tx.getAmount());
            item.put("reason", tx.getReason());
            item.put("balance_after", tx.getBalanceAfter());
            item.put("created_at", tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : null);
            items.add(item);
        }

        return Map.of(
                "items", items,
                "total", (int) pg.getTotalElements(),
                "page", page
        );
    }

    private User refreshCreditsIfNeeded(Integer userId) {
        LocalDateTime now = LocalDateTime.now();
        int initialized = userRepository.initializeCreditCycleIfMissing(userId, now);
        if (initialized == 0) {
            int reset = userRepository.resetCreditCycleIfDue(userId, dailyCredits, now, now.minusDays(7));
            if (reset == 0) {
                LocalDateTime todayStart = LocalDateTime.of(now.toLocalDate(), LocalTime.MIDNIGHT);
                userRepository.refillDailyCreditsIfDue(userId, dailyCredits, now, todayStart);
            }
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}
