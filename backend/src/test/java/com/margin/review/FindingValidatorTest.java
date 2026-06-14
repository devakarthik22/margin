package com.margin.review;

import com.margin.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FindingValidatorTest {

    private final FindingValidator validator = new FindingValidator();

    private CodeDiff diffWithAddressableLine(int line) {
        var hunkLine = new DiffLine(DiffLine.Type.ADDED, "x", line);
        var hunk = new DiffHunk(line, 1, List.of(hunkLine));
        return new CodeDiff(List.of(new FileDiff("App.java", List.of(hunk))));
    }

    @Test
    void clearsLineWhenItIsNotPartOfTheDiff() {
        var finding = Finding.builder()
                .category(Category.BUG).severity(Severity.HIGH)
                .filePath("App.java").line(999).title("Hallucinated line").build();
        var result = new ReviewResult(Verdict.COMMENT, "s", List.of(finding));

        var validated = validator.validate(result, diffWithAddressableLine(11));

        assertThat(validated.findings().get(0).line()).isNull();
    }

    @Test
    void keepsLineWhenItIsAddressable() {
        var finding = Finding.builder()
                .category(Category.BUG).severity(Severity.HIGH)
                .filePath("App.java").line(11).title("Real line").build();
        var result = new ReviewResult(Verdict.COMMENT, "s", List.of(finding));

        var validated = validator.validate(result, diffWithAddressableLine(11));

        assertThat(validated.findings().get(0).line()).isEqualTo(11);
    }
}
