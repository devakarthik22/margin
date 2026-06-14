package com.margin.agent;

import com.margin.domain.model.ReviewResult;

/**
 * The reviewing brain. A port: the orchestration layer depends on this
 * interface, not on Gemini or any specific model. Swapping models, or stubbing
 * the agent in tests, means providing another implementation — nothing else moves.
 */
public interface CodeReviewAgent {
    ReviewResult review(ReviewContext context);
}
