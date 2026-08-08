package com.deploysync.model.eardeployment;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class NestedArchivePatcher {
    private NestedArchivePatcher() {}

    public static void writeEntry(Path masterFile, List<String> segments, byte[] content) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(masterFile)) {
            writeRecursive(fs.getPath("/"), segments, 0, content);
        }
    }

    public static byte[] readEntry(Path masterFile, List<String> segments) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(masterFile)) {
            return readRecursive(fs.getPath("/"), segments, 0);
        }
    }

    private static void writeRecursive(Path currentRoot, List<String> segments, int startIndex, byte[] content) throws IOException  {
        Path current = currentRoot;
        for (int i = startIndex; i < segments.size(); i++) {
            current = current.resolve((segments.get(i)));
            boolean isLast = (i == segments.size()-1);

            if (!isLast && NestedArchivePathResolver.isArchiveSegment(segments.get(i))) {
                try (FileSystem nestedFs = FileSystems.newFileSystem(current)) {
                    writeRecursive(nestedFs.getPath("/"), segments, i+1, content);
                }
                return;
            }
        }

        Files.createDirectories(current.getParent());
        Files.write(current, content);
    }

    private static byte[] readRecursive(Path currentRoot, List<String> segments, int startIndex) throws IOException {
        Path current = currentRoot;
        for (int i = startIndex; i < segments.size(); i++) {
            current = current.resolve(segments.get(i));
            boolean isLast = (i == segments.size()-1);

            if (!isLast && NestedArchivePathResolver.isArchiveSegment(segments.get(i))) {
                try (FileSystem nestedFs = FileSystems.newFileSystem(current)) {
                    return readRecursive(nestedFs.getPath("/"), segments, i+1);
                }
            }
        }

        return Files.readAllBytes(current);
    }
}
