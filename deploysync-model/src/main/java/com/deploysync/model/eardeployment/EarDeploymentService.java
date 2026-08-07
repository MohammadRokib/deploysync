package com.deploysync.model.eardeployment;

import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class EarDeploymentService {
    private final DeploymentValidator deploymentValidator;

    public  EarDeploymentService(DeploymentValidator deploymentValidator) {
        this.deploymentValidator = deploymentValidator;
    }

    public DeploymentResult deploy(Path masterEarFile, Path deploymentFolder) {
        DeploymentValidationResult validation = deploymentValidator.validate(deploymentFolder, masterEarFile);
        if (!validation.valid()) {
            return new DeploymentResult(false, "Validation failed:\n" + String.join("\n", validation.errors()));
        }

        return new DeploymentResult(true, "Validation passed: " + validation.entries().size() + " file(s) ready to deploy.");
    }
}
