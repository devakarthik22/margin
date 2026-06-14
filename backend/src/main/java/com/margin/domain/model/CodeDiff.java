package com.margin.domain.model;

import java.util.List;
import java.util.Optional;

/** A parsed diff across one or more files. */
public record CodeDiff(List<FileDiff> files) {

    public CodeDiff {
        files = List.copyOf(files);
    }

    public Optional<FileDiff> file(String path) {
        return files.stream().filter(f -> f.path().equals(path)).findFirst();
    }

    public boolean isEmpty() {
        return files.isEmpty();
    }
}
