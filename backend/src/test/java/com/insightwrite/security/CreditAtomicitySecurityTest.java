package com.insightwrite.security;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CreditAtomicitySecurityTest {

    private static final Path PROJECT_ROOT = Path.of("..").toAbsolutePath().normalize();

    @Test
    void creditMutationPathsUseRepositoryAtomicUpdates() throws Exception {
        String analyzeService = read("backend/src/main/java/com/insightwrite/service/AnalyzeService.java");
        String asyncRunner = read("backend/src/main/java/com/insightwrite/service/AsyncAnalysisRunner.java");
        String creditService = read("backend/src/main/java/com/insightwrite/service/CreditService.java");
        String userRepository = read("backend/src/main/java/com/insightwrite/repository/UserRepository.java");

        assertThat(analyzeService).contains("deductCreditsIfEnough");
        assertThat(asyncRunner).contains("addCredits");
        assertThat(creditService).contains("initializeCreditCycleIfMissing");
        assertThat(creditService).contains("resetCreditCycleIfDue");
        assertThat(creditService).contains("refillDailyCreditsIfDue");

        assertThat(userRepository).contains("UPDATE User u SET u.credits = u.credits - :amount");
        assertThat(userRepository).contains("UPDATE User u SET u.credits = COALESCE(u.credits, 0) + :amount");
        assertThat(userRepository).contains("UPDATE User u SET u.credits = COALESCE(u.credits, 0) + :dailyCredits");

        assertThat(analyzeService).doesNotContain("setCredits(user.getCredits() - cost)");
        assertThat(asyncRunner).doesNotContain("setCredits(user.getCredits() + cost)");
        assertThat(creditService).doesNotContain("+ dailyCredits");
    }

    private String read(String relativePath) throws Exception {
        return Files.readString(PROJECT_ROOT.resolve(relativePath));
    }
}
