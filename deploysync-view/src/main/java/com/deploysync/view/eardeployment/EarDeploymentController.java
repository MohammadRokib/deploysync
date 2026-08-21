package com.deploysync.view.eardeployment;

import com.deploysync.model.eardeployment.DeploymentResult;
import com.deploysync.model.eardeployment.EarDeploymentService;
import com.deploysync.model.profile.EnvironmentProfile;
import com.deploysync.model.profile.ProfileStore;
import com.deploysync.view.shell.ShellModule;
import com.deploysync.view.support.ActiveProfileHolder;
import com.deploysync.view.support.Popups;
import com.deploysync.view.support.ProfileSaves;
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
    private final ProfileStore profileStore;
    private final ActiveProfileHolder activeProfileHolder;
    private final SimpleBooleanProperty deploying = new SimpleBooleanProperty(false);

    @FXML private ComboBox<String> profileCombo;
    @FXML private TextField masterEarField;
    @FXML private TextField deploymentFolderField;
    @FXML private Button deployButton;
    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;

    public EarDeploymentController(EarDeploymentService earDeploymentService, ProfileStore profileStore,
                                   ActiveProfileHolder activeProfileHolder) {
        this.earDeploymentService = earDeploymentService;
        this.profileStore = profileStore;
        this.activeProfileHolder = activeProfileHolder;
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

        profileCombo.getItems().setAll(profileStore.list().stream().map(EnvironmentProfile::name).toList());
        String active = activeProfileHolder.get();

        if (!active.isBlank() && profileCombo.getItems().contains(active)) {
            profileCombo.setValue(active);
            profileStore.find(active).ifPresent(profile -> masterEarField.setText(profile.masterEarPath()));
        }

        profileCombo.valueProperty().addListener((obs, oldName, newName) -> {
            activeProfileHolder.set(newName);
            if (newName != null) {
                profileStore.find(newName).ifPresent(profile -> masterEarField.setText(profile.masterEarPath()));
            }
        });
    }

    @FXML
    private void onSaveProfile() {
        String name = profileCombo.getValue();
        if (name == null || name.isBlank()) {
            Popups.showError("Name required", "Select or type a profile name before saving.");
            return;
        }

        EnvironmentProfile base = profileStore.find(name).orElse(EnvironmentProfile.blank(name));
        EnvironmentProfile merged = base.withName(name.trim()).withMasterEarPath(masterEarField.getText().trim());

        if (ProfileSaves.confirmAndSave(profileStore, merged)) {
            activeProfileHolder.set(merged.name());
            profileCombo.getItems().setAll(profileStore.list().stream().map(EnvironmentProfile::name).toList());
            profileCombo.setValue(merged.name());
        }
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
