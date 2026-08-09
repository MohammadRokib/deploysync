package com.deploysync.model.eardeployment;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@Service
public class DeploymentValidator {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DeploymentValidationResult validate(Path deploymentFolder, Path masterEarFile) {
        List<String> errors = new ArrayList<>();

        if (!Files.isDirectory(deploymentFolder)) {
            return DeploymentValidationResult.failure(List.of("Deployment folder does not exist: " + deploymentFolder));
        }

        Path manifestFile = findSingle(deploymentFolder, Files::isRegularFile, ".json", errors, "manifest JSON file");
        Path filesFolder = findSingle(deploymentFolder, Files::isDirectory, null, errors, "files subfolder");

        if (filesFolder != null && isEmptyDirectory(filesFolder)) {
            errors.add("Files subfolder is empty: " + filesFolder);
        }

        if (manifestFile == null || filesFolder == null || !errors.isEmpty()) {
            return DeploymentValidationResult.failure(errors);
        }

        List<ManifestEntry> entries;
        try {
            entries = List.of(objectMapper.readValue(manifestFile.toFile(), ManifestEntry[].class));
        } catch (IOException e) {
            return DeploymentValidationResult.failure(List.of("Failed to parse manifest JSON: " + e.getMessage()));
        }

        if (entries.isEmpty()) {
            return DeploymentValidationResult.failure(List.of("Manifest contains no entries: " + manifestFile));
        }

        for (ManifestEntry entry : entries) {
            validateEntry(entry, filesFolder, masterEarFile, errors);
        }

        validateNoOrphanFiles(entries, filesFolder, errors);

        return errors.isEmpty() ? DeploymentValidationResult.success(entries, filesFolder)
                                : DeploymentValidationResult.failure(errors);
    }

    private void validateEntry(ManifestEntry entry, Path filesFolder, Path masterEarFile, List<String> errors) {
        if (entry.source() == null || entry.source().isBlank()) {
            errors.add("Manifest entry has an empty source");
            return;
        }

        if (entry.target() == null || entry.target().isBlank()) {
            errors.add("Manifest entry '" + entry.source() + "' has an empty target");
            return;
        }

        Path sourceFile = filesFolder.resolve(entry.source());
        if (!Files.isRegularFile(sourceFile)) {
            errors.add("Source file not found in deployment folder: " + entry.source());
        }
    }

    private Path findSingle(Path dir, Predicate<Path> typeFilter, String requiredSuffix,
                            List<String> errors, String label) {
        List<Path> matches = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (typeFilter.test(entry) && (requiredSuffix == null || entry.toString().endsWith(requiredSuffix))) {
                    matches.add(entry);
                }
            }
        } catch (IOException e) {
            errors.add("Failed to read deployment folder: " + e.getMessage());
            return null;
        }

        if (matches.isEmpty()) {
            errors.add("Deployment folder is missing a " + label);
            return null;
        }

        if (matches.size() > 1) {
            errors.add("Deployment folder has more than one " + label + ": " + matches);
            return null;
        }

        return matches.getFirst();
    }

    private boolean isEmptyDirectory(Path dir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            return !stream.iterator().hasNext();
        } catch (IOException e) {
            return true;
        }
    }

    private void validateNoOrphanFiles(List<ManifestEntry> entries, Path filesFolder, List<String> errors) {
        Set<String> referencedNames = new HashSet<>();
        for (ManifestEntry entry : entries) {
            if (entry.source() != null) {
                referencedNames.add(entry.source());
            }
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(filesFolder)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file) && !referencedNames.contains(file.getFileName().toString())) {
                    errors.add("File in deployment folder is not referenced by any manifest entry: " + file.getFileName());
                }
            }
        } catch (IOException e) {
            errors.add("Failed to read files subfolder: " + e.getMessage());
        }
    }
}
