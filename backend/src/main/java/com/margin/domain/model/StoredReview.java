package com.margin.domain.model;

import java.time.Instant;

/** A persisted review with its PR coordinate and timestamp — the history read model. */
public record StoredReview(PullRequestRef ref, ReviewResult result, Instant createdAt) {
}
