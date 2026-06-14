package com.margin.agent;

import com.margin.domain.model.CodeDiff;

/**
 * Everything the agent needs for one review: the parsed diff to reason over,
 * and a {@link FileContextProvider} it can call back into when a hunk needs
 * surrounding code. Bundling them keeps the agent signature stable as inputs grow.
 */
public record ReviewContext(CodeDiff diff, FileContextProvider fileContext) {
}
