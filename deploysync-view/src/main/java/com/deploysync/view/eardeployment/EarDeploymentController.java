package com.deploysync.view.eardeployment;

import com.deploysync.model.eardeployment.DeploymentResult;
import com.deploysync.model.eardeployment.EarDeploymentService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;


@Component
public class EarDeploymentController {
    private final EarDeploymentService earDeploymentService;

    @FXML private TextField masterEarField;
    @FXML private TextField deploymentFolderField;
    @FXML private Button deployButton;
    @FXML private Label statusLabel;

    public EarDeploymentController(EarDeploymentService earDeploymentService) {
        this.earDeploymentService = earDeploymentService;
    }

    @FXML
    private void initialize() {
        deployButton.disableProperty().bind(
                masterEarField.textProperty().isEmpty()
                        .or(deploymentFolderField.textProperty().isEmpty())
        );
    }

    @FXML
    private void onBrowseMasterEar() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select master .ear file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("EAR files", "*.ear"));

        File selected = chooser.showOpenDialog(windowOf(masterEarField));
        if (selected != null) {
            masterEarField.setText(selected.getAbsolutePath());
        }
    }

    @FXML
    private void onBrowseDeploymentFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select new-deployment-files folder");

        File selected = chooser.showDialog(windowOf(deploymentFolderField));
        if (selected != null) {
            deploymentFolderField.setText(selected.getAbsolutePath());
        }
    }

    @FXML
    private void onDeploy() {
        Path masterEar = Path.of(masterEarField.getText());
        Path deploymentFolder = Path.of(deploymentFolderField.getText());

        DeploymentResult result = earDeploymentService.deploy(masterEar, deploymentFolder);
        statusLabel.setText((result.success() ? "Success!\n" : "Failed!\n") + result.message());
        statusLabel.setStyle(result.success() ? "-fx-text-fill: #2e7d32;" : "-fx-text-fill: #c62828;");
    }

    private Window windowOf(TextField field) {
        return field.getScene().getWindow();
    }
}
