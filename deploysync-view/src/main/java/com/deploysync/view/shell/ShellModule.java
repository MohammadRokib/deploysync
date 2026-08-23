package com.deploysync.view.shell;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleBooleanProperty;
import org.kordamp.ikonli.Ikon;

public interface ShellModule {
    String displayName();
    String fxmlResourcePath();
    Ikon icon();
    default BooleanExpression busyProperty() {
        return new SimpleBooleanProperty(false);
    }
}
