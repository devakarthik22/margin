package com.margin.api.dto;

/** Findings as the frontend consumes them. */
public record FindingDto(
        String category,
        String severity,
        Integer line,
        String filePath,
        String title,
        String explanation,
        String suggestion) {
}
