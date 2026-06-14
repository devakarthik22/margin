package com.margin.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Ad-hoc review of a pasted unified diff (dashboard flow). */
public record DiffReviewRequest(@NotBlank String rawDiff) {
}
