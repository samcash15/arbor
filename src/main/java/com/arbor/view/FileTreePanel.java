package com.arbor.view;

import com.arbor.service.FileOperationService;
import com.arbor.service.FileTreeService;
import com.arbor.util.DialogHelper;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

public class FileTreePanel extends VBox {
    private final TreeView<Path> treeView;
    private final FileTreeService treeService;
    private final FileOperationService fileOps;
    private TreeItem<Path> rootItem;

    public FileTreePanel(FileTreeService treeService, FileOperationService fileOps, Consumer<Path> onFileOpen) {
        this.treeService = treeService;
        this.fileOps = fileOps;

        getStyleClass().add("file-tree-panel");

        // Header
        Label header = new Label("Projects");
        header.getStyleClass().add("tree-panel-header");
        header.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(header, Priority.ALWAYS);

        HBox headerBox = new HBox(header);
        headerBox.getStyleClass().add("tree-panel-header-box");
        headerBox.setPadding(new Insets(8, 12, 8, 12));

        // Tree view
        treeView = new TreeView<>();
        treeView.setShowRoot(true);
        treeView.setCellFactory(tv -> new PathTreeCell(fileOps, treeService, onFileOpen));
        VBox.setVgrow(treeView, Priority.ALWAYS);

        // Separator line + New Folder link
        Separator separator = new Separator();
        separator.getStyleClass().add("tree-panel-separator");

        Label newFolderLink = new Label("+ New Folder");
        newFolderLink.getStyleClass().add("new-folder-link");
        newFolderLink.setPadding(new Insets(6, 12, 8, 12));
        newFolderLink.setOnMouseClicked(e -> {
            if (rootItem != null) {
                Optional<String> name = DialogHelper.showTextInput("New Folder", "Folder name:", "New Folder");
                name.ifPresent(n -> {
                    try {
                        fileOps.createDirectory(rootItem.getValue(), n);
                        treeService.populateChildren(rootItem);
                    } catch (IOException ex) {
                        DialogHelper.showError("Error", "Could not create folder: " + ex.getMessage());
                    }
                });
            }
        });

        getChildren().addAll(headerBox, treeView, separator, newFolderLink);
        setMinWidth(200);
        setPrefWidth(250);
    }

    public void loadGrove(Path rootPath) {
        rootItem = treeService.buildTree(rootPath);
        treeView.setRoot(rootItem);
    }

    public TreeView<Path> getTreeView() {
        return treeView;
    }

    public void refresh() {
        if (rootItem != null) {
            treeService.populateChildren(rootItem);
        }
    }
}
