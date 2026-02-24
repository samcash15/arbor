package com.arbor.view;

import javafx.collections.ListChangeListener;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

public class DraggableTabPane extends TabPane {

    private static final DataFormat TAB_DRAG_FORMAT = new DataFormat("application/x-arbor-tab");
    static Tab draggedTab;
    static DraggableTabPane sourcePane;

    public DraggableTabPane() {
        setTabDragPolicy(TabDragPolicy.REORDER);
        setupDropTarget();

        // Install drag handlers once scene is available
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                // Delay to allow tab header nodes to be created
                javafx.application.Platform.runLater(this::installTabDragHandlers);
            }
        });

        // Re-install when tabs change
        getTabs().addListener((ListChangeListener<Tab>) change -> {
            javafx.application.Platform.runLater(this::installTabDragHandlers);
        });
    }

    void installTabDragHandlers() {
        // Look up all .tab nodes in this tab pane's header
        lookupAll(".tab").forEach(node -> {
            if (node.getOnDragDetected() != null) return; // already installed

            node.setOnDragDetected(event -> {
                // Find which tab this node represents
                int idx = -1;
                var tabNodes = new java.util.ArrayList<>(lookupAll(".tab"));
                for (int i = 0; i < tabNodes.size(); i++) {
                    if (tabNodes.get(i) == node) {
                        idx = i;
                        break;
                    }
                }
                if (idx >= 0 && idx < getTabs().size()) {
                    Tab tab = getTabs().get(idx);
                    if (!tab.isClosable() && !(tab instanceof EditorTab)) return; // don't drag welcome tab

                    draggedTab = tab;
                    sourcePane = DraggableTabPane.this;

                    Dragboard db = node.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.put(TAB_DRAG_FORMAT, "tab");
                    db.setContent(content);
                }
                event.consume();
            });

            node.setOnDragDone(event -> {
                draggedTab = null;
                sourcePane = null;
                event.consume();
            });
        });
    }

    private void setupDropTarget() {
        setOnDragOver(event -> {
            if (draggedTab != null && sourcePane != this && event.getDragboard().hasContent(TAB_DRAG_FORMAT)) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        setOnDragDropped(event -> {
            if (draggedTab != null && sourcePane != null && sourcePane != this) {
                Tab tab = draggedTab;
                sourcePane.getTabs().remove(tab);
                getTabs().add(tab);
                getSelectionModel().select(tab);
                event.setDropCompleted(true);
            } else {
                event.setDropCompleted(false);
            }
            event.consume();
        });
    }
}
