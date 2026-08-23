package com.deploysync.view.weblogic;

import com.deploysync.model.eardeployment.DeploymentResult;
import com.deploysync.model.profile.EnvironmentProfile;
import com.deploysync.model.profile.ProfileStore;
import com.deploysync.model.weblogic.WeblogicConnection;
import com.deploysync.model.weblogic.WeblogicService;
import com.deploysync.view.shell.ShellModule;
import com.deploysync.view.support.ActiveProfileHolder;
import com.deploysync.view.support.Popups;
import com.deploysync.view.support.ProfileSaves;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
@Order(2)
public class WeblogicControlController implements ShellModule {
    private final WeblogicService weblogicService;
    private final SimpleBooleanProperty busy = new SimpleBooleanProperty(false);

    private final ProfileStore profileStore;
    private final ActiveProfileHolder activeProfileHolder;

    @FXML private ComboBox<String> profileCombo;

    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField appNameField;
    @FXML private TextField targetField;

    @FXML private Button stopButton;
    @FXML private Button startButton;
    @FXML private Button updateButton;
    @FXML private Button checkConnectionButton;

    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;

    public WeblogicControlController(WeblogicService weblogicService, ProfileStore profileStore,
                                     ActiveProfileHolder activeProfileHolder) {
        this.weblogicService = weblogicService;
        this.profileStore = profileStore;
        this.activeProfileHolder = activeProfileHolder;
    }

    @Override public String displayName() { return "WebLogic Control"; }
    @Override public String fxmlResourcePath() { return "/com/deploysync/view/weblogic/WeblogicControl.fxml"; }
    @Override public Ikon icon() { return MaterialDesignS.SERVER; }
    @Override public BooleanExpression busyProperty() { return busy; }

    @FXML
    private void initialize() {
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        BooleanBinding credentialsIncomplete = hostField.textProperty().isEmpty()
                .or(portField.textProperty().isEmpty())
                .or(usernameField.textProperty().isEmpty())
                .or(passwordField.textProperty().isEmpty())
                .or(appNameField.textProperty().isEmpty())
                .or(targetField.textProperty().isEmpty());

        stopButton.disableProperty().bind(credentialsIncomplete.or(busy));
        startButton.disableProperty().bind(credentialsIncomplete.or(busy));
        updateButton.disableProperty().bind(credentialsIncomplete.or(busy));
        checkConnectionButton.disableProperty().bind(credentialsIncomplete.or(busy));

        profileCombo.getItems().setAll(profileStore.list().stream().map(EnvironmentProfile::name).toList());
        String active = activeProfileHolder.get();

        if (!active.isBlank() && profileCombo.getItems().contains(active)) {
            profileCombo.setValue(active);
            profileStore.find(active).ifPresent(this::applyProfile);
        }

        profileCombo.valueProperty().addListener((obs, oldName, newNmae) -> {
            activeProfileHolder.set(newNmae);
            if (newNmae != null) {
                profileStore.find(newNmae).ifPresent(this::applyProfile);
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

        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            Popups.showError("Invalid port", "Port must be a number");
            return;
        }

        EnvironmentProfile base = profileStore.find(name).orElse(EnvironmentProfile.blank(name));
        EnvironmentProfile merged = base.withName(name.trim()).withWeblogicFields(
                hostField.getText().trim(), port, usernameField.getText().trim(),
                appNameField.getText().trim(), targetField.getText().trim()
        );

        if (ProfileSaves.confirmAndSave(profileStore, merged)) {
            activeProfileHolder.set(merged.name());
            profileCombo.getItems().setAll(profileStore.list().stream().map(EnvironmentProfile::name).toList());
            profileCombo.setValue(merged.name());
        }
    }

    @FXML
    private void onStop() {
        runAction("Stopping service", "Service stopped", "Stop failed", weblogicService::stop);
    }

    @FXML
    private void onStart() {
        runAction("Starting Service", "Service started", "Start failed", weblogicService::start);
    }

    @FXML
    private void onUpdate() {
        runAction("Updating service", "Service updated", "Update failed", weblogicService::redeploy);
    }

    @FXML
    private void onCheckConnection() {
        runAction("Checking", "Connection OK", "Connection failed", weblogicService::checkState);
    }

    private void runAction(String statusText, String successTitle, String failureTitle,
                            Function<WeblogicConnection, DeploymentResult> action) {
        WeblogicConnection connection = readConnection();
        if (connection == null) {
            return;
        }

        statusLabel.setText(statusText);
        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressBar.setVisible(true);
        progressBar.setManaged(true);
        busy.set(true);

        Task<DeploymentResult> task = new Task<>() {
            @Override
            protected DeploymentResult call() {
                return action.apply(connection);
            }
        };

        task.setOnSucceeded(e -> finishAction(successTitle, failureTitle, task.getValue()));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            finishAction(successTitle, failureTitle,
                    new DeploymentResult(false, "Unexpected error: " + (ex != null ? ex.getMessage() : "unknown")));
        });

        Thread thread = new Thread(task, "weblogic-worker");
        thread.setDaemon(true);
        thread.start();
    }

    private void finishAction(String successTitle, String failureTitle, DeploymentResult result) {
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        statusLabel.setText("");
        busy.set(false);

        Popups.showResult(successTitle, failureTitle, result.success(), result.message());
    }

    private WeblogicConnection readConnection() {
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            Popups.showError("Invalid port", "Port must be a number.");
            return null;
        }

        return new WeblogicConnection(
                hostField.getText().trim(),
                port,
                usernameField.getText().trim(),
                passwordField.getText(),
                appNameField.getText().trim(),
                targetField.getText().trim());
    }

    private void applyProfile(EnvironmentProfile profile) {
        hostField.setText(profile.weblogicHost());
        portField.setText(String.valueOf(profile.weblogicPort()));
        usernameField.setText(profile.weblogicUsername());
        appNameField.setText(profile.weblogicAppname());
        targetField.setText(profile.weblogicTarget());
    }
}
