package com.deploysync.model.eardeployment;

import java.nio.file.Path;
import java.util.List;

public record DeploymentValidationResult(boolean valid, List<ManifestEntry> entries, Path filesFolder, List<String> errors) {
    public static DeploymentValidationResult success(List<ManifestEntry> entries, Path filesFolder) {
        return new DeploymentValidationResult(true, entries, filesFolder, List.of());
    }

    public static DeploymentValidationResult failure(List<String> errors) {
        return new DeploymentValidationResult(false, List.of(), null, errors);
    }
}
