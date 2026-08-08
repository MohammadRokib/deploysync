package com.deploysync.view.patchbuilder;

import com.deploysync.model.eardeployment.DeploymentResult;
import com.deploysync.model.eardeployment.ManifestEntry;
import com.deploysync.model.patchbuilder.ArchiveNode;
import com.deploysync.model.patchbuilder.PatchBuilderService;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class PatchBuilderController {
    private final PatchBuilderService patchBuilderService;
    private final ObservableList<ManifestEntry> selectedFiles = FXCollections.observableArrayList();
    private Path masterFile;

    @FXML private TextField masterFileField;
    @FXML private Button openButton;
    @FXML private Button extractButton;
    @FXML private TreeView<ArchiveNode> archiveTree;
    @FXML private ListView<ManifestEntry> selectedFilesList;

    public PatchBuilderController(PatchBuilderService patchBuilderService) {
        this.patchBuilderService = patchBuilderService;
    }

    @FXML
    private void initialize() {
        extractButton.disableProperty().bind(Bindings.isEmpty(selectedFiles));

        selectedFilesList.setItems(selectedFiles);
        selectedFilesList.setCellFactory(list -> new SelectedFileCell());

        archiveTree.setCellFactory(tree -> new TreeCell<>() {
            @Override
            protected void updateItem(ArchiveNode item, boolean empty) {
                super.updateItem(item, empty);
                textProperty().unbind();

                if (empty || item == null) {
                    setText(null);
                } else if (item.directory() || item.archive()) {
                    textProperty().bind(Bindings.when(getTreeItem().expandedProperty())
                            .then(item.name())
                            .otherwise(item.name() + "/"));
                } else {
                    setText(item.name());
                }
            }
        });

        archiveTree.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem == null || newItem.getValue().directory()) {
                return;
            }
            addSelection(newItem.getValue());
        });
    }

    @FXML
    private void onSelectMasterFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select master file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("EAR files", "*.ear"));

        File selected = chooser.showOpenDialog(windowOf(masterFileField));
        if (selected != null) {
            masterFileField.setText(selected.getAbsolutePath());
        }
    }

    @FXML
    private void onOpen() {
        String text = masterFileField.getText();
        if (text == null || text.isBlank()) {
            return;
        }

        Path selected = Path.of(text);
        if (!Files.isRegularFile(selected)) {
            showError("File not found", selected + " does not exist.");
            return;
        }

        ArchiveNode rootNode = new ArchiveNode(selected.getFileName().toString(), List.of(), true, false);
        ArchiveTreeItem root = new ArchiveTreeItem(rootNode, selected);
        try {
            root.getChildren();
        } catch (UncheckedIOException e) {
            showError("Failed to open file", e.getCause().getMessage());
            return;
        }

        masterFile = selected;
        selectedFiles.clear();
        root.setExpanded(true);
        archiveTree.setRoot(root);
    }

    @FXML
    private void onExtract() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select destination folder");

        File selected = chooser.showDialog(windowOf(masterFileField));
        if (selected == null) {
            return;
        }

        DeploymentResult result = patchBuilderService.extract(masterFile, List.copyOf(selectedFiles), selected.toPath());
        if (!result.success()) {
            showError("Extraction failed", result.message());
        }
    }

    private void addSelection(ArchiveNode node) {
        List<String> segments = node.segments();
        String source = segments.get(segments.size()-1);
        String target = String.join("/", segments.subList(0, segments.size()-1));
        ManifestEntry entry = new ManifestEntry(source, target);

        if (selectedFiles.stream().noneMatch(existing -> existing.equals(entry))) {
            selectedFiles.add(entry);
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Window windowOf(TextField field) {
        return field.getScene().getWindow();
    }

    private final class SelectedFileCell extends ListCell<ManifestEntry> {
        private final Button removeButton = new Button("X");
        private final Label nameLabel = new Label();
        private final HBox root = new HBox(8, removeButton, nameLabel);

        SelectedFileCell() {
            removeButton.setOnAction(e -> {
                ManifestEntry item = getItem();
                if (item != null) {
                    selectedFiles.remove(item);
                }
            });
        }

        @Override
        protected void updateItem(ManifestEntry item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(item.source());
                setGraphic(root);
            }
        }
    }
}
