package com.margin.agent.gemini;

import com.margin.domain.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Translates the model's loose response into the strict domain model, defending
 * against unknown enum values and missing fields. Anything unparseable is
 * dropped rather than allowed to corrupt a review.
 */
@Component
public class AgentResponseMapper {

    public ReviewResult toDomain(AgentReviewResponse response) {
        if (response == null) {
            return ReviewResult.clean();
        }
        List<Finding> findings = (response.findings() == null ? List.<AgentReviewResponse.Item>of()
                : response.findings()).stream()
                .map(this::toFinding)
                .filter(java.util.Objects::nonNull)
                .toList();

        Verdict verdict = parseEnum(Verdict.class, response.verdict(), Verdict.COMMENT);
        String summary = response.summary() == null ? "" : response.summary();
        return new ReviewResult(verdict, summary, findings);
    }

    private Finding toFinding(AgentReviewResponse.Item item) {
        try {
            return Finding.builder()
                    .category(parseEnum(Category.class, item.category(), Category.MAINTAINABILITY))
                    .severity(parseEnum(Severity.class, item.severity(), Severity.LOW))
                    .line(item.line())
                    .filePath(item.filePath())
                    .title(item.title())
                    .explanation(item.explanation())
                    .suggestion(item.suggestion())
                    .build();
        } catch (RuntimeException ex) {
            return null; // malformed finding -> skip
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String raw, E fallback) {
        if (raw == null) return fallback;
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
