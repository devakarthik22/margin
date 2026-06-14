package com.margin.persistence;

import com.margin.domain.model.PullRequestRef;
import com.margin.domain.model.ReviewResult;
import com.margin.domain.model.StoredReview;
import com.margin.review.ReviewStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementing the {@link ReviewStore} port. The only place that
 * knows both the domain and the persistence model; everything else stays clean.
 */
@Component
public class JpaReviewStore implements ReviewStore {

    private final ReviewJpaRepository repository;
    private final ReviewEntityMapper mapper;

    public JpaReviewStore(ReviewJpaRepository repository, ReviewEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewResult> findByHeadSha(String headSha) {
        return repository.findByHeadSha(headSha).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void save(PullRequestRef ref, ReviewResult result) {
        repository.save(mapper.toEntity(ref, result));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredReview> history(String owner, String repo) {
        return repository.findByOwnerAndRepoOrderByCreatedAtDesc(owner, repo).stream()
                .map(mapper::toStoredReview)
                .toList();
    }
}
