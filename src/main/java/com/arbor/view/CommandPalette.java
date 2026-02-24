package com.arbor.view;

import com.arbor.model.CommandEntry;
import com.arbor.service.CommandRegistry;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class CommandPalette extends VBox {
    private final CommandRegistry registry;
    private final TextField searchField;
    private final ListView<CommandEntry> commandList;
    private Runnable onHide;

    public CommandPalette(CommandRegistry registry) {
        this.registry = registry;

        getStyleClass().add("command-palette");
        setVisible(false);
        setManaged(false);
        setMaxWidth(450);
        setMaxHeight(350);
        setMinWidth(450);

        // Search field
        searchField = new TextField();
        searchField.setPromptText("Type a command...");
        searchField.getStyleClass().add("command-palette-field");

        // Command list
        commandList = new ListView<>();
        commandList.getStyleClass().add("command-palette-list");
        commandList.setCellFactory(lv -> new CommandCell());
        VBox.setVgrow(commandList, Priority.ALWAYS);

        getChildren().addAll(searchField, commandList);
        setPadding(Insets.EMPTY);

        // Filter on text change
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            commandList.getItems().setAll(registry.search(newVal));
            if (!commandList.getItems().isEmpty()) {
                commandList.getSelectionModel().selectFirst();
            }
        });

        // Key handling on search field
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                hide();
                event.consume();
            } else if (event.getCode() == KeyCode.DOWN) {
                if (!commandList.getItems().isEmpty()) {
                    commandList.requestFocus();
                    if (commandList.getSelectionModel().getSelectedIndex() < 0) {
                        commandList.getSelectionModel().selectFirst();
                    }
                }
                event.consume();
            } else if (event.getCode() == KeyCode.UP) {
                if (!commandList.getItems().isEmpty()) {
                    commandList.requestFocus();
                    commandList.getSelectionModel().selectLast();
                }
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER) {
                executeSelected();
                event.consume();
            }
        });

        // Key handling on command list
        commandList.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                hide();
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER) {
                executeSelected();
                event.consume();
            }
        });

        // Click to execute
        commandList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                executeSelected();
            }
        });
    }

    private void executeSelected() {
        CommandEntry selected = commandList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            hide();
            selected.action().run();
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
        searchField.clear();
        commandList.getItems().setAll(registry.getAll());
        if (!commandList.getItems().isEmpty()) {
            commandList.getSelectionModel().selectFirst();
        }
        searchField.requestFocus();
    }

    public void hide() {
        setVisible(false);
        setManaged(false);
        if (onHide != null) {
            onHide.run();
        }
    }

    public void setOnHide(Runnable onHide) {
        this.onHide = onHide;
    }

    private static class CommandCell extends ListCell<CommandEntry> {
        private final HBox container;
        private final Label categoryLabel;
        private final Label nameLabel;
        private final Label shortcutLabel;

        CommandCell() {
            container = new HBox(8);
            container.setAlignment(Pos.CENTER_LEFT);

            categoryLabel = new Label();
            categoryLabel.getStyleClass().add("command-category-chip");

            nameLabel = new Label();

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            shortcutLabel = new Label();
            shortcutLabel.getStyleClass().add("command-shortcut");

            container.getChildren().addAll(categoryLabel, nameLabel, spacer, shortcutLabel);
        }

        @Override
        protected void updateItem(CommandEntry item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                categoryLabel.setText(item.category());
                nameLabel.setText(item.name());
                shortcutLabel.setText(item.shortcut() != null ? item.shortcut() : "");
                shortcutLabel.setVisible(item.shortcut() != null);
                setGraphic(container);
                setText(null);
            }
        }
    }
}
