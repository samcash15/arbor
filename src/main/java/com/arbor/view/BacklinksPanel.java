package com.arbor.view;

import com.arbor.model.BacklinkEntry;
import com.arbor.service.BacklinkService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class BacklinksPanel extends VBox {

    private final BacklinkService backlinkService;
    private final Label headerLabel;
    private final ListView<BacklinkEntry> listView;
    private boolean collapsed = false;
    private Consumer<Path> onFileOpen;

    public BacklinksPanel(BacklinkService backlinkService) {
        this.backlinkService = backlinkService;
        getStyleClass().add("backlinks-panel");
        setMaxHeight(150);

        // Header
        headerLabel = new Label("Backlinks (0)");
        headerLabel.getStyleClass().add("backlinks-header");
        headerLabel.setOnMouseClicked(e -> toggleCollapse());
        headerLabel.setCursor(javafx.scene.Cursor.HAND);

        HBox headerBox = new HBox(headerLabel);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(4, 12, 4, 12));

        // List view
        listView = new ListView<>();
        listView.getStyleClass().add("backlinks-list");
        listView.setPrefHeight(120);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(BacklinkEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.sourcePath().getFileName().toString() + " (line " + (item.lineNumber() + 1) + ")");
                }
            }
        });

        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && onFileOpen != null) {
                BacklinkEntry selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    onFileOpen.accept(selected.sourcePath());
                }
            }
        });

        VBox.setVgrow(listView, Priority.ALWAYS);
        getChildren().addAll(headerBox, listView);
    }

    public void setOnFileOpen(Consumer<Path> onFileOpen) {
        this.onFileOpen = onFileOpen;
    }

    public void updateForFile(Path filePath) {
        Thread.startVirtualThread(() -> {
            List<BacklinkEntry> backlinks = backlinkService.getBacklinksTo(filePath);
            Platform.runLater(() -> {
                headerLabel.setText("Backlinks (" + backlinks.size() + ")");
                listView.getItems().setAll(backlinks);
                setVisible(!backlinks.isEmpty());
                setManaged(!backlinks.isEmpty());
            });
        });
    }

    private void toggleCollapse() {
        collapsed = !collapsed;
        listView.setVisible(!collapsed);
        listView.setManaged(!collapsed);
    }
}
