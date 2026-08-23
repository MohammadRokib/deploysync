package com.deploysync.view.profile;

import com.deploysync.model.profile.EnvironmentProfile;
import com.deploysync.model.profile.ProfileStore;
import com.deploysync.view.shell.ShellModule;
import com.deploysync.view.support.ActiveProfileHolder;
import com.deploysync.view.support.Popups;
import com.deploysync.view.support.ProfileSaves;
import javafx.fxml.FXML;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@Order(3)
public class ProfileController implements ShellModule {
    private final ProfileStore profileStore;
    private final ActiveProfileHolder activeProfileHolder;

    @FXML private ComboBox<String> profileCombo;
    @FXML private TextField masterEarField;
    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField usernameField;
    @FXML private TextField appNameField;
    @FXML private TextField targetField;
    @FXML private Button duplicateButton;
    @FXML private Button deleteButton;
    @FXML private Label statusLabel;

    public ProfileController(ProfileStore profileStore, ActiveProfileHolder activeProfileHolder) {
        this.profileStore = profileStore;
        this.activeProfileHolder = activeProfileHolder;
    }

    @Override public String displayName() { return "Configuration"; }
    @Override public String fxmlResourcePath() { return "/com/deploysync/view/profile/Profile.fxml"; }
    @Override public Ikon icon() { return MaterialDesignC.COG_OUTLINE; }

    @FXML
    private void initialize() {
        refreshProfileList();

        String active = activeProfileHolder.get();
        if (!active.isBlank() &&  profileCombo.getItems().contains(active)) {
            profileCombo.setValue(active);
            profileStore.find(active).ifPresent(this::applyToFields);
        }

        profileCombo.valueProperty().addListener((obs, oldName, newName) -> {
            activeProfileHolder.set(newName);
            if (newName != null) {
                profileStore.find(newName).ifPresent(this::applyToFields);
            }
        });

        deleteButton.disableProperty().bind(profileCombo.valueProperty().isNull());
        duplicateButton.disableProperty().bind(profileCombo.valueProperty().isNull());
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
    private void onNew() {
        profileCombo.setValue(null);
        activeProfileHolder.set("");
        clearFields();
        statusLabel.setText("");
    }

    @FXML
    private void onDuplicate() {
        String selected = profileCombo.getValue();
        if (selected == null) {
            return;
        }

        profileStore.find(selected).ifPresent(profile -> {
            String newName = profileStore.suggestDuplicateName(selected);
            profileCombo.setValue(newName);
            applyToFields(profile.withName(newName));
            statusLabel.setText("Duplicate '" + selected + "' as '" + newName + "' -review and save.");
        });
    }

    @FXML
    private void onSave() {
        String name = profileCombo.getValue();
        if (name == null || name.isBlank()) {
            Popups.showError("Name required", "Enter a profile name before saving.");
            return;
        }

        int port;
        try {
            port = portField.getText().isBlank() ? 7001 : Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            Popups.showError("Invalid port", "Port must be a number.");
            return;
        }

        EnvironmentProfile profile = new EnvironmentProfile(
                name.trim(), masterEarField.getText().trim(),
                hostField.getText().trim(), port, usernameField.getText().trim(),
                appNameField.getText().trim(), targetField.getText().trim()
        );

        if (!ProfileSaves.confirmAndSave(profileStore, profile)) {
            return;
        }

        activeProfileHolder.set(profile.name());
        refreshProfileList();
        profileCombo.setValue(profile.name());
        statusLabel.setText("Saved '" + profile.name() + "'.");
    }

    @FXML
    private void onDelete() {
        String name = profileCombo.getValue();
        if (name == null) {
            return;
        }

        profileStore.delete(name);
        if (activeProfileHolder.get().equals(name)) {
            activeProfileHolder.set("");
        }

        onNew();
        refreshProfileList();
        statusLabel.setText("Deleted '" + name + "'.");
    }

    private void applyToFields(EnvironmentProfile profile) {
        masterEarField.setText(profile.masterEarPath());
        hostField.setText(profile.weblogicHost());
        portField.setText(String.valueOf(profile.weblogicPort()));
        usernameField.setText(profile.weblogicUsername());
        appNameField.setText(profile.weblogicAppname());
        targetField.setText(profile.weblogicTarget());
    }

    private void clearFields() {
        masterEarField.clear();
        hostField.clear();
        portField.clear();
        usernameField.clear();
        appNameField.clear();
        targetField.clear();
    }

    private void refreshProfileList() {
        String currentValue = profileCombo.getValue();
        profileCombo.getItems().setAll(profileStore.list().stream().map(EnvironmentProfile::name).toList());
        profileCombo.setValue(currentValue);
    }

    private Window windowOf(TextField field) {
        return field.getScene().getWindow();
    }
}
