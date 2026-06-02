package com.insightwrite.util;

public class PreviewUtils {

    private static final int DEFAULT_MAX_LENGTH = 80;

    public static String truncate(String text) {
        return truncate(text, DEFAULT_MAX_LENGTH);
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength
                ? text.substring(0, maxLength) + "..."
                : text;
    }
}
