package com.arbor.service;

import javafx.scene.control.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FileTreeService {
    private static final Logger log = LoggerFactory.getLogger(FileTreeService.class);
    private static final String ARBOR_DIR = ".arbor";

    public TreeItem<Path> buildTree(Path rootPath) {
        TreeItem<Path> root = new TreeItem<>(rootPath);
        root.setExpanded(true);
        populateChildren(root);
        return root;
    }

    public void populateChildren(TreeItem<Path> parent) {
        parent.getChildren().clear();
        Path dir = parent.getValue();
        if (!Files.isDirectory(dir)) {
            return;
        }

        List<TreeItem<Path>> dirs = new ArrayList<>();
        List<TreeItem<Path>> files = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                if (name.equals(ARBOR_DIR) || name.startsWith(".")) {
                    continue;
                }
                TreeItem<Path> item = new TreeItem<>(entry);
                if (Files.isDirectory(entry)) {
                    // Add a dummy child so the expand arrow appears
                    item.getChildren().add(new TreeItem<>());
                    item.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
                        if (isExpanded && item.getChildren().size() == 1
                                && item.getChildren().getFirst().getValue() == null) {
                            populateChildren(item);
                        }
                    });
                    dirs.add(item);
                } else {
                    files.add(item);
                }
            }
        } catch (IOException e) {
            log.error("Failed to list directory: {}", dir, e);
        }

        dirs.sort(Comparator.comparing(i -> i.getValue().getFileName().toString().toLowerCase()));
        files.sort(Comparator.comparing(i -> i.getValue().getFileName().toString().toLowerCase()));

        parent.getChildren().addAll(dirs);
        parent.getChildren().addAll(files);
    }
}
