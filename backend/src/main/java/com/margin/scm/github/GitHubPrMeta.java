package com.margin.scm.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Slice of GitHub's pull-request JSON we need: the title and head commit SHA. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubPrMeta(String title, Head head) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Head(String sha) {}

    public String headSha() {
        return head == null ? null : head.sha();
    }
}
