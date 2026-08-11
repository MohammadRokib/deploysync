package com.deploysync.view.support;

import javafx.scene.control.Alert;

public final class Popups {
    private Popups() {}

    public static void showResult(String successHeader, String failureHeader, boolean success, String message) {
        Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        alert.setTitle(success ? successHeader : failureHeader);
        alert.setHeaderText(success ? successHeader : failureHeader);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
