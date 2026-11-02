package me.mafl1x.mxyoutubers.core.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class YouTubeUrlUtil {

    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile(
            "(?:youtu\\.be/|youtube\\.com/(?:watch\\?v=|embed/|v/|shorts/))([A-Za-z0-9_-]{11})"
    );

    private YouTubeUrlUtil() {
    }

    public static String extractVideoId(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.matches("[A-Za-z0-9_-]{11}")) {
            return trimmed;
        }
        Matcher matcher = VIDEO_ID_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1);
        }
        int queryIndex = trimmed.indexOf("v=");
        if (queryIndex >= 0 && trimmed.length() >= queryIndex + 13) {
            return trimmed.substring(queryIndex + 2, queryIndex + 13);
        }
        return null;
    }
}
