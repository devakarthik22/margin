package com.margin.diff;

import com.margin.domain.model.CodeDiff;

/**
 * Parses raw patch text into a structured {@link CodeDiff}.
 * An interface so the unified-diff implementation can be swapped or wrapped
 * (e.g. with a caching decorator) without touching callers.
 */
public interface DiffParser {
    CodeDiff parse(String rawDiff);
}
