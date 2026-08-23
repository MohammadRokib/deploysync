package com.deploysync.view.support;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public final class Popups {
    private Popups() {}

    public static void showResult(String successHeader, String failureHeader, boolean success, String message) {
        Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        alert.setTitle(success ? successHeader : failureHeader);
        alert.setHeaderText(success ? successHeader : failureHeader);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static boolean confirmOverwrite(String profileName) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Overwrite profile");
        alert.setHeaderText("Profile '" + profileName + "' already exists");
        alert.setContentText("Overwrite it with the current values?");
        return alert.showAndWait().filter(button -> button == ButtonType.OK).isPresent();
    }

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
