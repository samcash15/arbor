package com.arbor.view;

import com.arbor.model.OutlineItem;
import com.arbor.service.OutlineService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.util.*;

public class OutlinePanel extends VBox {

    private final OutlineService outlineService;
    private final TreeView<OutlineItem> treeView;
    private Timer debounceTimer;
    private javafx.beans.value.ChangeListener<String> textListener;
    private EditorTab boundTab;

    public OutlinePanel(OutlineService outlineService) {
        this.outlineService = outlineService;
        getStyleClass().add("outline-panel");
        setPrefWidth(200);
        setMinWidth(120);

        // Header
        Label header = new Label("Outline");
        header.getStyleClass().add("outline-panel-header");
        HBox headerBox = new HBox(header);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(8, 12, 8, 12));
        headerBox.getStyleClass().add("outline-panel-header-box");

        // Tree view
        treeView = new TreeView<>();
        treeView.setShowRoot(false);
        treeView.getStyleClass().add("outline-tree");
        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(OutlineItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.label());
                }
            }
        });

        // Click to navigate
        treeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                TreeItem<OutlineItem> selected = treeView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getValue() != null && boundTab != null) {
                    navigateToLine(selected.getValue().lineNumber());
                }
            }
        });

        VBox.setVgrow(treeView, Priority.ALWAYS);
        getChildren().addAll(headerBox, treeView);
    }

    public void bindToTab(EditorTab tab) {
        unbind();
        this.boundTab = tab;

        // Initial build
        refreshOutline(tab);

        // Listen for text changes with debounce
        textListener = (obs, oldText, newText) -> scheduleRefresh(tab);
        tab.getTextArea().textProperty().addListener(textListener);
    }

    public void unbind() {
        if (boundTab != null && textListener != null) {
            boundTab.getTextArea().textProperty().removeListener(textListener);
        }
        boundTab = null;
        textListener = null;
        cancelTimer();
    }

    public void clear() {
        unbind();
        treeView.setRoot(null);
    }

    private void scheduleRefresh(EditorTab tab) {
        cancelTimer();
        debounceTimer = new Timer(true);
        debounceTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> refreshOutline(tab));
            }
        }, 400);
    }

    private void cancelTimer() {
        if (debounceTimer != null) {
            debounceTimer.cancel();
            debounceTimer = null;
        }
    }

    private void refreshOutline(EditorTab tab) {
        String text = tab.getTextArea().getText();
        String language = tab.getLanguage();
        boolean markdown = tab.isMarkdown();

        List<OutlineItem> items = outlineService.buildOutline(text, language, markdown);
        TreeItem<OutlineItem> root = buildTree(items);
        treeView.setRoot(root);
        expandAll(root);
    }

    private TreeItem<OutlineItem> buildTree(List<OutlineItem> items) {
        TreeItem<OutlineItem> root = new TreeItem<>();
        Deque<TreeItem<OutlineItem>> stack = new ArrayDeque<>();
        // Stack tracks: item + its level. Root is level 0.
        stack.push(root);
        int rootLevel = 0;

        for (OutlineItem item : items) {
            TreeItem<OutlineItem> treeItem = new TreeItem<>(item);

            // Pop until we find a parent with a lower level
            while (stack.size() > 1) {
                OutlineItem parentItem = stack.peek().getValue();
                if (parentItem != null && parentItem.level() >= item.level()) {
                    stack.pop();
                } else {
                    break;
                }
            }

            stack.peek().getChildren().add(treeItem);
            stack.push(treeItem);
        }

        return root;
    }

    private void expandAll(TreeItem<OutlineItem> item) {
        if (item == null) return;
        item.setExpanded(true);
        for (TreeItem<OutlineItem> child : item.getChildren()) {
            expandAll(child);
        }
    }

    private void navigateToLine(int lineNumber) {
        if (boundTab == null) return;
        StyleClassedTextArea textArea = boundTab.getTextArea();
        if (lineNumber >= 0 && lineNumber < textArea.getParagraphs().size()) {
            textArea.moveTo(lineNumber, 0);
            textArea.requestFollowCaret();
            textArea.requestFocus();
        }
    }
}
