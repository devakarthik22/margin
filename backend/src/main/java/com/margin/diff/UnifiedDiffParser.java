package com.margin.diff;

import com.margin.domain.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses standard unified diff text (the format GitHub returns for a PR patch).
 * Tracks the new-file line number for every added and context line so that
 * downstream validation can confirm an agent's reported line actually exists.
 */
@Component
public class UnifiedDiffParser implements DiffParser {

    private static final Pattern FILE_HEADER =
            Pattern.compile("^\\+\\+\\+ b/(.+)$");
    private static final Pattern HUNK_HEADER =
            Pattern.compile("^@@ -\\d+(?:,\\d+)? \\+(\\d+)(?:,(\\d+))? @@.*");

    @Override
    public CodeDiff parse(String rawDiff) {
        if (rawDiff == null || rawDiff.isBlank()) {
            return new CodeDiff(List.of());
        }

        List<FileDiff> files = new ArrayList<>();
        String currentPath = null;
        List<DiffHunk> hunks = new ArrayList<>();
        List<DiffLine> hunkLines = null;
        int newStart = 0, newCount = 0, newLine = 0;

        for (String line : rawDiff.split("\n", -1)) {
            Matcher fileMatcher = FILE_HEADER.matcher(line);
            Matcher hunkMatcher = HUNK_HEADER.matcher(line);

            if (line.startsWith("--- ")) {
                // Start of a new file block: flush whatever we were building.
                flushHunk(hunks, hunkLines, newStart, newCount);
                hunkLines = null;
                flushFile(files, currentPath, hunks);
                currentPath = null;
                hunks = new ArrayList<>();
            } else if (fileMatcher.matches()) {
                currentPath = fileMatcher.group(1);
            } else if (hunkMatcher.matches()) {
                flushHunk(hunks, hunkLines, newStart, newCount);
                newStart = Integer.parseInt(hunkMatcher.group(1));
                newCount = hunkMatcher.group(2) == null ? 1 : Integer.parseInt(hunkMatcher.group(2));
                newLine = newStart;
                hunkLines = new ArrayList<>();
            } else if (hunkLines != null) {
                if (line.startsWith("+")) {
                    hunkLines.add(new DiffLine(DiffLine.Type.ADDED, line.substring(1), newLine++));
                } else if (line.startsWith("-")) {
                    hunkLines.add(new DiffLine(DiffLine.Type.REMOVED, line.substring(1), null));
                } else if (line.startsWith(" ")) {
                    hunkLines.add(new DiffLine(DiffLine.Type.CONTEXT, line.substring(1), newLine++));
                }
            }
        }
        flushHunk(hunks, hunkLines, newStart, newCount);
        flushFile(files, currentPath, hunks);
        return new CodeDiff(files);
    }

    private void flushHunk(List<DiffHunk> hunks, List<DiffLine> lines, int start, int count) {
        if (lines != null && !lines.isEmpty()) {
            hunks.add(new DiffHunk(start, count, lines));
        }
    }

    private void flushFile(List<FileDiff> files, String path, List<DiffHunk> hunks) {
        if (path != null && !hunks.isEmpty()) {
            files.add(new FileDiff(path, List.copyOf(hunks)));
        }
    }
}
