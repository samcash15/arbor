package com.arbor.view;

import com.arbor.service.SearchService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class SearchBar extends VBox {
    private final SearchService searchService;
    private final TextField searchField;
    private final ToggleButton fileToggle;
    private final ToggleButton contentToggle;
    private final ListView<Path> resultsList;

    private Path rootPath;
    private Consumer<Path> onFileOpen;

    public SearchBar(SearchService searchService) {
        this.searchService = searchService;

        getStyleClass().add("search-bar");
        setVisible(false);
        setManaged(false);

        // Search field
        searchField = new TextField();
        searchField.setPromptText("Search files...");
        searchField.getStyleClass().add("search-bar-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        // File / Content toggle buttons
        ToggleGroup modeGroup = new ToggleGroup();
        fileToggle = new ToggleButton("File");
        fileToggle.setToggleGroup(modeGroup);
        fileToggle.setSelected(true);
        fileToggle.getStyleClass().add("search-bar-toggle");

        contentToggle = new ToggleButton("Content");
        contentToggle.setToggleGroup(modeGroup);
        contentToggle.getStyleClass().add("search-bar-toggle");

        // Prevent deselecting both toggles
        modeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true);
            }
        });

        // Close button
        Label closeBtn = new Label("\u2715");
        closeBtn.getStyleClass().add("search-bar-close");
        closeBtn.setOnMouseClicked(e -> hide());

        // Search row
        HBox searchRow = new HBox(8, searchField, fileToggle, contentToggle, closeBtn);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.setPadding(new Insets(8, 12, 4, 12));

        // Results dropdown
        resultsList = new ListView<>();
        resultsList.getStyleClass().add("search-bar-results");
        resultsList.setMaxHeight(200);
        resultsList.setVisible(false);
        resultsList.setManaged(false);
        resultsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Path item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || rootPath == null) {
                    setText(null);
                } else {
                    setText(rootPath.relativize(item).toString());
                }
            }
        });

        getChildren().addAll(searchRow, resultsList);

        // Search on text change
        searchField.textProperty().addListener((obs, oldVal, newVal) -> performSearch(newVal));

        // Re-search when toggle changes
        modeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> performSearch(searchField.getText()));

        // Open file on click
        resultsList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                openSelected();
            }
        });

        // Enter on results list opens file
        resultsList.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                openSelected();
            }
        });

        // Escape closes, Down arrow moves to results
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                hide();
            } else if (event.getCode() == KeyCode.DOWN && !resultsList.getItems().isEmpty()) {
                resultsList.requestFocus();
                resultsList.getSelectionModel().selectFirst();
            } else if (event.getCode() == KeyCode.ENTER && !resultsList.getItems().isEmpty()) {
                resultsList.getSelectionModel().selectFirst();
                openSelected();
            }
        });
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty() || rootPath == null) {
            resultsList.getItems().clear();
            resultsList.setVisible(false);
            resultsList.setManaged(false);
            return;
        }

        List<Path> results;
        if (contentToggle.isSelected()) {
            results = searchService.searchByContent(rootPath, query.trim())
                    .stream().map(SearchService.SearchResult::path).toList();
        } else {
            results = searchService.searchByName(rootPath, query.trim());
        }

        resultsList.getItems().setAll(results);
        boolean hasResults = !results.isEmpty();
        resultsList.setVisible(hasResults);
        resultsList.setManaged(hasResults);
    }

    private void openSelected() {
        Path selected = resultsList.getSelectionModel().getSelectedItem();
        if (selected != null && !Files.isDirectory(selected) && onFileOpen != null) {
            onFileOpen.accept(selected);
            hide();
        }
    }

    public void toggle() {
        if (isVisible()) {
            hide();
        } else {
            show();
        }
    }

    public void show() {
        setVisible(true);
        setManaged(true);
        searchField.requestFocus();
        searchField.selectAll();
    }

    public void hide() {
        setVisible(false);
        setManaged(false);
        searchField.clear();
        resultsList.getItems().clear();
        resultsList.setVisible(false);
        resultsList.setManaged(false);
    }

    public void setRootPath(Path rootPath) {
        this.rootPath = rootPath;
    }

    public void setOnFileOpen(Consumer<Path> onFileOpen) {
        this.onFileOpen = onFileOpen;
    }
}
