package com.insightwrite.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DeepSeekServiceAnnotationTest {

    @Test
    void parsesAnnotationsWhenAiUsesPlainAnnotationHeader() throws Exception {
        String essay = "A healthy diet is one of the most powerful tools for maintaining good health.";
        String result = """
                这篇文章整体表达清楚。
                原文标注指引：
                1: "A healthy diet" | 淡蓝色 | 开头表达自然。
                2: "maintaining good health" | 黄色 | 可替换为更具体的表达。
                """;

        List<Map<String, Object>> annotations = parseAnnotationsOnly(essay, result);

        assertThat(annotations).hasSize(2);
        assertThat(annotations.get(0)).containsEntry("start", 0).containsEntry("type", "highlight");
        assertThat(annotations.get(1)).containsEntry("type", "suggestion");
    }

    @Test
    void parsesAnnotationsWhenAiUsesChineseColonAfterIndex() throws Exception {
        String essay = "Vegetables and fruits should form the largest portion of our meals.";
        String result = """
                【原文标注指引】
                1： "Vegetables and fruits" | 蓝色 | 分类清楚，表达直接。
                2： "largest portion" | 淡黄色 | 表达基本正确，可进一步精确。
                """;

        List<Map<String, Object>> annotations = parseAnnotationsOnly(essay, result);

        assertThat(annotations).hasSize(2);
        assertThat(annotations.get(0)).containsEntry("type", "excellent");
        assertThat(annotations.get(1)).containsEntry("severity", "low");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseAnnotationsOnly(String essay, String resultText) throws Exception {
        Class<?> serviceClass = Class.forName("com.insightwrite.service.DeepSeekService");
        Object service = serviceClass.getConstructor().newInstance();
        return (List<Map<String, Object>>) serviceClass
                .getMethod("parseAnnotationsOnly", String.class, String.class)
                .invoke(service, essay, resultText);
    }
}
