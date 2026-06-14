package com.margin.review;

import com.margin.domain.model.StoredReview;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Read side of the review use case (kept separate from the write-oriented
 * {@link ReviewService} so each stays cohesive). Depends only on the
 * {@link ReviewStore} port.
 */
@Service
public class ReviewQueryService {

    private final ReviewStore store;

    public ReviewQueryService(ReviewStore store) {
        this.store = store;
    }

    public List<StoredReview> history(String owner, String repo) {
        return store.history(owner, repo);
    }
}
