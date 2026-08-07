package com.deploysync.view.shell;

import com.deploysync.model.eardeployment.EarDeploymentService;
import com.deploysync.view.support.SpringFxmlLoader;
import javafx.fxml.FXML;

import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MainShellController {
    private static final String EAR_DEPLOYMENT_MODULE = "EAR Deployment";
    private final SpringFxmlLoader fxmlLoader;

    @FXML private ListView<String> moduleList;
    @FXML private StackPane contentPane;

    public MainShellController(SpringFxmlLoader fxmlLoader) {
        this.fxmlLoader = fxmlLoader;
    }

    @FXML
    private void initialize() {
        moduleList.getItems().add(EAR_DEPLOYMENT_MODULE);
        moduleList.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldModule, newModule) -> showModule(newModule));
    }

    private void showModule(String moduleName) {
        String resourcePath = switch (moduleName) {
            case EAR_DEPLOYMENT_MODULE -> "/com/deploysync/view/eardeployment/EarDeployment.fxml";
            default -> throw new IllegalArgumentException("Unknown module: " + moduleName);
        };

        try {
            Parent view = fxmlLoader.load(resourcePath);
            contentPane.getChildren().setAll(view);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load module view: " + resourcePath, e);
        }
    }
}
