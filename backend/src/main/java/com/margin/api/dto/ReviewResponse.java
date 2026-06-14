package com.margin.api.dto;

import java.util.List;
import java.util.Map;

/** Review result plus per-severity counts the dashboard renders directly. */
public record ReviewResponse(
        String verdict,
        String summary,
        List<FindingDto> findings,
        Map<String, Long> countsBySeverity) {
}
