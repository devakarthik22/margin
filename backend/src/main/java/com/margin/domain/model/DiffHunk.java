package com.margin.domain.model;

import java.util.List;

/** A contiguous block of changes within a file, as delimited by an @@ header. */
public record DiffHunk(int newStart, int newCount, List<DiffLine> lines) {
    public DiffHunk {
        lines = List.copyOf(lines);
    }
}
