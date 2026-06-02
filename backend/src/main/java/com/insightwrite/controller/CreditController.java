package com.insightwrite.controller;

import com.insightwrite.service.CreditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CreditController {

    private final CreditService creditService;

    public CreditController(CreditService creditService) {
        this.creditService = creditService;
    }

    @GetMapping("/credits")
    public ResponseEntity<?> getCredits(HttpServletRequest request) {
        Integer userId = ApiResponses.currentUserId(request);
        if (userId == null) {
            return ApiResponses.unauthorized();
        }
        return ResponseEntity.ok(creditService.getBalance(userId));
    }

    @GetMapping("/credits/transactions")
    public ResponseEntity<?> getTransactions(@RequestParam(defaultValue = "1") int page,
                                             HttpServletRequest request) {
        Integer userId = ApiResponses.currentUserId(request);
        if (userId == null) {
            return ApiResponses.unauthorized();
        }
        return ResponseEntity.ok(creditService.getTransactions(userId, page, 20));
    }
}
