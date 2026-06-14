package com.margin.review;

import com.margin.domain.model.PullRequestRef;
import com.margin.domain.model.ReviewResult;
import com.margin.domain.model.StoredReview;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port. The orchestrator depends on this, not on JPA. The adapter
 * lives in the persistence package, so swapping the datastore never touches
 * application logic (Dependency Inversion).
 */
public interface ReviewStore {

    /** Most recent review for a given commit SHA, if one exists (idempotency check). */
    Optional<ReviewResult> findByHeadSha(String headSha);

    void save(PullRequestRef ref, ReviewResult result);

    /** Past reviews for a repository, newest first. */
    List<StoredReview> history(String owner, String repo);
}
