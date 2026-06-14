package com.margin.domain.model;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** All hunks belonging to one file in a diff. */
public record FileDiff(String path, List<DiffHunk> hunks) {

    public FileDiff {
        hunks = List.copyOf(hunks);
    }

    /** Line numbers in the new file that a comment can legitimately attach to. */
    public Set<Integer> addressableLines() {
        return hunks.stream()
                .flatMap(h -> h.lines().stream())
                .filter(DiffLine::isAddressable)
                .map(DiffLine::newLineNumber)
                .collect(Collectors.toUnmodifiableSet());
    }
}
