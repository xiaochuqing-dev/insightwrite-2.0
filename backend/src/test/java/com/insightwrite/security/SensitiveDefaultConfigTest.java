package com.insightwrite.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDefaultConfigTest {

    private static final Path PROJECT_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Pattern SENSITIVE_FALLBACK = Pattern.compile(
            "\\$\\{(?:DEEPSEEK_API_KEY|DB_USERNAME|DB_PASSWORD|MAIL_USERNAME|MAIL_PASSWORD):");
    private static final Pattern RAW_SECRET_SHAPE = Pattern.compile("sk-[A-Za-z0-9]{20,}");
    private static final Pattern REUSABLE_JWT_ATTACK_SNIPPET = Pattern.compile("jwt\\.encode\\(|secret\\s*=\\s*b\"");

    @Test
    void applicationConfigDoesNotContainSensitiveEnvironmentFallbacks() throws IOException {
        String content = readProjectFile("backend/src/main/resources/application.yml");

        assertThat(content).doesNotContainPattern(SENSITIVE_FALLBACK);
        assertThat(content).doesNotContainPattern(RAW_SECRET_SHAPE);
    }

    @Test
    void dockerComposeDoesNotContainHardcodedDatabaseCredentials() throws IOException {
        List<String> lines = Files.readAllLines(PROJECT_ROOT.resolve("docker/docker-compose.yml"));

        assertThat(lines).noneMatch(line -> hasLiteralValue(line, "MYSQL_ROOT_PASSWORD"));
        assertThat(lines).noneMatch(line -> hasLiteralValue(line, "SPRING_DATASOURCE_PASSWORD"));
        assertThat(lines).noneMatch(line -> hasLiteralRootUsername(line, "SPRING_DATASOURCE_USERNAME"));
    }

    @Test
    void generatedAuditReportsDoNotContainReusableSecretsOrJwtForgeryMaterial() throws IOException {
        Path report = PROJECT_ROOT.resolve("Deep_Logic_Audit_Report.md");
        if (!Files.exists(report)) {
            return;
        }

        String content = Files.readString(report);

        assertThat(content).doesNotContainPattern(RAW_SECRET_SHAPE);
        assertThat(content).doesNotContainPattern(SENSITIVE_FALLBACK);
        assertThat(content).doesNotContainPattern(REUSABLE_JWT_ATTACK_SNIPPET);
    }

    private String readProjectFile(String relativePath) throws IOException {
        return Files.readString(PROJECT_ROOT.resolve(relativePath));
    }

    private boolean hasLiteralValue(String line, String key) {
        String trimmed = line.trim();
        return trimmed.startsWith(key + ":") && !trimmed.substring(key.length() + 1).trim().startsWith("${");
    }

    private boolean hasLiteralRootUsername(String line, String key) {
        String trimmed = line.trim();
        return trimmed.startsWith(key + ":") && "root".equals(trimmed.substring(key.length() + 1).trim());
    }
}
