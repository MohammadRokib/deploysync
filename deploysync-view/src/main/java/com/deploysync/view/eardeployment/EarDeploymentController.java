package com.deploysync.view.eardeployment;

import com.deploysync.model.eardeployment.DeploymentResult;
import com.deploysync.model.eardeployment.EarDeploymentService;
import com.deploysync.view.shell.ShellModule;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;


@Component
public class EarDeploymentController implements ShellModule {
    private final EarDeploymentService earDeploymentService;
    private final SimpleBooleanProperty deploying = new SimpleBooleanProperty(false);

    @FXML private TextField masterEarField;
    @FXML private TextField deploymentFolderField;
    @FXML private Button deployButton;
    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;

    public EarDeploymentController(EarDeploymentService earDeploymentService) {
        this.earDeploymentService = earDeploymentService;
    }

    @Override public String displayName() { return "EAR Deployment"; }
    @Override public String fxmlResourcePath() { return "/com/deploysync/view/eardeployment/EarDeployment.fxml"; }

    @FXML
    private void initialize() {
        deployButton.disableProperty().bind(
                masterEarField.textProperty().isEmpty()
                        .or(deploymentFolderField.textProperty().isEmpty())
                        .or(deploying)
        );

        progressBar.setVisible(false);
        progressBar.setManaged(false);
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

        statusLabel.setText("Deploying...");
        progressBar.setProgress(0);
        progressBar.setVisible(true);
        progressBar.setManaged(true);
        deploying.set(true);

        Task<DeploymentResult> task = new Task<>() {
            @Override
            protected DeploymentResult call() {
                return earDeploymentService.deploy(masterEar, deploymentFolder, (completed, total) -> {
                    updateProgress(completed, total);
                    updateMessage("Patching " + completed + " / " + total + " file(s)...");
                });
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> finishDeploy(task.getValue()));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            finishDeploy(new DeploymentResult(false, "Unexpected error: " + (ex != null ? ex.getMessage() : "unknown")));
        });

        Thread thread = new Thread(task, "deploy-worker");
        thread.setDaemon(true);
        thread.start();
    }

    private void finishDeploy(DeploymentResult result) {
        progressBar.progressProperty().unbind();
        statusLabel.textProperty().unbind();
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        statusLabel.setText("");
        deploying.set(false);

        Alert alert = new Alert(result.success() ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        alert.setTitle(result.success() ? "Success" : "Failed");
        alert.setHeaderText(result.success() ? "Deployment succeeded" : "Deployment failed");
        alert.setContentText(result.message());
        alert.showAndWait();
    }

    private Window windowOf(TextField field) {
        return field.getScene().getWindow();
    }
}
