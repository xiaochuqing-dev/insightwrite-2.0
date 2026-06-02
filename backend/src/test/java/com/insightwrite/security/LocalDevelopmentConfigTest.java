package com.insightwrite.security;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDevelopmentConfigTest {

    private static final Path PROJECT_ROOT = Path.of("..").toAbsolutePath().normalize();

    @Test
    void backendCanImportUntrackedRootEnvironmentFile() throws Exception {
        String application = read("backend/src/main/resources/application.yml");

        assertThat(application).contains("optional:file:../.env[.properties]");
    }

    @Test
    void rootEnvironmentTemplateIsTrackedButRealEnvironmentFileIsIgnored() throws Exception {
        String gitignore = read(".gitignore");
        Path template = PROJECT_ROOT.resolve(".env.example");

        assertThat(Files.exists(template)).isTrue();
        assertThat(gitignore).contains(".env");
        assertThat(gitignore).contains("backend/src/main/resources/application-local.yml");
    }

    private String read(String relativePath) throws Exception {
        return Files.readString(PROJECT_ROOT.resolve(relativePath));
    }
}
