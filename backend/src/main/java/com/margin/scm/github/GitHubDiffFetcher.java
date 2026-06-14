package com.margin.scm.github;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Fetches a pull request's unified diff from GitHub's REST API for the read-only
 * dashboard "review by URL" flow. Distinct from {@link GitHubScmProvider}, which
 * also publishes reviews back: this one only ever reads, and works without a
 * token for public repositories (a token, if configured, raises the rate limit).
 */
@Component
public class GitHubDiffFetcher {

    private static final String API = "https://api.github.com";
    private final RestClient http;

    public GitHubDiffFetcher(GitHubProperties props) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(API)
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");
        if (props.token() != null && !props.token().isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + props.token());
        }
        this.http = builder.build();
    }

    /** Returns the raw unified diff for the given public PR. */
    public String fetchDiff(String owner, String repo, int number) {
        return http.get()
                .uri("/repos/{o}/{r}/pulls/{n}", owner, repo, number)
                .accept(MediaType.parseMediaType("application/vnd.github.v3.diff"))
                .retrieve()
                .body(String.class);
    }

    /** Returns PR metadata (title, head SHA) needed to resolve file contents. */
    public GitHubPrMeta fetchMeta(String owner, String repo, int number) {
        return http.get()
                .uri("/repos/{o}/{r}/pulls/{n}", owner, repo, number)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(GitHubPrMeta.class);
    }

    /**
     * Returns the full contents of a file at a specific ref (the PR head SHA).
     * Path segments are encoded individually so nested paths (a/b/c.java) work.
     */
    public String fetchFileContent(String owner, String repo, String ref, String path) {
        return http.get()
                .uri(b -> b.pathSegment("repos", owner, repo, "contents")
                        .pathSegment(path.split("/"))
                        .queryParam("ref", ref)
                        .build())
                .accept(MediaType.parseMediaType("application/vnd.github.raw"))
                .retrieve()
                .body(String.class);
    }
}
