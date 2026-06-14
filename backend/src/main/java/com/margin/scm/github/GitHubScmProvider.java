package com.margin.scm.github;

import com.margin.domain.model.Finding;
import com.margin.domain.model.PullRequestRef;
import com.margin.domain.model.ReviewResult;
import com.margin.scm.ScmProvider;
import com.margin.scm.ScmProviderType;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GitHub implementation of the {@link ScmProvider} strategy. Talks to the REST
 * v3 API: fetches the PR patch, reads file contents for the agent's tool, and
 * publishes a review with inline comments. HTTP concerns live here only; the
 * rest of the system stays host-agnostic.
 */
@Component
public class GitHubScmProvider implements ScmProvider {

    private static final String API = "https://api.github.com";
    private final RestClient http;

    public GitHubScmProvider(GitHubProperties props) {
        this.http = RestClient.builder()
                .baseUrl(API)
                .defaultHeader("Authorization", "Bearer " + props.token())
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    @Override
    public ScmProviderType type() {
        return ScmProviderType.GITHUB;
    }

    @Override
    public String fetchRawDiff(PullRequestRef ref) {
        return http.get()
                .uri("/repos/{o}/{r}/pulls/{n}", ref.owner(), ref.repo(), ref.number())
                .accept(MediaType.parseMediaType("application/vnd.github.v3.diff"))
                .retrieve()
                .body(String.class);
    }

    @Override
    public String fetchFileContent(PullRequestRef ref, String path) {
        return http.get()
                .uri("/repos/{o}/{r}/contents/{p}?ref={sha}",
                        ref.owner(), ref.repo(), path, ref.headSha())
                .accept(MediaType.parseMediaType("application/vnd.github.raw"))
                .retrieve()
                .body(String.class);
    }

    @Override
    public void publishReview(PullRequestRef ref, ReviewResult result) {
        List<Map<String, Object>> comments = result.findings().stream()
                .filter(f -> f.line() != null)
                .map(this::toInlineComment)
                .toList();

        Map<String, Object> body = Map.of(
                "commit_id", ref.headSha(),
                "body", result.summary(),
                "event", mapEvent(result),
                "comments", comments);

        http.post()
                .uri("/repos/{o}/{r}/pulls/{n}/reviews", ref.owner(), ref.repo(), ref.number())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Publishes a review with user-edited inline comments to a PR. Posted as a
     * COMMENT-event review (not APPROVE/REQUEST_CHANGES) so it succeeds even when
     * the token owner is the PR author. Requires write access to the repo.
     *
     * @param comments each map must carry {@code path}, {@code line}, {@code body}
     */
    public void publishComments(String owner, String repo, int number, String commitId,
                                String summary, List<Map<String, Object>> comments) {
        Map<String, Object> body = new HashMap<>();
        body.put("commit_id", commitId);
        body.put("body", summary == null ? "" : summary);
        body.put("event", "COMMENT");
        body.put("comments", comments);

        http.post()
                .uri("/repos/{o}/{r}/pulls/{n}/reviews", owner, repo, number)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private Map<String, Object> toInlineComment(Finding f) {
        String text = "**[" + f.severity() + " · " + f.category() + "]** " + f.title()
                + "\n\n" + f.explanation()
                + (f.suggestion() == null ? "" : "\n\n_Suggested fix:_ " + f.suggestion());
        return Map.of("path", f.filePath(), "line", f.line(), "body", text);
    }

    private String mapEvent(ReviewResult result) {
        return switch (result.verdict()) {
            case APPROVE -> "APPROVE";
            case REQUEST_CHANGES -> "REQUEST_CHANGES";
            case COMMENT -> "COMMENT";
        };
    }
}
