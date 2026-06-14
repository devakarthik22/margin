package com.margin.persistence;

import com.margin.domain.model.*;
import com.margin.persistence.entity.FindingEntity;
import com.margin.persistence.entity.ReviewEntity;
import org.springframework.stereotype.Component;

/** Maps between domain model and JPA entities, keeping persistence types out of the domain. */
@Component
public class ReviewEntityMapper {

    public ReviewEntity toEntity(PullRequestRef ref, ReviewResult result) {
        ReviewEntity entity = new ReviewEntity();
        entity.setOwner(ref.owner());
        entity.setRepo(ref.repo());
        entity.setPrNumber(ref.number());
        entity.setHeadSha(ref.headSha());
        entity.setVerdict(result.verdict().name());
        entity.setSummary(result.summary());
        result.findings().forEach(f -> entity.addFinding(toFindingEntity(f)));
        return entity;
    }

    public ReviewResult toDomain(ReviewEntity entity) {
        var findings = entity.getFindings().stream()
                .map(this::toFinding)
                .toList();
        return new ReviewResult(Verdict.valueOf(entity.getVerdict()), entity.getSummary(), findings);
    }


    public StoredReview toStoredReview(ReviewEntity entity) {
        PullRequestRef ref = new PullRequestRef(
                entity.getOwner(), entity.getRepo(), entity.getPrNumber(), entity.getHeadSha());
        return new StoredReview(ref, toDomain(entity), entity.getCreatedAt());
    }

    private FindingEntity toFindingEntity(Finding f) {
        FindingEntity e = new FindingEntity();
        e.setCategory(f.category().name());
        e.setSeverity(f.severity().name());
        e.setLine(f.line());
        e.setFilePath(f.filePath());
        e.setTitle(f.title());
        e.setExplanation(f.explanation());
        e.setSuggestion(f.suggestion());
        return e;
    }

    private Finding toFinding(FindingEntity e) {
        return Finding.builder()
                .category(Category.valueOf(e.getCategory()))
                .severity(Severity.valueOf(e.getSeverity()))
                .line(e.getLine())
                .filePath(e.getFilePath())
                .title(e.getTitle())
                .explanation(e.getExplanation())
                .suggestion(e.getSuggestion())
                .build();
    }
}
