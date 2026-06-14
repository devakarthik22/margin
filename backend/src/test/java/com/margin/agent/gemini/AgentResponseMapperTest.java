package com.margin.agent.gemini;

import com.margin.domain.model.Category;
import com.margin.domain.model.ReviewResult;
import com.margin.domain.model.Severity;
import com.margin.domain.model.Verdict;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The anti-corruption mapper is pure logic — fully testable in isolation. */
class AgentResponseMapperTest {

    private final AgentResponseMapper mapper = new AgentResponseMapper();

    @Test
    void nullResponseMapsToClean() {
        ReviewResult result = mapper.toDomain(null);
        assertThat(result.verdict()).isEqualTo(Verdict.APPROVE);
        assertThat(result.findings()).isEmpty();
    }

    @Test
    void unknownEnumsFallBackInsteadOfThrowing() {
        var item = new AgentReviewResponse.Item(
                "not-a-category", "catastrophic", 12, "A.java", "Title", "why", "fix");
        var response = new AgentReviewResponse("nonsense-verdict", "summary", List.of(item));

        ReviewResult result = mapper.toDomain(response);

        assertThat(result.verdict()).isEqualTo(Verdict.COMMENT);             // default
        assertThat(result.findings()).hasSize(1);
        assertThat(result.findings().get(0).category()).isEqualTo(Category.MAINTAINABILITY);
        assertThat(result.findings().get(0).severity()).isEqualTo(Severity.LOW);
    }

    @Test
    void malformedFindingIsSkippedNotFatal() {
        var bad = new AgentReviewResponse.Item("bug", "high", 1, "A.java", "  ", "", "");
        var good = new AgentReviewResponse.Item("security", "critical", 2, "A.java", "Real", "x", "y");
        var response = new AgentReviewResponse("request_changes", "s", List.of(bad, good));

        ReviewResult result = mapper.toDomain(response);

        assertThat(result.findings()).hasSize(1);
        assertThat(result.findings().get(0).title()).isEqualTo("Real");
    }
}
