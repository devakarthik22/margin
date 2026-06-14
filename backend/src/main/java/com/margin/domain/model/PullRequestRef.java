package com.margin.domain.model;

import java.util.Objects;

/**
 * Immutable coordinate of a pull request on a source-control host.
 * {@code headSha} is the key used for idempotency: a given commit is reviewed once.
 */
public record PullRequestRef(String owner, String repo, int number, String headSha) {

    public PullRequestRef {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(repo, "repo");
        if (number <= 0) {
            throw new IllegalArgumentException("pull request number must be positive");
        }
    }

    public String slug() {
        return owner + "/" + repo + "#" + number;
    }
}
