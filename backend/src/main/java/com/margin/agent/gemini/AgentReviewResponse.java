package com.margin.agent.gemini;

import java.util.List;

/**
 * The raw shape we ask the model to return. Deliberately separate from the
 * domain {@link com.margin.domain.model.ReviewResult}: the LLM contract is an
 * anti-corruption boundary, so prompt/model changes never leak into the domain.
 */
public record AgentReviewResponse(String verdict, String summary, List<Item> findings) {

    public record Item(
            String category,
            String severity,
            Integer line,
            String filePath,
            String title,
            String explanation,
            String suggestion) {
    }
}
