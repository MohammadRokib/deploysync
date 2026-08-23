package com.deploysync.view.patchbuilder;

import com.deploysync.model.eardeployment.DeploymentResult;
import com.deploysync.model.eardeployment.ManifestEntry;
import com.deploysync.model.patchbuilder.ArchiveNode;
import com.deploysync.model.patchbuilder.PatchBuilderService;
import com.deploysync.view.shell.ShellModule;
import com.deploysync.view.support.Popups;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Component
@Order(4)
public class PatchBuilderController implements ShellModule {
    private final PatchBuilderService patchBuilderService;
    private final SimpleBooleanProperty extracting = new SimpleBooleanProperty(false);
    private final ObservableList<ManifestEntry> selectedFiles = FXCollections.observableArrayList();
    private Path masterFile;

    @FXML private TextField masterFileField;
    @FXML private TextField extractionFolderField;

    @FXML private Button openButton;
    @FXML private Button extractButton;

    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;

    @FXML private TreeView<ArchiveNode> archiveTree;
    @FXML private ListView<ManifestEntry> selectedFilesList;

    public PatchBuilderController(PatchBuilderService patchBuilderService) {
        this.patchBuilderService = patchBuilderService;
    }

    @Override public String displayName() { return "Patch Management"; }
    @Override public String fxmlResourcePath() { return "/com/deploysync/view/patchbuilder/PatchBuilder.fxml"; }
    @Override public Ikon icon() { return MaterialDesignW.WRENCH; }
    @Override public BooleanExpression busyProperty() { return extracting; }

    @FXML
    private void initialize() {
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        openButton.disableProperty().bind(
                extractionFolderField.textProperty().isEmpty()
                        .or(masterFileField.textProperty().isEmpty()));

        extractButton.disableProperty().bind(
                Bindings.isEmpty(selectedFiles)
                        .or(extractionFolderField.textProperty().isEmpty())
                        .or(masterFileField.textProperty().isEmpty())
                        .or(extracting));

        selectedFilesList.setItems(selectedFiles);
        selectedFilesList.setCellFactory(list -> new SelectedFileCell());

        archiveTree.setCellFactory(tree -> {
            TreeCell<ArchiveNode> cell = new TreeCell<>() {
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
            };

            cell.setOnMouseClicked(event -> {
                Node disclosureNode = cell.getDisclosureNode();

                if (cell.isEmpty() || cell.getItem() == null) {
                    return;
                }

                if (disclosureNode != null && disclosureNode.getBoundsInParent().contains(event.getX(), event.getY())) {
                    return;
                }

                if (!cell.getItem().directory()) {
                    addSelection(cell.getItem());
                }
            });

            return cell;
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
    private void onSelectExtractionFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select extraction folder");

        File selected = chooser.showDialog(windowOf(extractionFolderField));
        if (selected == null) {
            return;
        }

        try (Stream<Path> entries = Files.list(selected.toPath())) {
            if (entries.findAny().isPresent()) {
                Popups.showError("Folder not empty", "Selected directory is not empty");
                return;
            }
        } catch (IOException e) {
            Popups.showError("Failed to read folder", e.getMessage());
            return;
        }

        extractionFolderField.setText(selected.getAbsolutePath());
    }

    @FXML
    private void onOpen() {
        String text = masterFileField.getText();
        if (text == null || text.isBlank()) {
            return;
        }

        Path selected = Path.of(text);
        if (!Files.isRegularFile(selected)) {
            Popups.showError("File not found", selected + " does not exist.");
            return;
        }

        ArchiveNode rootNode = new ArchiveNode(selected.getFileName().toString(), List.of(), true, false);
        ArchiveTreeItem root = new ArchiveTreeItem(rootNode, selected);
        try {
            root.getChildren();
        } catch (UncheckedIOException e) {
            Popups.showError("Failed to open file", e.getCause().getMessage());
            return;
        }

        masterFile = selected;
        selectedFiles.clear();
        root.setExpanded(true);
        archiveTree.setRoot(root);
    }

    @FXML
    private void onExtract() {
        Path destinationFolder = Path.of(extractionFolderField.getText());
        List<ManifestEntry> entries = List.copyOf(selectedFiles);

        statusLabel.setText("Extracting...");
        progressBar.setProgress(0);
        progressBar.setVisible(true);
        progressBar.setManaged(true);
        extracting.set(true);

        Task<DeploymentResult> task = new Task<>() {
            @Override
            protected DeploymentResult call() {
                return patchBuilderService.extract(masterFile, entries, destinationFolder, (completed, total) -> {
                    updateProgress(completed, total);
                    updateMessage("Extracting " + completed + " / " + total + " file(s)...");
                });
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> finishExtraction(task.getValue()));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            finishExtraction(new DeploymentResult(false, "Unexpected error: " + (ex != null ? ex.getMessage() : "unknown")));
        });

        Thread thread = new Thread(task, "extract-worker");
        thread.setDaemon(true);
        thread.start();
    }

    private void finishExtraction(DeploymentResult result) {
        progressBar.progressProperty().unbind();
        statusLabel.textProperty().unbind();
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        statusLabel.setText("");
        extracting.set(false);

        Popups.showResult("Extraction succeed", "Extraction failed", result.success(), result.message());

        if (result.success()) {
            selectedFiles.clear();
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
