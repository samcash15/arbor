package com.arbor.view;

import com.arbor.service.FileOperationService;
import com.arbor.service.FileTreeService;
import com.arbor.util.DialogHelper;
import com.arbor.util.IconFactory;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

public class PathTreeCell extends TreeCell<Path> {
    private final FileOperationService fileOps;
    private final FileTreeService treeService;
    private final Consumer<Path> onFileOpen;

    public PathTreeCell(FileOperationService fileOps, FileTreeService treeService, Consumer<Path> onFileOpen) {
        this.fileOps = fileOps;
        this.treeService = treeService;
        this.onFileOpen = onFileOpen;
    }

    @Override
    protected void updateItem(Path path, boolean empty) {
        super.updateItem(path, empty);
        if (empty || path == null) {
            setText(null);
            setGraphic(null);
            setContextMenu(null);
            return;
        }

        String fileName = path.getFileName().toString();
        boolean isRoot = getTreeItem() != null && getTreeItem().getParent() == null;

        if (isRoot) {
            setText(null);
            Label nameLabel = new Label(fileName);
            nameLabel.setStyle("-fx-padding: 0;");
            nameLabel.textFillProperty().bind(textFillProperty());

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label addBtn = new Label("+");
            addBtn.getStyleClass().add("tree-panel-add-button");
            addBtn.setOnMouseClicked(e -> {
                e.consume();
                ContextMenu addMenu = new ContextMenu();
                MenuItem newFile = new MenuItem("New File");
                newFile.setOnAction(ev -> {
                    Optional<String> name = DialogHelper.showTextInput("New File", "File name:", "untitled.md");
                    name.ifPresent(n -> {
                        try {
                            fileOps.createFile(path, n);
                            treeService.populateChildren(getTreeItem());
                            getTreeItem().setExpanded(true);
                        } catch (IOException ex) {
                            DialogHelper.showError("Error", "Could not create file: " + ex.getMessage());
                        }
                    });
                });
                MenuItem newFolder = new MenuItem("New Folder");
                newFolder.setOnAction(ev -> {
                    Optional<String> name = DialogHelper.showTextInput("New Folder", "Folder name:", "New Folder");
                    name.ifPresent(n -> {
                        try {
                            fileOps.createDirectory(path, n);
                            treeService.populateChildren(getTreeItem());
                            getTreeItem().setExpanded(true);
                        } catch (IOException ex) {
                            DialogHelper.showError("Error", "Could not create folder: " + ex.getMessage());
                        }
                    });
                });
                addMenu.getItems().addAll(newFile, newFolder);
                addMenu.show(addBtn, Side.BOTTOM, 0, 0);
            });

            HBox rootGraphic = new HBox(4, IconFactory.folderIcon(), nameLabel, spacer, addBtn);
            rootGraphic.setAlignment(Pos.CENTER_LEFT);
            rootGraphic.setMaxWidth(Double.MAX_VALUE);
            setGraphic(rootGraphic);
        } else {
            setText(fileName);
            if (Files.isDirectory(path)) {
                setGraphic(IconFactory.folderIcon());
            } else {
                int dot = fileName.lastIndexOf('.');
                String ext = dot >= 0 ? fileName.substring(dot + 1) : "";
                setGraphic(IconFactory.fileIcon(ext));
            }
        }
        setContextMenu(createContextMenu(path));

        setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !Files.isDirectory(path)) {
                onFileOpen.accept(path);
            }
        });
    }

    private ContextMenu createContextMenu(Path path) {
        ContextMenu menu = new ContextMenu();

        if (Files.isDirectory(path)) {
            MenuItem newFile = new MenuItem("New File");
            newFile.setOnAction(e -> {
                Optional<String> name = DialogHelper.showTextInput("New File", "File name:", "untitled.md");
                name.ifPresent(n -> {
                    try {
                        fileOps.createFile(path, n);
                        TreeItem<Path> item = getTreeItem();
                        if (item != null) {
                            treeService.populateChildren(item);
                            item.setExpanded(true);
                        }
                    } catch (IOException ex) {
                        DialogHelper.showError("Error", "Could not create file: " + ex.getMessage());
                    }
                });
            });

            MenuItem newFolder = new MenuItem("New Folder");
            newFolder.setOnAction(e -> {
                Optional<String> name = DialogHelper.showTextInput("New Folder", "Folder name:", "New Folder");
                name.ifPresent(n -> {
                    try {
                        fileOps.createDirectory(path, n);
                        TreeItem<Path> item = getTreeItem();
                        if (item != null) {
                            treeService.populateChildren(item);
                            item.setExpanded(true);
                        }
                    } catch (IOException ex) {
                        DialogHelper.showError("Error", "Could not create folder: " + ex.getMessage());
                    }
                });
            });

            menu.getItems().addAll(newFile, newFolder, new SeparatorMenuItem());
        }

        MenuItem rename = new MenuItem("Rename");
        rename.setOnAction(e -> {
            Optional<String> name = DialogHelper.showTextInput("Rename", "New name:", path.getFileName().toString());
            name.ifPresent(n -> {
                try {
                    fileOps.rename(path, n);
                    TreeItem<Path> item = getTreeItem();
                    if (item != null && item.getParent() != null) {
                        treeService.populateChildren(item.getParent());
                    }
                } catch (IOException ex) {
                    DialogHelper.showError("Error", "Could not rename: " + ex.getMessage());
                }
            });
        });

        MenuItem delete = new MenuItem("Delete");
        delete.setOnAction(e -> {
            boolean confirmed = DialogHelper.showConfirmation("Delete",
                    "Are you sure you want to delete \"" + path.getFileName() + "\"?");
            if (confirmed) {
                try {
                    fileOps.delete(path);
                    TreeItem<Path> item = getTreeItem();
                    if (item != null && item.getParent() != null) {
                        treeService.populateChildren(item.getParent());
                    }
                } catch (IOException ex) {
                    DialogHelper.showError("Error", "Could not delete: " + ex.getMessage());
                }
            }
        });

        menu.getItems().addAll(rename, delete);
        return menu;
    }
}
