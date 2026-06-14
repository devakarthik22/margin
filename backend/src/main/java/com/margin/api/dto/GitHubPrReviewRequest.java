package com.margin.api.dto;

import com.margin.scm.github.GitHubPrUrl;
import jakarta.validation.constraints.NotBlank;

/**
 * Read-only review of a public GitHub pull request, triggered from the dashboard
 * by pasting the PR's web URL (e.g. https://github.com/owner/repo/pull/123).
 * Nothing is published back to GitHub — findings are streamed to the UI only.
 */
public record GitHubPrReviewRequest(@NotBlank String prUrl) {

    /** owner / repo / number parsed out of the pasted URL. */
    public GitHubPrUrl.Location location() {
        return GitHubPrUrl.parse(prUrl);
    }
}
