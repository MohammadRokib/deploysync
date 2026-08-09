package com.deploysync.model.eardeployment;

import java.nio.file.Path;
import java.util.*;

public final class NestedArchivePathResolver {
    private static final Set<String> ARCHIVE_EXTENSIONS = Set.of(".war", ".jar", ".ear", ".zip", ".rar");

    private NestedArchivePathResolver() {}

    public static List<String> splitSegments(String target) {
        return Arrays.stream(target.split("[/\\\\]"))
                .map(String::trim)
                .filter(segment -> !segment.isEmpty())
                .toList();
    }

    public static List<String> resolveEntrySegments(String target, String source) {
        List<String> segments = new ArrayList<>(splitSegments(target));
        segments.add(Path.of(source).getFileName().toString());
        return segments;
    }

    public static boolean isArchiveSegment(String segment) {
        String lower = segment.toLowerCase(Locale.ROOT);
        return ARCHIVE_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }
}
