package com.deploysync.view.shell;

import com.deploysync.view.support.AppIcon;
import com.deploysync.view.support.HostServicesHolder;
import com.deploysync.view.support.SpringFxmlLoader;
import com.deploysync.view.support.ThemeManager;
import com.deploysync.view.support.WindowChrome;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MainShellController {
    private static final String REPO_URL = "https://github.com/MohammadRokib/deploysync";

    private final SpringFxmlLoader fxmlLoader;
    private final ThemeManager themeManager;
    private final HostServicesHolder hostServicesHolder;
    private final BuildProperties buildProperties;
    private final Map<String, ShellModule> modulesByName = new LinkedHashMap<>();
    private final Map<String, Tab> openTabsByModule = new LinkedHashMap<>();

    @FXML private ListView<String> moduleList;
    @FXML private TabPane tabPane;
    @FXML private Button themeToggleButton;
    @FXML private Hyperlink versionLink;
    @FXML private HBox titleBar;
    @FXML private ImageView headerIcon;
    @FXML private Button minimizeButton;
    @FXML private Button maximizeButton;
    @FXML private Button closeButton;

    private final FontIcon maximizeIcon = new FontIcon(MaterialDesignW.WINDOW_MAXIMIZE);
    private WindowChrome windowChrome;

    public MainShellController(SpringFxmlLoader fxmlLoader, ThemeManager themeManager,
                                HostServicesHolder hostServicesHolder, BuildProperties buildProperties,
                                List<ShellModule> modules) {
        this.fxmlLoader = fxmlLoader;
        this.themeManager = themeManager;
        this.hostServicesHolder = hostServicesHolder;
        this.buildProperties = buildProperties;
        for (ShellModule module : modules) {
            modulesByName.put(module.displayName(), module);
        }
    }

    @FXML
    private void initialize() {
        moduleList.getItems().addAll(modulesByName.keySet());
        versionLink.setText("DeploySync v" + buildProperties.getVersion());

        headerIcon.setImage(AppIcon.render(18));
        minimizeButton.setGraphic(new FontIcon(MaterialDesignW.WINDOW_MINIMIZE));
        maximizeButton.setGraphic(maximizeIcon);
        closeButton.setGraphic(new FontIcon(MaterialDesignW.WINDOW_CLOSE));

        titleBar.sceneProperty().flatMap(Scene::windowProperty).subscribe(window -> {
            if (window instanceof Stage stage) {
                windowChrome = WindowChrome.install(stage, titleBar.getScene(), titleBar);
                windowChrome.maximizedProperty().addListener((o, wasMaximized, isMaximized) ->
                        maximizeIcon.setIconCode(isMaximized ? MaterialDesignW.WINDOW_RESTORE : MaterialDesignW.WINDOW_MAXIMIZE));
            }
        });

        themeToggleButton.textProperty().bind(
                Bindings.when(themeManager.darkModeProperty()).then("Light Mode").otherwise("Dark Mode"));

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
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item);
                        ShellModule module = modulesByName.get(item);
                        setGraphic(module != null ? new FontIcon(module.icon()) : null);
                    }
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

        SplitPane.setResizableWithParent(moduleList, false);
    }

    @FXML
    private void onToggleTheme() {
        themeManager.toggle();
    }

    @FXML
    private void onOpenRepo() {
        hostServicesHolder.openUrl(REPO_URL);
    }

    @FXML
    private void onMinimize() {
        ((Stage) titleBar.getScene().getWindow()).setIconified(true);
    }

    @FXML
    private void onMaximizeRestore() {
        windowChrome.toggleMaximize();
    }

    @FXML
    private void onClose() {
        ((Stage) titleBar.getScene().getWindow()).close();
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
            tab.setGraphic(new FontIcon(module.icon()));
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
        if (tabPane.getSelectionModel().getSelectedItem() != tab) {
            tabPane.getSelectionModel().select(tab);
        }
    }
}
