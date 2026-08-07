package com.deploysync.model.eardeployment;

import java.util.List;

public record DeploymentValidationResult(boolean valid, List<ManifestEntry> entries, List<String> errors) {
    public static DeploymentValidationResult success(List<ManifestEntry> entries) {
        return new DeploymentValidationResult(true, entries, List.of());
    }

    public static DeploymentValidationResult failure(List<String> errors) {
        return new DeploymentValidationResult(false, List.of(), errors);
    }
}
