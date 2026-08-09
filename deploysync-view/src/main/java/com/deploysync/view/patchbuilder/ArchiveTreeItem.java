package com.deploysync.view.patchbuilder;

import com.deploysync.model.patchbuilder.ArchiveNode;
import com.deploysync.model.patchbuilder.NestedArchiveExplorer;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;

public class ArchiveTreeItem extends TreeItem<ArchiveNode> {
    private final Path masterFile;
    private boolean childrenLoaded = false;

    public ArchiveTreeItem(ArchiveNode node, Path masterFile) {
        super(node);
        this.masterFile = masterFile;
    }

    @Override
    public boolean isLeaf() {
        ArchiveNode node = getValue();
        return !node.directory() && !node.archive();
    }

    @Override
    public ObservableList<TreeItem<ArchiveNode>> getChildren() {
        if (!childrenLoaded) {
            childrenLoaded = true;
            super.getChildren().setAll(loadChildren());
        }
        return super.getChildren();
    }

    private List<ArchiveTreeItem> loadChildren() {
        try {
            return NestedArchiveExplorer.listChildren(masterFile, getValue().segments()).stream()
                    .map(child -> new ArchiveTreeItem(child, masterFile))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
