package com.insightwrite.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyzeServiceLimitsTest {

    @Test
    void validateInputRejectsOverlongEssayForMode() throws Exception {
        String overLimit = "word ".repeat(801);

        assertThatThrownBy(() -> validateInput("basic", overLimit, "Short title", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("800");
    }

    @Test
    void validateInputRejectsOverlongTopic() throws Exception {
        String longTopic = "topic ".repeat(31);

        assertThatThrownBy(() -> validateInput("basic", "A concise essay.", longTopic, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("标题");
    }

    @Test
    void validateInputAcceptsDeepModeWithinLimit() throws Exception {
        assertThat(validateInput("deep", "word ".repeat(1200), "Reading title", ""))
                .isEqualTo(1200);
    }

    @Test
    void getTaskStatusReturnsWordCountBeforeCompletion() throws Exception {
        Class<?> taskClass = Class.forName("com.insightwrite.entity.AnalysisTask");
        Object task = taskClass.getConstructor().newInstance();
        taskClass.getMethod("setId", Integer.class).invoke(task, 7);
        taskClass.getMethod("setStatus", String.class).invoke(task, "processing");
        taskClass.getMethod("setMode", String.class).invoke(task, "deep");
        taskClass.getMethod("setWordCount", Integer.class).invoke(task, 356);
        taskClass.getMethod("setIsFavorite", Boolean.class).invoke(task, false);
        taskClass.getMethod("setCreatedAt", LocalDateTime.class).invoke(task, LocalDateTime.parse("2026-05-29T12:00:00"));

        Class<?> serviceClass = Class.forName("com.insightwrite.service.AnalyzeService");
        Class<?> taskRepoClass = Class.forName("com.insightwrite.repository.AnalysisTaskRepository");
        Class<?> userRepoClass = Class.forName("com.insightwrite.repository.UserRepository");
        Class<?> creditRepoClass = Class.forName("com.insightwrite.repository.CreditTransactionRepository");
        Class<?> deepSeekClass = Class.forName("com.insightwrite.service.DeepSeekService");
        Class<?> asyncRunnerClass = Class.forName("com.insightwrite.service.AsyncAnalysisRunner");
        Object taskRepository = mock(taskRepoClass);
        when(taskRepoClass.getMethod("findByIdAndUser_Id", Integer.class, Integer.class).invoke(taskRepository, 7, 3))
                .thenReturn(Optional.of(task));
        Object service = serviceClass
                .getConstructor(taskRepoClass, userRepoClass, creditRepoClass, deepSeekClass, asyncRunnerClass)
                .newInstance(taskRepository, null, null, null, null);

        @SuppressWarnings("unchecked")
        Map<String, Object> status = (Map<String, Object>) serviceClass
                .getMethod("getTaskStatus", Integer.class, Integer.class)
                .invoke(service, 7, 3);

        assertThat(status).containsEntry("word_count", 356);
    }

    private int validateInput(String mode, String text, String topic, String custom) throws Exception {
        Class<?> serviceClass = Class.forName("com.insightwrite.service.AnalyzeService");
        Class<?> taskRepoClass = Class.forName("com.insightwrite.repository.AnalysisTaskRepository");
        Class<?> userRepoClass = Class.forName("com.insightwrite.repository.UserRepository");
        Class<?> creditRepoClass = Class.forName("com.insightwrite.repository.CreditTransactionRepository");
        Class<?> deepSeekClass = Class.forName("com.insightwrite.service.DeepSeekService");
        Class<?> asyncRunnerClass = Class.forName("com.insightwrite.service.AsyncAnalysisRunner");
        Object service = serviceClass
                .getConstructor(taskRepoClass, userRepoClass, creditRepoClass, deepSeekClass, asyncRunnerClass)
                .newInstance(null, null, null, null, null);
        try {
            return (int) serviceClass
                    .getMethod("validateInput", String.class, String.class, String.class, String.class)
                    .invoke(service, mode, text, topic, custom);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw e;
        }
    }
}
