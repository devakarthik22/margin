package com.margin.scm.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Typed representation of the GitHub pull_request webhook payload. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubPullRequestEvent(
        String action,
        @JsonProperty("pull_request") PullRequest pullRequest,
        Repository repository) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PullRequest(Integer number, Head head) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Head(String sha) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Repository(String name, Owner owner) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Owner(String login) {}
}
