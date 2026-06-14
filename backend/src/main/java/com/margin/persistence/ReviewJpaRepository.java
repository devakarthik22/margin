package com.margin.persistence;

import com.margin.persistence.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Spring Data repository for review aggregates. */
public interface ReviewJpaRepository extends JpaRepository<ReviewEntity, Long> {
    Optional<ReviewEntity> findByHeadSha(String headSha);

    List<ReviewEntity> findByOwnerAndRepoOrderByCreatedAtDesc(String owner, String repo);
}
