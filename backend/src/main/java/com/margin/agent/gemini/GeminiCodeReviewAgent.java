package com.margin.agent.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.margin.agent.CodeReviewAgent;
import com.margin.agent.ReviewContext;
import com.margin.agent.tool.FileContextTool;
import com.margin.domain.model.ReviewResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * {@link CodeReviewAgent} backed by Google Gemini via Spring AI. Responsibilities
 * kept narrow: assemble the call (prompt + per-review tool), ask for a structured
 * response, and hand off mapping. Prompt-building and domain-mapping live in
 * their own collaborators (Single Responsibility).
 */
@Component
public class GeminiCodeReviewAgent implements CodeReviewAgent {

    private static final Logger log = LoggerFactory.getLogger(GeminiCodeReviewAgent.class);

    private final ChatClient chat;
    private final ReviewPromptFactory prompts;
    private final AgentResponseMapper mapper;
    private final ObjectMapper json;

    public GeminiCodeReviewAgent(ChatClient.Builder chatBuilder,
                                 ReviewPromptFactory prompts,
                                 AgentResponseMapper mapper,
                                 ObjectMapper json) {
        this.chat = chatBuilder.build();
        this.prompts = prompts;
        this.mapper = mapper;
        this.json = json;
    }

    @Override
    public ReviewResult review(ReviewContext context) {
        if (context.diff().isEmpty()) {
            return ReviewResult.clean();
        }

        FileContextTool tool = new FileContextTool(context.fileContext());

        String raw = chat.prompt()
                .system(prompts.system())
                .user(prompts.user(context.diff()))
                .tools(tool)
                .call()
                .content();

        log.debug("Raw model response:\n{}", raw);

        AgentReviewResponse response = parseResponse(raw);
        return mapper.toDomain(response);
    }

    /**
     * Gemini may wrap its JSON in markdown code fences. Strip them before parsing.
     * Logs a warning and returns null (→ clean result) if the response is still
     * unparseable after stripping, so one bad model reply never crashes a review.
     */
    private AgentReviewResponse parseResponse(String raw) {
        if (raw == null || raw.isBlank()) {
            log.warn("Model returned blank response — treating as clean review");
            return null;
        }
        String stripped = raw.strip();
        // Remove ```json ... ``` or ``` ... ``` wrappers
        if (stripped.startsWith("```")) {
            int first = stripped.indexOf('\n');
            int last  = stripped.lastIndexOf("```");
            if (first != -1 && last > first) {
                stripped = stripped.substring(first + 1, last).strip();
            }
        }
        // Find the outermost JSON object in case the model prefixed prose
        int start = stripped.indexOf('{');
        int end   = stripped.lastIndexOf('}');
        if (start != -1 && end > start) {
            stripped = stripped.substring(start, end + 1);
        }
        try {
            return json.readValue(stripped, AgentReviewResponse.class);
        } catch (Exception e) {
            log.warn("Could not parse model response as AgentReviewResponse: {}", stripped, e);
            return null;
        }
    }
}
