package com.arbor.view;

import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.layout.StackPane;

public class SplitEditorPane extends StackPane {

    private final DraggableTabPane primaryPane;
    private DraggableTabPane secondaryPane;
    private SplitPane internalSplit;
    private boolean isSplit = false;
    private DraggableTabPane activePane;

    public SplitEditorPane(DraggableTabPane primaryPane) {
        this.primaryPane = primaryPane;
        this.activePane = primaryPane;
        getChildren().add(primaryPane);

        // Track focus on primary
        primaryPane.getSelectionModel().selectedItemProperty().addListener((obs, old, newTab) -> {
            if (newTab != null) activePane = primaryPane;
        });
        primaryPane.focusedProperty().addListener((obs, old, focused) -> {
            if (focused) activePane = primaryPane;
        });
    }

    public void splitRight(Tab tab) {
        if (isSplit) {
            // Already split, move tab to secondary
            if (primaryPane.getTabs().contains(tab)) {
                primaryPane.getTabs().remove(tab);
                secondaryPane.getTabs().add(tab);
                secondaryPane.getSelectionModel().select(tab);
            }
            return;
        }

        // Create secondary pane
        secondaryPane = new DraggableTabPane();
        secondaryPane.focusedProperty().addListener((obs, old, focused) -> {
            if (focused) activePane = secondaryPane;
        });
        secondaryPane.getSelectionModel().selectedItemProperty().addListener((obs, old, newTab) -> {
            if (newTab != null) activePane = secondaryPane;
        });

        // Move tab from primary to secondary
        primaryPane.getTabs().remove(tab);
        secondaryPane.getTabs().add(tab);
        secondaryPane.getSelectionModel().select(tab);

        // Auto-collapse when secondary empties
        secondaryPane.getTabs().addListener((ListChangeListener<Tab>) change -> {
            while (change.next()) {
                if (secondaryPane.getTabs().isEmpty()) {
                    javafx.application.Platform.runLater(this::collapseSplit);
                }
            }
        });

        // Create internal split
        internalSplit = new SplitPane(primaryPane, secondaryPane);
        internalSplit.setDividerPositions(0.5);

        getChildren().clear();
        getChildren().add(internalSplit);
        isSplit = true;

        // Install drag handlers on the new pane after it's shown
        javafx.application.Platform.runLater(() -> secondaryPane.installTabDragHandlers());
    }

    public void collapseSplit() {
        if (!isSplit) return;

        // Move any remaining tabs from secondary to primary
        if (secondaryPane != null) {
            for (Tab tab : new java.util.ArrayList<>(secondaryPane.getTabs())) {
                secondaryPane.getTabs().remove(tab);
                primaryPane.getTabs().add(tab);
            }
        }

        getChildren().clear();
        getChildren().add(primaryPane);
        secondaryPane = null;
        internalSplit = null;
        isSplit = false;
        activePane = primaryPane;
    }

    public boolean isSplit() {
        return isSplit;
    }

    public DraggableTabPane getPrimaryPane() {
        return primaryPane;
    }

    public DraggableTabPane getSecondaryPane() {
        return secondaryPane;
    }

    public DraggableTabPane getActivePane() {
        return activePane;
    }

    public DraggableTabPane findPaneContaining(Tab tab) {
        if (primaryPane.getTabs().contains(tab)) return primaryPane;
        if (isSplit && secondaryPane != null && secondaryPane.getTabs().contains(tab)) return secondaryPane;
        return null;
    }
}
