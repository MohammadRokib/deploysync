package com.deploysync.model.patchbuilder;

import com.deploysync.model.eardeployment.NestedArchivePathResolver;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NestedArchiveExplorer {
    private NestedArchiveExplorer()  {}

    public static List<ArchiveNode> listChildren(Path masterFile, List<String> segments) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(masterFile)) {
            return listRecursive(fs.getPath("/"), segments, 0);
        }
    }

    private static List<ArchiveNode> listRecursive(Path currentRoot, List<String> segments, int startIndex) throws IOException {
        Path current = currentRoot;
        for (int i = startIndex; i < segments.size(); i++) {
            current = current.resolve(segments.get(i));
            if (NestedArchivePathResolver.isArchiveSegment(segments.get(i))) {
                try (FileSystem nestedFs = FileSystems.newFileSystem(current)) {
                    return listRecursive(nestedFs.getPath("/"), segments, i+1);
                }
            }
        }

        return listDirectory(current, segments);
    }

    private static List<ArchiveNode> listDirectory(Path dir, List<String> parentSegments) throws IOException {
        List<ArchiveNode> nodes = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                if (name.endsWith("/")) {
                    name = name.substring(0, name.length()-1);
                }

                boolean directory = Files.isDirectory(entry);
                boolean archive = !directory && NestedArchivePathResolver.isArchiveSegment(name);

                List<String> childSegments = new ArrayList<>(parentSegments);
                childSegments.add(name);

                nodes.add(new ArchiveNode(name, List.copyOf(childSegments), directory, archive));
            }
        }

        nodes.sort(Comparator.comparing(ArchiveNode::name, String.CASE_INSENSITIVE_ORDER));
        return nodes;
    }
}
