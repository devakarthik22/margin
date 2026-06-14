package com.margin.agent.gemini;

import com.margin.agent.ReviewContext;
import com.margin.domain.model.CodeDiff;
import com.margin.domain.model.ReviewResult;
import com.margin.domain.model.Verdict;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GeminiCodeReviewAgentTest {

    @Test
    void emptyDiffShortCircuitsWithoutCallingTheModel() {
        ChatClient chat = mock(ChatClient.class);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(chat);

        var agent = new GeminiCodeReviewAgent(builder, new ReviewPromptFactory(), new AgentResponseMapper());

        ReviewResult result = agent.review(new ReviewContext(new CodeDiff(List.of()), path -> ""));

        assertThat(result.verdict()).isEqualTo(Verdict.APPROVE);
        assertThat(result.findings()).isEmpty();
        // The expensive part — the model call — is never reached for an empty diff.
        verifyNoInteractions(chat);
    }
}
