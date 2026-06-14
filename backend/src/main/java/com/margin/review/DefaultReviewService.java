package com.margin.review;

import com.margin.agent.AgentEvent;
import com.margin.agent.CodeReviewAgent;
import com.margin.agent.FileContextProvider;
import com.margin.agent.ReviewContext;
import com.margin.diff.DiffParser;
import com.margin.domain.model.CodeDiff;
import com.margin.domain.model.PullRequestRef;
import com.margin.domain.model.ReviewResult;
import com.margin.scm.ScmProvider;
import com.margin.scm.ScmProviderFactory;
import com.margin.scm.ScmProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * Orchestrates one review end to end. This is the application layer: it owns the
 * sequence (resolve provider -> fetch -> parse -> agent -> validate -> persist ->
 * publish) but delegates every step to a collaborator behind an interface. It
 * depends on abstractions only, so each step is independently swappable and testable.
 */
@Service
public class DefaultReviewService implements ReviewService {

    private static final Logger log = LoggerFactory.getLogger(DefaultReviewService.class);

    private final ScmProviderFactory scmFactory;
    private final DiffParser diffParser;
    private final CodeReviewAgent agent;
    private final FindingValidator validator;
    private final ReviewStore store;

    public DefaultReviewService(ScmProviderFactory scmFactory,
                                DiffParser diffParser,
                                CodeReviewAgent agent,
                                FindingValidator validator,
                                ReviewStore store) {
        this.scmFactory = scmFactory;
        this.diffParser = diffParser;
        this.agent = agent;
        this.validator = validator;
        this.store = store;
    }

    @Override
    public ReviewResult reviewPullRequest(PullRequestRef ref, ScmProviderType providerType) {
        // Idempotency: never re-review the same commit (saves cost + avoids dup comments).
        var existing = store.findByHeadSha(ref.headSha());
        if (existing.isPresent()) {
            log.info("Review for {} (sha {}) already exists; returning cached result.",
                    ref.slug(), ref.headSha());
            return existing.get();
        }

        ScmProvider provider = scmFactory.get(providerType);

        String rawDiff = provider.fetchRawDiff(ref);
        CodeDiff diff = diffParser.parse(rawDiff);

        ReviewContext context = new ReviewContext(diff, path -> provider.fetchFileContent(ref, path));
        ReviewResult raw = agent.review(context);

        ReviewResult validated = validator.validate(raw, diff);

        store.save(ref, validated);
        provider.publishReview(ref, validated);

        log.info("Reviewed {}: verdict={} findings={}",
                ref.slug(), validated.verdict(), validated.findings().size());
        return validated;
    }

    @Override
    public ReviewResult reviewRawDiff(String rawDiff) {
        return reviewRawDiff(rawDiff, event -> {});
    }

    @Override
    public ReviewResult reviewRawDiff(String rawDiff, Consumer<AgentEvent> onEvent) {
        // No real source available (pasted diff): tell the model so, and review hunks only.
        FileContextProvider unavailable = path ->
                "File content is not available in raw-diff review mode. " +
                "Please review based solely on the diff hunks provided.";
        return reviewRawDiff(rawDiff, unavailable, onEvent);
    }

    @Override
    public ReviewResult reviewRawDiff(String rawDiff, FileContextProvider fileContext,
                                      Consumer<AgentEvent> onEvent) {
        CodeDiff diff = diffParser.parse(rawDiff);
        int fileCount = diff.files().size();
        long hunkCount = diff.files().stream().mapToLong(f -> f.hunks().size()).sum();
        onEvent.accept(new AgentEvent("sys",
                "parse diff → " + fileCount + " file(s), " + hunkCount + " hunk(s)"));

        // Wrap the real provider so each tool call the model makes surfaces as a
        // trace event the dashboard can render live.
        FileContextProvider traced = path -> {
            onEvent.accept(new AgentEvent("tool", "getFileContent(\"" + path + "\")"));
            String content = fileContext.getFileContent(path);
            onEvent.accept(new AgentEvent("ret",
                    content == null ? "empty" : content.length() + " chars"));
            return content;
        };

        onEvent.accept(new AgentEvent("sys", "scan · bug · security · performance · convention"));
        ReviewContext context = new ReviewContext(diff, traced);
        ReviewResult raw = agent.review(context);
        onEvent.accept(new AgentEvent("sys", "compose margin notes"));
        return validator.validate(raw, diff);
    }
}
