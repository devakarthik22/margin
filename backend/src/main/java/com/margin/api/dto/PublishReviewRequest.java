package com.margin.api.dto;

import com.margin.scm.github.GitHubPrUrl;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Publishes user-edited review comments back to a GitHub PR as a single review.
 * Requires a configured GitHub token with write access to the target repo.
 */
public record PublishReviewRequest(
        @NotBlank String prUrl,
        String summary,
        List<Comment> comments) {

    /** One inline comment. {@code line} is the new-file line it attaches to. */
    public record Comment(String path, Integer line, String body) {}

    public GitHubPrUrl.Location location() {
        return GitHubPrUrl.parse(prUrl);
    }
}
