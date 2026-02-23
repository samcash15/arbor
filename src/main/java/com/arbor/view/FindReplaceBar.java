package com.arbor.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FindReplaceBar extends VBox {
    private final StyleClassedTextArea textArea;
    private final TextField findField;
    private final TextField replaceField;
    private final Label matchCountLabel;
    private final CheckBox caseSensitiveBox;
    private final HBox replaceRow;

    private final List<int[]> matches = new ArrayList<>();
    private int currentMatchIndex = -1;
    private boolean replaceVisible = false;

    public FindReplaceBar(StyleClassedTextArea textArea) {
        this.textArea = textArea;
        getStyleClass().add("find-replace-bar");
        setSpacing(4);
        setPadding(new Insets(6, 12, 6, 12));

        // Find row
        findField = new TextField();
        findField.setPromptText("Find...");
        findField.getStyleClass().add("find-field");
        HBox.setHgrow(findField, Priority.ALWAYS);

        caseSensitiveBox = new CheckBox("Aa");
        caseSensitiveBox.getStyleClass().add("find-option");
        caseSensitiveBox.setOnAction(e -> performSearch());

        matchCountLabel = new Label("");
        matchCountLabel.getStyleClass().add("match-count");
        matchCountLabel.setMinWidth(70);

        Button prevBtn = new Button("\u25B2");
        prevBtn.getStyleClass().add("find-nav-button");
        prevBtn.setOnAction(e -> navigateMatch(-1));

        Button nextBtn = new Button("\u25BC");
        nextBtn.getStyleClass().add("find-nav-button");
        nextBtn.setOnAction(e -> navigateMatch(1));

        // Replace row (initialized before toggle button so it can be referenced)
        replaceField = new TextField();
        replaceField.setPromptText("Replace...");
        replaceField.getStyleClass().add("find-field");
        HBox.setHgrow(replaceField, Priority.ALWAYS);

        Button replaceBtn = new Button("Replace");
        replaceBtn.getStyleClass().add("find-action-button");
        replaceBtn.setOnAction(e -> replaceCurrent());

        Button replaceAllBtn = new Button("All");
        replaceAllBtn.getStyleClass().add("find-action-button");
        replaceAllBtn.setOnAction(e -> replaceAll());

        replaceRow = new HBox(4, replaceField, replaceBtn, replaceAllBtn);
        replaceRow.setAlignment(Pos.CENTER_LEFT);
        replaceRow.setPadding(new Insets(0, 0, 0, 28));
        replaceRow.setVisible(false);
        replaceRow.setManaged(false);

        Button toggleReplaceBtn = new Button("\u25BA");
        toggleReplaceBtn.getStyleClass().add("find-nav-button");
        toggleReplaceBtn.setOnAction(e -> {
            replaceVisible = !replaceVisible;
            replaceRow.setVisible(replaceVisible);
            replaceRow.setManaged(replaceVisible);
            toggleReplaceBtn.setText(replaceVisible ? "\u25BC" : "\u25BA");
        });

        Button closeBtn = new Button("\u2715");
        closeBtn.getStyleClass().add("find-nav-button");
        closeBtn.setOnAction(e -> close());

        HBox findRow = new HBox(4, toggleReplaceBtn, findField, caseSensitiveBox, matchCountLabel, prevBtn, nextBtn, closeBtn);
        findRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(findRow, replaceRow);

        // Search on typing
        findField.textProperty().addListener((obs, oldVal, newVal) -> performSearch());

        // Enter navigates to next match
        findField.setOnAction(e -> navigateMatch(1));
        replaceField.setOnAction(e -> replaceCurrent());
    }

    public void show(boolean withReplace) {
        setVisible(true);
        setManaged(true);
        replaceVisible = withReplace;
        replaceRow.setVisible(withReplace);
        replaceRow.setManaged(withReplace);

        // Pre-fill with selected text
        String selected = textArea.getSelectedText();
        if (selected != null && !selected.isEmpty() && !selected.contains("\n")) {
            findField.setText(selected);
        }
        findField.requestFocus();
        findField.selectAll();
    }

    public void close() {
        setVisible(false);
        setManaged(false);
        clearHighlights();
        textArea.requestFocus();
    }

    private void performSearch() {
        clearHighlights();
        matches.clear();
        currentMatchIndex = -1;

        String query = findField.getText();
        if (query == null || query.isEmpty()) {
            matchCountLabel.setText("");
            return;
        }

        String text = textArea.getText();
        boolean caseSensitive = caseSensitiveBox.isSelected();

        int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
        Pattern pattern = Pattern.compile(Pattern.quote(query), flags);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            matches.add(new int[]{matcher.start(), matcher.end()});
        }

        matchCountLabel.setText(matches.isEmpty() ? "No results" : matches.size() + " found");

        // Highlight all matches
        for (int[] match : matches) {
            textArea.setStyleClass(match[0], match[1], "find-highlight");
        }

        // Navigate to first match near caret
        if (!matches.isEmpty()) {
            int caretPos = textArea.getCaretPosition();
            currentMatchIndex = 0;
            for (int i = 0; i < matches.size(); i++) {
                if (matches.get(i)[0] >= caretPos) {
                    currentMatchIndex = i;
                    break;
                }
            }
            highlightCurrentMatch();
        }
    }

    private void navigateMatch(int direction) {
        if (matches.isEmpty()) return;
        currentMatchIndex = (currentMatchIndex + direction + matches.size()) % matches.size();
        highlightCurrentMatch();
        matchCountLabel.setText((currentMatchIndex + 1) + " of " + matches.size());
    }

    private void highlightCurrentMatch() {
        if (currentMatchIndex < 0 || currentMatchIndex >= matches.size()) return;
        int[] match = matches.get(currentMatchIndex);
        textArea.selectRange(match[0], match[1]);
        textArea.requestFollowCaret();
        matchCountLabel.setText((currentMatchIndex + 1) + " of " + matches.size());
    }

    private void replaceCurrent() {
        if (currentMatchIndex < 0 || currentMatchIndex >= matches.size()) return;
        int[] match = matches.get(currentMatchIndex);
        String replacement = replaceField.getText();
        textArea.replaceText(match[0], match[1], replacement);
        performSearch();
    }

    private void replaceAll() {
        String query = findField.getText();
        String replacement = replaceField.getText();
        if (query == null || query.isEmpty()) return;

        boolean caseSensitive = caseSensitiveBox.isSelected();
        int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
        String newText = Pattern.compile(Pattern.quote(query), flags)
                .matcher(textArea.getText())
                .replaceAll(Matcher.quoteReplacement(replacement));
        textArea.replaceText(newText);
        performSearch();
    }

    private void clearHighlights() {
        // Reset all styling — this will clear find highlights
        // Syntax highlighting will re-apply via the listener
        if (!textArea.getText().isEmpty()) {
            textArea.setStyleClass(0, textArea.getLength(), "");
        }
    }
}
