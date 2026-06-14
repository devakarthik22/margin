package com.margin.review;

import com.margin.domain.model.*;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Guards against the model's most common failure: citing a line that isn't part
 * of the change. A finding whose line is not an addressable line in the diff has
 * its line cleared (kept as a PR-level note) rather than posted to a wrong place.
 */
@Component
public class FindingValidator {

    public ReviewResult validate(ReviewResult result, CodeDiff diff) {
        var validated = result.findings().stream()
                .map(f -> isLineValid(f, diff) ? f : f.withoutLine())
                .toList();
        return new ReviewResult(result.verdict(), result.summary(), validated);
    }

    private boolean isLineValid(Finding finding, CodeDiff diff) {
        if (finding.line() == null || finding.filePath() == null) {
            return false;
        }
        Set<Integer> addressable = diff.file(finding.filePath())
                .map(FileDiff::addressableLines)
                .orElse(Set.of());
        return addressable.contains(finding.line());
    }
}
