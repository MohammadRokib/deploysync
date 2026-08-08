package com.deploysync.model.eardeployment;

import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class EarDeploymentService {
    private final DeploymentValidator deploymentValidator;
    private final DeploymentPatcher deploymentPatcher;

    public  EarDeploymentService(DeploymentValidator deploymentValidator, DeploymentPatcher deploymentPatcher) {
        this.deploymentValidator = deploymentValidator;
        this.deploymentPatcher = deploymentPatcher;
    }

    public DeploymentResult deploy(Path masterEarFile, Path deploymentFolder) {
        DeploymentValidationResult validation = deploymentValidator.validate(deploymentFolder, masterEarFile);
        if (!validation.valid()) {
            return new DeploymentResult(false, "Validation failed:\n" + String.join("\n", validation.errors()));
        }

        return deploymentPatcher.patch(masterEarFile, validation.filesFolder(), validation.entries());
    }
}
