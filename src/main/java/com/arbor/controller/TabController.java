package com.arbor.controller;

import com.arbor.service.FileOperationService;
import com.arbor.util.DialogHelper;
import com.arbor.view.DraggableTabPane;
import com.arbor.view.EditorTab;
import com.arbor.view.SplitEditorPane;
import com.arbor.view.WelcomeView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TabController {
    private final SplitEditorPane splitEditorPane;
    private final FileOperationService fileOps;
    private final Tab welcomeTab;

    public TabController(SplitEditorPane splitEditorPane, FileOperationService fileOps) {
        this.splitEditorPane = splitEditorPane;
        this.fileOps = fileOps;

        this.welcomeTab = new Tab("Welcome");
        welcomeTab.setClosable(false);
        welcomeTab.setContent(new WelcomeView());
        splitEditorPane.getPrimaryPane().getTabs().add(welcomeTab);

        // Keep welcome tab when all tabs are closed
        splitEditorPane.getPrimaryPane().getTabs().addListener((javafx.collections.ListChangeListener<Tab>) change -> {
            while (change.next()) {
                if (splitEditorPane.getPrimaryPane().getTabs().isEmpty()) {
                    splitEditorPane.getPrimaryPane().getTabs().add(welcomeTab);
                }
            }
        });
    }

    public void openFile(Path path) {
        // Check both panes for duplicates
        Tab existing = findTab(path);
        if (existing != null) {
            DraggableTabPane pane = splitEditorPane.findPaneContaining(existing);
            if (pane != null) {
                pane.getSelectionModel().select(existing);
            }
            return;
        }

        // Remove welcome tab if present
        splitEditorPane.getPrimaryPane().getTabs().remove(welcomeTab);

        EditorTab editorTab = new EditorTab(path, fileOps);
        editorTab.setOnCloseRequest(event -> {
            if (editorTab.isDirty()) {
                boolean save = DialogHelper.showConfirmation("Unsaved Changes",
                        "Save changes to \"" + path.getFileName() + "\" before closing?");
                if (save) {
                    editorTab.save();
                }
            }
        });

        // Add to active (focused) pane
        DraggableTabPane activePane = splitEditorPane.getActivePane();
        activePane.getTabs().add(editorTab);
        activePane.getSelectionModel().select(editorTab);
    }

    public void saveCurrentTab() {
        Tab selected = getActiveSelectedTab();
        if (selected instanceof EditorTab editorTab) {
            editorTab.save();
        }
    }

    public void closeCurrentTab() {
        Tab selected = getActiveSelectedTab();
        if (selected != null && selected != welcomeTab) {
            if (selected instanceof EditorTab editorTab && editorTab.isDirty()) {
                boolean save = DialogHelper.showConfirmation("Unsaved Changes",
                        "Save changes before closing?");
                if (save) {
                    editorTab.save();
                }
            }
            DraggableTabPane pane = splitEditorPane.findPaneContaining(selected);
            if (pane != null) {
                pane.getTabs().remove(selected);
            }
        }
    }

    public boolean promptSaveAllDirty() {
        List<EditorTab> dirtyTabs = new ArrayList<>();
        collectDirtyTabs(splitEditorPane.getPrimaryPane(), dirtyTabs);
        if (splitEditorPane.isSplit() && splitEditorPane.getSecondaryPane() != null) {
            collectDirtyTabs(splitEditorPane.getSecondaryPane(), dirtyTabs);
        }

        if (dirtyTabs.isEmpty()) {
            return true;
        }

        boolean save = DialogHelper.showConfirmation("Unsaved Changes",
                "You have " + dirtyTabs.size() + " unsaved file(s). Save all before closing?");
        if (save) {
            for (EditorTab tab : dirtyTabs) {
                tab.save();
            }
        }
        return true;
    }

    public void restoreSession(List<Path> paths, int selectedIndex) {
        for (Path path : paths) {
            if (Files.exists(path)) {
                openFile(path);
            }
        }
        DraggableTabPane primary = splitEditorPane.getPrimaryPane();
        if (selectedIndex >= 0 && selectedIndex < primary.getTabs().size()) {
            primary.getSelectionModel().select(selectedIndex);
        }
    }

    public void splitRight() {
        Tab selected = getActiveSelectedTab();
        if (selected instanceof EditorTab) {
            splitEditorPane.splitRight(selected);
        }
    }

    public SplitEditorPane getSplitEditorPane() {
        return splitEditorPane;
    }

    private Tab getActiveSelectedTab() {
        DraggableTabPane active = splitEditorPane.getActivePane();
        return active.getSelectionModel().getSelectedItem();
    }

    private Tab findTab(Path path) {
        Tab found = findTabInPane(splitEditorPane.getPrimaryPane(), path);
        if (found != null) return found;
        if (splitEditorPane.isSplit() && splitEditorPane.getSecondaryPane() != null) {
            found = findTabInPane(splitEditorPane.getSecondaryPane(), path);
        }
        return found;
    }

    private Tab findTabInPane(TabPane pane, Path path) {
        for (Tab tab : pane.getTabs()) {
            if (tab instanceof EditorTab editorTab && editorTab.getFilePath().equals(path)) {
                return tab;
            }
        }
        return null;
    }

    private void collectDirtyTabs(TabPane pane, List<EditorTab> dirtyTabs) {
        for (Tab tab : pane.getTabs()) {
            if (tab instanceof EditorTab editorTab && editorTab.isDirty()) {
                dirtyTabs.add(editorTab);
            }
        }
    }
}
