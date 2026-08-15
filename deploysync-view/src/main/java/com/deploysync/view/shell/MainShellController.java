package com.deploysync.view.shell;

import com.deploysync.view.support.SpringFxmlLoader;
import javafx.fxml.FXML;

import javafx.scene.Parent;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.StackPane;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MainShellController {
    private final SpringFxmlLoader fxmlLoader;
    private final Map<String, String> resourceByModule = new LinkedHashMap<>();

    @FXML private ListView<String> moduleList;
    @FXML private StackPane contentPane;

    public MainShellController(SpringFxmlLoader fxmlLoader, List<ShellModule> modules) {
        this.fxmlLoader = fxmlLoader;
        for (ShellModule module : modules) {
            resourceByModule.put(module.displayName(), module.fxmlResourcePath());
        }
    }

    @FXML
    private void initialize() {
        moduleList.getItems().addAll(resourceByModule.keySet());
        moduleList.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldModule, newModule) -> showModule(newModule));

        SplitPane splitPane = (SplitPane) moduleList.getParent();
        SplitPane.setResizableWithParent(moduleList, false);
    }

    private void showModule(String moduleName) {
        String resourcePath = resourceByModule.get(moduleName);
        if (resourcePath == null) {
            throw new IllegalArgumentException("Unknown module: " + moduleName);
        }

        try {
            Parent view = fxmlLoader.load(resourcePath);
            contentPane.getChildren().setAll(view);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load module view: " + resourcePath, e);
        }
    }
}
