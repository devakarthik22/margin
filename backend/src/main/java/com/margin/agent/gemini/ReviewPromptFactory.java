package com.margin.agent.gemini;

import com.margin.domain.model.CodeDiff;
import com.margin.domain.model.FileDiff;
import org.springframework.stereotype.Component;

/**
 * Builds the system and user prompts. Isolated from the agent so prompt wording
 * can evolve (and be unit-tested) independently of the model-calling code.
 */
@Component
public class ReviewPromptFactory {

    public String system() {
        return """
            You are Margin, a meticulous senior code-review agent.
            Review the supplied diff for correctness, security, performance,
            convention, and maintainability. When a hunk lacks context, call the
            getFileContent tool rather than guessing.

            Report only the most important findings (at most six), each tied to a
            specific added line where possible. Be concrete and concise. If the
            change is clean, return an empty findings list with verdict APPROVE.
            Choose verdict REQUEST_CHANGES only when a finding is CRITICAL or HIGH.

            YOU MUST respond with ONLY a valid JSON object — no prose, no markdown
            code fences, no explanation before or after the JSON. The schema is:
            {
              "verdict": "APPROVE|COMMENT|REQUEST_CHANGES",
              "summary": "one-sentence overall assessment",
              "findings": [
                {
                  "category": "SECURITY|BUG|PERFORMANCE|CONVENTION|MAINTAINABILITY",
                  "severity": "CRITICAL|HIGH|MEDIUM|LOW",
                  "line": <line number or null>,
                  "filePath": "<file path>",
                  "title": "<short title>",
                  "explanation": "<detailed explanation>",
                  "suggestion": "<suggested fix or null>"
                }
              ]
            }
            """;
    }

    public String user(CodeDiff diff) {
        StringBuilder sb = new StringBuilder("Review the following diff:\n\n");
        for (FileDiff file : diff.files()) {
            sb.append("=== ").append(file.path()).append(" ===\n");
            file.hunks().forEach(h -> h.lines().forEach(l -> sb
                    .append(symbol(l.type()))
                    .append(l.newLineNumber() == null ? "    " : pad(l.newLineNumber()))
                    .append(" ").append(l.content()).append("\n")));
            sb.append("\n");
        }
        return sb.toString();
    }

    private String symbol(com.margin.domain.model.DiffLine.Type t) {
        return switch (t) {
            case ADDED -> "+";
            case REMOVED -> "-";
            case CONTEXT -> " ";
        };
    }

    private String pad(int n) {
        return String.format("%4d", n);
    }
}
