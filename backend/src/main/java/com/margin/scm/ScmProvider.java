package com.margin.scm;

import com.margin.domain.model.PullRequestRef;
import com.margin.domain.model.ReviewResult;

/**
 * Strategy for talking to a specific source-control host. Each host (GitHub,
 * GitLab, ...) provides one implementation; the orchestration layer depends only
 * on this interface, never on a concrete host. Adding a host = adding a bean.
 */
public interface ScmProvider {

    /** The host this strategy handles. Used by the factory for lookup. */
    ScmProviderType type();

    /** Raw unified-diff text for the pull request. */
    String fetchRawDiff(PullRequestRef ref);

    /** Full current contents of a file at the PR's head — used by the agent's tool. */
    String fetchFileContent(PullRequestRef ref, String path);

    /** Publish the review back to the host as a summary plus inline comments. */
    void publishReview(PullRequestRef ref, ReviewResult result);
}
