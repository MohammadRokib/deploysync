package com.deploysync.view.support;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Scene;
import org.springframework.stereotype.Component;

@Component
public class ThemeManager {
    private final BooleanProperty darkMode = new SimpleBooleanProperty(false);
    private Scene scene;
    private String currentStylesheet;

    public BooleanProperty darkModeProperty() {
        return darkMode;
    }

    public void attachTo(Scene scene) {
        this.scene = scene;
    }

    public void applyCurrentTheme() {
        String stylesheet = darkMode.get()
                ? new PrimerDark().getUserAgentStylesheet()
                : new PrimerLight().getUserAgentStylesheet();

        Application.setUserAgentStylesheet(stylesheet);

        if (scene != null) {
            if (currentStylesheet != null) {
                scene.getStylesheets().remove(currentStylesheet);
            }
            scene.getStylesheets().add(stylesheet);
        }

        currentStylesheet = stylesheet;
    }

    public void toggle() {
        darkMode.set(!darkMode.get());
        applyCurrentTheme();
    }
}
