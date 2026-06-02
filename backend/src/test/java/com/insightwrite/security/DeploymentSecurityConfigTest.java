package com.insightwrite.security;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DeploymentSecurityConfigTest {

    private static final Path PROJECT_ROOT = Path.of("..").toAbsolutePath().normalize();

    @Test
    void composeOnlyPublishesNginxAndUsesLeastPrivilegeDatabaseUser() throws Exception {
        String compose = read("docker/docker-compose.yml");

        assertThat(compose).doesNotContain("\"3306:3306\"");
        assertThat(compose).doesNotContain("\"8080:8080\"");
        assertThat(compose).contains("MYSQL_USER: ${DB_USERNAME");
        assertThat(compose).contains("MYSQL_PASSWORD: ${DB_PASSWORD");
        assertThat(compose).contains("\"443:443\"");
    }

    @Test
    void nginxTerminatesHttpsAndSendsSecurityHeaders() throws Exception {
        String nginx = read("docker/nginx.conf");

        assertThat(nginx).contains("listen 443 ssl");
        assertThat(nginx).contains("return 301 https://$host$request_uri");
        assertThat(nginx).contains("Strict-Transport-Security");
        assertThat(nginx).contains("Content-Security-Policy");
        assertThat(nginx).contains("X-Content-Type-Options");
        assertThat(nginx).contains("X-Frame-Options");
        assertThat(nginx).contains("Referrer-Policy");
        assertThat(nginx).contains("X-Forwarded-Proto");
    }

    @Test
    void backendContainerRunsAsNonRootUser() throws Exception {
        String dockerfile = read("docker/Dockerfile");

        assertThat(dockerfile).contains("USER appuser");
    }

    private String read(String relativePath) throws Exception {
        return Files.readString(PROJECT_ROOT.resolve(relativePath));
    }
}
