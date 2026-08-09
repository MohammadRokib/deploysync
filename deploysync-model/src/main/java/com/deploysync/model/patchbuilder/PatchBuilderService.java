package com.deploysync.model.patchbuilder;

import com.deploysync.model.eardeployment.DeploymentResult;
import com.deploysync.model.eardeployment.ManifestEntry;
import com.deploysync.model.eardeployment.NestedArchivePatcher;
import com.deploysync.model.eardeployment.NestedArchivePathResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class PatchBuilderService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DeploymentResult extract(Path masterFile, List<ManifestEntry> entries, Path destinationFolder) {
        if (entries.isEmpty()) {
            return new DeploymentResult(false, "No files selected.");
        }

        try {
            Path filesFolder = destinationFolder.resolve("files");
            Files.createDirectories(filesFolder);

            for (ManifestEntry entry : entries) {
                List<String> segments = NestedArchivePathResolver.resolveEntrySegments(entry.target(), entry.source());
                byte[] content = NestedArchivePatcher.readEntry(masterFile, segments);
                Files.write(filesFolder.resolve(entry.source()), content);
            }

            Path manifestFile = destinationFolder.resolve("manifest.json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(manifestFile.toFile(), entries);

            return new DeploymentResult(true, entries.size() + " file(s) extracted to " + destinationFolder);
        } catch (IOException e) {
            return new DeploymentResult(false, "Extraction failed " + e.getMessage());
        }
    }
}
