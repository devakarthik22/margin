package com.margin.api.dto;

import java.time.Instant;

/** A compact past-review row for the history endpoint. */
public record ReviewHistoryItem(
        String slug,
        int number,
        String headSha,
        String verdict,
        String summary,
        int findingCount,
        Instant createdAt) {
}
