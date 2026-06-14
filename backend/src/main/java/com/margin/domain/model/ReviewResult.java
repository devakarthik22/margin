package com.margin.domain.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The outcome of a review: an overall verdict, a short summary, and the
 * ordered findings. Findings are sorted by severity weight on construction so
 * every consumer (API, persistence, SCM publisher) sees the same ordering.
 */
public record ReviewResult(Verdict verdict, String summary, List<Finding> findings) {

    public ReviewResult {
        findings = findings.stream()
                .sorted((a, b) -> Integer.compare(a.severity().weight(), b.severity().weight()))
                .collect(Collectors.toUnmodifiableList());
    }

    public Map<Severity, Long> countsBySeverity() {
        return findings.stream()
                .collect(Collectors.groupingBy(Finding::severity, Collectors.counting()));
    }

    public boolean hasBlockingFindings() {
        return findings.stream().anyMatch(f -> f.severity().isBlocking());
    }

    public static ReviewResult clean() {
        return new ReviewResult(Verdict.APPROVE, "No issues found.", List.of());
    }
}
