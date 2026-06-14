package com.margin.api;

import com.margin.api.dto.FindingDto;
import com.margin.api.dto.PrFilesResponse;
import com.margin.api.dto.ReviewResponse;
import com.margin.api.dto.ReviewHistoryItem;
import com.margin.domain.model.CodeDiff;
import com.margin.domain.model.DiffLine;
import com.margin.domain.model.ReviewResult;
import com.margin.domain.model.StoredReview;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Maps the domain result to the transport DTO. Keeps controllers thin. */
@Component
public class ApiMapper {

    public ReviewResponse toResponse(ReviewResult result) {
        var findings = result.findings().stream()
                .map(f -> new FindingDto(
                        f.category().name().toLowerCase(),
                        f.severity().name().toLowerCase(),
                        f.line(), f.filePath(), f.title(), f.explanation(), f.suggestion()))
                .toList();

        Map<String, Long> counts = result.countsBySeverity().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name().toLowerCase(), Map.Entry::getValue));

        return new ReviewResponse(
                result.verdict().name().toLowerCase(),
                result.summary(),
                findings,
                counts);
    }

    /** Maps a parsed diff + PR coordinates into the file-view DTO. */
    public PrFilesResponse toPrFiles(CodeDiff diff, String owner, String repo,
                                     int number, String title) {
        var files = diff.files().stream().map(file -> {
            List<PrFilesResponse.PrLine> lines = new ArrayList<>();
            int additions = 0, deletions = 0;
            for (var hunk : file.hunks()) {
                lines.add(new PrFilesResponse.PrLine("hunk", null,
                        "@@ +" + hunk.newStart() + "," + hunk.newCount() + " @@"));
                for (DiffLine l : hunk.lines()) {
                    lines.add(new PrFilesResponse.PrLine(
                            type(l.type()), l.newLineNumber(), l.content()));
                    if (l.type() == DiffLine.Type.ADDED) additions++;
                    else if (l.type() == DiffLine.Type.REMOVED) deletions++;
                }
            }
            return new PrFilesResponse.PrFile(file.path(), additions, deletions, lines);
        }).toList();
        return new PrFilesResponse(owner, repo, number, title, files);
    }

    private String type(DiffLine.Type t) {
        return switch (t) {
            case ADDED -> "add";
            case REMOVED -> "del";
            case CONTEXT -> "ctx";
        };
    }

    public ReviewHistoryItem toHistoryItem(StoredReview stored) {
        var ref = stored.ref();
        var result = stored.result();
        return new ReviewHistoryItem(
                ref.slug(),
                ref.number(),
                ref.headSha(),
                result.verdict().name().toLowerCase(),
                result.summary(),
                result.findings().size(),
                stored.createdAt());
    }
}
