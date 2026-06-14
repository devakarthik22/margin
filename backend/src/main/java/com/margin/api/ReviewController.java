package com.margin.api;

import com.margin.api.dto.DiffReviewRequest;
import com.margin.api.dto.GitHubPrReviewRequest;
import com.margin.api.dto.PrFilesResponse;
import com.margin.api.dto.PublishReviewRequest;
import com.margin.api.dto.ReviewRequest;
import com.margin.api.dto.ReviewResponse;
import com.margin.agent.AgentEvent;
import com.margin.agent.FileContextProvider;
import com.margin.diff.DiffParser;
import com.margin.domain.model.PullRequestRef;
import com.margin.api.dto.ReviewHistoryItem;
import com.margin.review.ReviewQueryService;
import com.margin.review.ReviewService;
import com.margin.scm.github.GitHubDiffFetcher;
import com.margin.scm.github.GitHubPrMeta;
import com.margin.scm.github.GitHubScmProvider;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/** On-demand review endpoint used by the dashboard. */
@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "${margin.cors.allowed-origin:http://localhost:4200}")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewQueryService queryService;
    private final ApiMapper mapper;
    private final Executor sseExecutor;
    private final GitHubDiffFetcher gitHubDiffFetcher;
    private final DiffParser diffParser;
    private final GitHubScmProvider gitHubScmProvider;

    public ReviewController(ReviewService reviewService,
                            ReviewQueryService queryService,
                            ApiMapper mapper,
                            @Qualifier("sseExecutor") Executor sseExecutor,
                            GitHubDiffFetcher gitHubDiffFetcher,
                            DiffParser diffParser,
                            GitHubScmProvider gitHubScmProvider) {
        this.reviewService = reviewService;
        this.queryService = queryService;
        this.mapper = mapper;
        this.sseExecutor = sseExecutor;
        this.gitHubDiffFetcher = gitHubDiffFetcher;
        this.diffParser = diffParser;
        this.gitHubScmProvider = gitHubScmProvider;
    }

    @PostMapping
    public ReviewResponse review(@Valid @RequestBody ReviewRequest request) {
        PullRequestRef ref = new PullRequestRef(
                request.owner(), request.repo(), request.number(), request.headSha());
        var result = reviewService.reviewPullRequest(ref, request.providerOrDefault());
        return mapper.toResponse(result);
    }

    @PostMapping("/diff")
    public ReviewResponse reviewDiff(@Valid @RequestBody DiffReviewRequest request) {
        var result = reviewService.reviewRawDiff(request.rawDiff());
        return mapper.toResponse(result);
    }

    /** SSE endpoint: streams agent trace events then the final review result. */
    @PostMapping(value = "/diff/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter reviewDiffStream(@Valid @RequestBody DiffReviewRequest request) {
        return stream(onEvent -> reviewService.reviewRawDiff(request.rawDiff(), onEvent));
    }

    /**
     * Returns the parsed contents of a public GitHub PR (file list + diffs) for
     * the dashboard's PR browser. Read-only; no review is run.
     */
    @PostMapping("/github/files")
    public PrFilesResponse gitHubFiles(@Valid @RequestBody GitHubPrReviewRequest request) {
        var loc = request.location();
        GitHubPrMeta meta = gitHubDiffFetcher.fetchMeta(loc.owner(), loc.repo(), loc.number());
        String rawDiff = gitHubDiffFetcher.fetchDiff(loc.owner(), loc.repo(), loc.number());
        var diff = diffParser.parse(rawDiff);
        return mapper.toPrFiles(diff, loc.owner(), loc.repo(), loc.number(),
                meta == null ? null : meta.title());
    }

    /**
     * SSE endpoint: review a public GitHub PR by URL. Read-only — fetches the
     * diff, runs the agent (with real file context from the PR head), and streams
     * findings. Nothing is published back.
     */
    @PostMapping(value = "/github/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter reviewGitHubStream(@Valid @RequestBody GitHubPrReviewRequest request) {
        return stream(onEvent -> {
            // Parse inside the stream so a bad URL surfaces as an SSE error event
            // rather than a ProblemDetail the text/event-stream response can't write.
            var loc = request.location();
            onEvent.accept(new AgentEvent("sys",
                    "fetch PR · " + loc.owner() + "/" + loc.repo() + " #" + loc.number()));
            GitHubPrMeta meta = gitHubDiffFetcher.fetchMeta(loc.owner(), loc.repo(), loc.number());
            String headSha = meta == null ? null : meta.headSha();
            String rawDiff = gitHubDiffFetcher.fetchDiff(loc.owner(), loc.repo(), loc.number());

            // Real file context: the agent can pull full file contents from the PR head.
            FileContextProvider fileContext = path ->
                    gitHubDiffFetcher.fetchFileContent(loc.owner(), loc.repo(), headSha, path);

            return reviewService.reviewRawDiff(rawDiff, fileContext, onEvent);
        });
    }

    /**
     * Shared SSE plumbing: runs the supplied review on the SSE executor, relaying
     * each {@link AgentEvent} as a {@code trace} event, then the final result (or
     * an {@code error} event if it fails).
     */
    private SseEmitter stream(java.util.function.Function<java.util.function.Consumer<AgentEvent>, ?> work) {
        SseEmitter emitter = new SseEmitter(120_000L);
        sseExecutor.execute(() -> {
            try {
                var result = (com.margin.domain.model.ReviewResult) work.apply(event -> {
                    try {
                        emitter.send(SseEmitter.event().name("trace")
                                .data(event, MediaType.APPLICATION_JSON));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                });
                emitter.send(SseEmitter.event().name("result")
                        .data(mapper.toResponse(result), MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data("{\"message\":\"" + e.getMessage() + "\"}", MediaType.APPLICATION_JSON));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    /**
     * Publishes user-edited comments to a GitHub PR as a single review.
     * Requires a configured GitHub token with write access to the repo.
     */
    @PostMapping("/github/publish")
    public Map<String, Object> publishToGitHub(@Valid @RequestBody PublishReviewRequest request) {
        var loc = request.location();
        GitHubPrMeta meta = gitHubDiffFetcher.fetchMeta(loc.owner(), loc.repo(), loc.number());
        String commitId = meta == null ? null : meta.headSha();

        List<Map<String, Object>> comments = (request.comments() == null ? List.<PublishReviewRequest.Comment>of()
                : request.comments()).stream()
                .filter(c -> c.path() != null && c.line() != null
                        && c.body() != null && !c.body().isBlank())
                .map(c -> Map.<String, Object>of("path", c.path(), "line", c.line(), "body", c.body()))
                .toList();

        if (comments.isEmpty()) {
            throw new IllegalArgumentException(
                    "No inline comments to post — each comment needs a file and a line number.");
        }

        try {
            gitHubScmProvider.publishComments(loc.owner(), loc.repo(), loc.number(),
                    commitId, request.summary(), comments);
        } catch (RestClientResponseException e) {
            throw new IllegalArgumentException(
                    "GitHub rejected the review (" + e.getStatusCode().value() + "): "
                            + e.getResponseBodyAsString());
        }
        return Map.of("posted", comments.size());
    }

    @GetMapping("/{owner}/{repo}")
    public List<ReviewHistoryItem> history(@PathVariable String owner, @PathVariable String repo) {
        return queryService.history(owner, repo).stream()
                .map(mapper::toHistoryItem)
                .toList();
    }
}
