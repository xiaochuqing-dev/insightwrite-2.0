package com.insightwrite.util;

import org.springframework.data.domain.Page;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PageUtils {

    private PageUtils() {
    }

    public static Map<String, Object> response(Page<?> page, int pageNumber, List<?> items) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", (int) page.getTotalElements());
        result.put("page", pageNumber);
        result.put("pages", page.getTotalPages());
        return result;
    }
}
