package com.margin.scm.github;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses a GitHub pull-request web URL into owner / repo / number. */
public final class GitHubPrUrl {

    private static final Pattern PR_URL = Pattern.compile(
            "^https?://github\\.com/([^/\\s]+)/([^/\\s]+)/pull/(\\d+)");

    public record Location(String owner, String repo, int number) {}

    private GitHubPrUrl() {}

    /**
     * @throws IllegalArgumentException if the URL is not a recognisable GitHub PR link
     */
    public static Location parse(String url) {
        if (url == null) {
            throw new IllegalArgumentException("PR URL is required.");
        }
        Matcher m = PR_URL.matcher(url.trim());
        if (!m.find()) {
            throw new IllegalArgumentException(
                    "Not a GitHub pull-request URL. Expected https://github.com/<owner>/<repo>/pull/<number>");
        }
        return new Location(m.group(1), m.group(2), Integer.parseInt(m.group(3)));
    }
}
