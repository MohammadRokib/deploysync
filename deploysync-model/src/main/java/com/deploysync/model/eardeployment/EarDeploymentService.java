package com.deploysync.model.eardeployment;

import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class EarDeploymentService {
    public DeploymentResult deploy(Path masterEarFile, Path deploymentFilesFolder) {
        return new DeploymentResult(
                true,
                "Received master EAR: " + masterEarFile + " | deployment files: " + deploymentFilesFolder
        );
    }
}
