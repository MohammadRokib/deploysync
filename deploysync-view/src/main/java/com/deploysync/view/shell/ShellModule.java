package com.deploysync.view.shell;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleBooleanProperty;

public interface ShellModule {
    String displayName();
    String fxmlResourcePath();
    default BooleanExpression busyProperty() {
        return new SimpleBooleanProperty(false);
    }
}
