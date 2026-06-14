package com.margin.review;

import com.margin.agent.AgentEvent;
import com.margin.agent.FileContextProvider;
import com.margin.domain.model.PullRequestRef;
import com.margin.domain.model.ReviewResult;
import com.margin.scm.ScmProviderType;

import java.util.function.Consumer;

/** Entry point for triggering a review. Kept as an interface for testability. */
public interface ReviewService {

    /** Full flow: fetch the PR diff from the host, review, persist, and publish back. */
    ReviewResult reviewPullRequest(PullRequestRef ref, ScmProviderType providerType);

    /**
     * Ad-hoc flow used by the dashboard: review a diff pasted directly, with no
     * source-control round-trip and no publishing. Same agent, same validation.
     */
    ReviewResult reviewRawDiff(String rawDiff);

    /**
     * Same as {@link #reviewRawDiff(String)} but emits lifecycle events to the supplied
     * listener as the review progresses — used by the SSE streaming endpoint.
     */
    ReviewResult reviewRawDiff(String rawDiff, Consumer<AgentEvent> onEvent);

    /**
     * Reviews a diff with a real {@link FileContextProvider} so the agent can pull
     * actual file contents on demand (used by the read-only GitHub PR flow, where
     * files are fetched from the PR head). Emits lifecycle events as it progresses.
     */
    ReviewResult reviewRawDiff(String rawDiff, FileContextProvider fileContext,
                               Consumer<AgentEvent> onEvent);
}
