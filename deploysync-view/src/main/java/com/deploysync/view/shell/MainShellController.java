package com.deploysync.view.shell;

import com.deploysync.view.support.SpringFxmlLoader;
import javafx.fxml.FXML;

import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MainShellController {
    private final SpringFxmlLoader fxmlLoader;
    private final Map<String, ShellModule> modulesByName = new LinkedHashMap<>();
    private final Map<String , Tab> openTabsByModule = new LinkedHashMap<>();
    private boolean syncingSelection = false;

    @FXML private ListView<String> moduleList;
    @FXML private TabPane tabPane;
    @FXML private StackPane contentPane;

    public MainShellController(SpringFxmlLoader fxmlLoader, List<ShellModule> modules) {
        this.fxmlLoader = fxmlLoader;
        for (ShellModule module : modules) {
            modulesByName.put(module.displayName(), module);
        }
    }

    @FXML
    private void initialize() {
        moduleList.getItems().addAll(modulesByName.keySet());

        moduleList.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                openOrFocusModule(moduleList.getSelectionModel().getSelectedItem());
            }
        });

        moduleList.setCellFactory(list -> {
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                }
            };
            cell.setOnMouseClicked(event -> {
                if (!cell.isEmpty()) {
                    openOrFocusModule(cell.getItem());
                }
            });

            return cell;
        });

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                moduleList.getSelectionModel().select((String) newTab.getUserData());
            }
        });

        SplitPane splitPane = (SplitPane) moduleList.getParent();
        SplitPane.setResizableWithParent(moduleList, false);
    }

    private void openOrFocusModule(String moduleName) {
        if (moduleName == null) {
            return;
        }

        Tab existing = openTabsByModule.get(moduleName);
        if (existing != null) {
            selectWithoutFeedback(existing);
            return;
        }

        ShellModule module = modulesByName.get(moduleName);
        if (module == null) {
            throw new IllegalArgumentException("Unknown module: " + moduleName);
        }
        
        try {
            Parent view = fxmlLoader.load(module.fxmlResourcePath());
            Tab tab = new Tab(moduleName, view);
            tab.setUserData(moduleName);
            tab.closableProperty().bind(module.busyProperty().not());
            tab.setOnClosed(e -> openTabsByModule.remove(moduleName));

            openTabsByModule.put(moduleName, tab);
            tabPane.getTabs().add(tab);
            selectWithoutFeedback(tab);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load module view: " + module.fxmlResourcePath(), e);
        }
    }

    private void selectWithoutFeedback(Tab tab) {
        if (tabPane.getSelectionModel().getSelectedItem() == tab) {
            return;
        }

        syncingSelection = true;
        tabPane.getSelectionModel().select(tab);
        syncingSelection = false;
    }
}
