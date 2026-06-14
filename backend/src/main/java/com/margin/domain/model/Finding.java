package com.margin.domain.model;

import java.util.Objects;

/**
 * A single review note. Immutable; constructed via {@link #builder()} so that
 * the agent layer and the persistence layer build it the same, validated way.
 * {@code line} is nullable — a finding may apply to the change as a whole.
 */
public record Finding(
        Category category,
        Severity severity,
        Integer line,
        String filePath,
        String title,
        String explanation,
        String suggestion) {

    public Finding {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(severity, "severity");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("finding title must not be blank");
        }
    }

    /** Returns a copy with the line cleared — used when validation rejects a line ref. */
    public Finding withoutLine() {
        return new Finding(category, severity, null, filePath, title, explanation, suggestion);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Category category;
        private Severity severity;
        private Integer line;
        private String filePath;
        private String title;
        private String explanation;
        private String suggestion;

        public Builder category(Category v) { this.category = v; return this; }
        public Builder severity(Severity v) { this.severity = v; return this; }
        public Builder line(Integer v) { this.line = v; return this; }
        public Builder filePath(String v) { this.filePath = v; return this; }
        public Builder title(String v) { this.title = v; return this; }
        public Builder explanation(String v) { this.explanation = v; return this; }
        public Builder suggestion(String v) { this.suggestion = v; return this; }

        public Finding build() {
            return new Finding(category, severity, line, filePath, title, explanation, suggestion);
        }
    }
}
