package com.deploysync.model.eardeployment;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class NestedArchivePathResolver {
    private static final Set<String> ARCHIVE_EXTENSIONS = Set.of(".war", ".jar", ".ear", ".zip", ".rar");

    private NestedArchivePathResolver() {}

    public static List<String> splitSegments(String target) {
        return Arrays.stream(target.split("/"))
                .map(String::trim)
                .filter(segment -> !segment.isEmpty())
                .toList();
    }

    public static boolean isArchiveSegment(String segment) {
        String lower = segment.toLowerCase(Locale.ROOT);
        return ARCHIVE_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    public static boolean existsInMasterFile(Path masterFile, List<String> segments) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(masterFile)) {
            return existsRecursive(fs.getPath("/"), segments, 0);
        }
    }

    private static boolean existsRecursive(Path currentRoot, List<String> segments, int startIndex) throws IOException {
        Path current = currentRoot;
        for (int i = startIndex; i < segments.size(); i++) {
            String segment = segments.get(i);
            current = current.resolve(segment);
            boolean isLastSegment = (i == segments.size() - 1);

            if (!isLastSegment && isArchiveSegment(segment)) {
                if (!Files.isRegularFile(current)) {
                    return false;
                }

                try (FileSystem nestedFs = FileSystems.newFileSystem(current)) {
                    return existsRecursive(nestedFs.getPath("/"), segments, i+1);
                }
            }
        }
        return Files.isRegularFile(current);
    }
}
