package com.deploysync.view.shell;

import com.deploysync.model.eardeployment.EarDeploymentService;
import javafx.fxml.FXML;

import javafx.scene.control.Label;
import org.springframework.stereotype.Component;

@Component
public class MainShellController {
    private final EarDeploymentService earDeploymentService;

    @FXML
    private Label greetingLabel;

    public MainShellController(EarDeploymentService earDeploymentService) {
        this.earDeploymentService = earDeploymentService;
    }

    @FXML
    private void initialize() {
        greetingLabel.setText(earDeploymentService.status());
    }
}
