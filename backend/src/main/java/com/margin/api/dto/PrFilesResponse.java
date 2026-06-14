package com.margin.api.dto;

import java.util.List;

/**
 * The parsed contents of a pull request, shaped for the dashboard's file-tree /
 * diff view. Each file carries its changed lines (added/removed/context) plus
 * hunk separators, so the UI can render a GitHub-style diff.
 */
public record PrFilesResponse(
        String owner,
        String repo,
        int number,
        String title,
        List<PrFile> files) {

    public record PrFile(String path, int additions, int deletions, List<PrLine> lines) {}

    /** type: "add" | "del" | "ctx" | "hunk" (a @@ separator). */
    public record PrLine(String type, Integer line, String content) {}
}
