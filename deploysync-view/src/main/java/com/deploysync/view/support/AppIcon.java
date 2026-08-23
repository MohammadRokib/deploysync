package com.deploysync.view.support;

import javafx.scene.image.Image;

import java.net.URL;

public final class AppIcon {
    private static final URL SOURCE = AppIcon.class.getResource("app-icon.png");

    private AppIcon() {}

    public static Image render(int size) {
        return new Image(SOURCE.toExternalForm(), size, size, true, true);
    }
}
