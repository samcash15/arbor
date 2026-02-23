package com.arbor.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.fxmisc.richtext.StyleClassedTextArea;

public class StatusBar extends HBox {
    private final Label cursorPositionLabel;
    private final Label fileTypeLabel;
    private final Label wordCountLabel;

    public StatusBar() {
        getStyleClass().add("status-bar");
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(3, 12, 3, 12));
        setSpacing(16);

        cursorPositionLabel = new Label("Ln 1, Col 1");
        cursorPositionLabel.getStyleClass().add("status-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        wordCountLabel = new Label("");
        wordCountLabel.getStyleClass().add("status-label");

        fileTypeLabel = new Label("");
        fileTypeLabel.getStyleClass().add("status-label");

        getChildren().addAll(cursorPositionLabel, spacer, wordCountLabel, fileTypeLabel);
    }

    public void bindToTabPane(TabPane tabPane) {
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            updateForTab(newTab);
        });
    }

    private void updateForTab(Tab tab) {
        if (tab instanceof EditorTab editorTab) {
            StyleClassedTextArea textArea = editorTab.getTextArea();
            String fileName = editorTab.getFilePath().getFileName().toString();

            // File type
            int dot = fileName.lastIndexOf('.');
            String ext = dot >= 0 ? fileName.substring(dot + 1).toUpperCase() : "TEXT";
            fileTypeLabel.setText(ext);

            // Update cursor position on caret changes
            textArea.caretPositionProperty().addListener((o, oldPos, newPos) -> {
                updateCursorPosition(textArea);
                updateWordCount(textArea, fileName);
            });

            // Initial update
            updateCursorPosition(textArea);
            updateWordCount(textArea, fileName);
        } else {
            cursorPositionLabel.setText("");
            fileTypeLabel.setText("");
            wordCountLabel.setText("");
        }
    }

    private void updateCursorPosition(StyleClassedTextArea textArea) {
        int caretPos = textArea.getCaretPosition();
        int paragraph = textArea.getCurrentParagraph() + 1;
        int column = textArea.getCaretColumn() + 1;
        cursorPositionLabel.setText("Ln " + paragraph + ", Col " + column);
    }

    private void updateWordCount(StyleClassedTextArea textArea, String fileName) {
        if (fileName.toLowerCase().endsWith(".md") || fileName.toLowerCase().endsWith(".txt")) {
            String text = textArea.getText();
            if (text.isBlank()) {
                wordCountLabel.setText("0 words");
            } else {
                int words = text.trim().split("\\s+").length;
                wordCountLabel.setText(words + " words");
            }
        } else {
            int lines = textArea.getParagraphs().size();
            wordCountLabel.setText(lines + " lines");
        }
    }
}
