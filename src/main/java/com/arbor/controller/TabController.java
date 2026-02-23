package com.arbor.controller;

import com.arbor.service.FileOperationService;
import com.arbor.util.DialogHelper;
import com.arbor.view.EditorTab;
import com.arbor.view.WelcomeView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TabController {
    private final TabPane tabPane;
    private final FileOperationService fileOps;
    private final Tab welcomeTab;

    public TabController(TabPane tabPane, FileOperationService fileOps) {
        this.tabPane = tabPane;
        this.fileOps = fileOps;

        this.welcomeTab = new Tab("Welcome");
        welcomeTab.setClosable(false);
        welcomeTab.setContent(new WelcomeView());
        tabPane.getTabs().add(welcomeTab);

        tabPane.getTabs().addListener((javafx.collections.ListChangeListener<Tab>) change -> {
            while (change.next()) {
                if (tabPane.getTabs().isEmpty()) {
                    tabPane.getTabs().add(welcomeTab);
                }
            }
        });
    }

    public void openFile(Path path) {
        // Check if already open
        for (Tab tab : tabPane.getTabs()) {
            if (tab instanceof EditorTab editorTab && editorTab.getFilePath().equals(path)) {
                tabPane.getSelectionModel().select(tab);
                return;
            }
        }

        // Remove welcome tab if present
        tabPane.getTabs().remove(welcomeTab);

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

        tabPane.getTabs().add(editorTab);
        tabPane.getSelectionModel().select(editorTab);
    }

    public void saveCurrentTab() {
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        if (selected instanceof EditorTab editorTab) {
            editorTab.save();
        }
    }

    public void closeCurrentTab() {
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        if (selected != null && selected != welcomeTab) {
            if (selected instanceof EditorTab editorTab && editorTab.isDirty()) {
                boolean save = DialogHelper.showConfirmation("Unsaved Changes",
                        "Save changes before closing?");
                if (save) {
                    editorTab.save();
                }
            }
            tabPane.getTabs().remove(selected);
        }
    }

    public boolean promptSaveAllDirty() {
        List<EditorTab> dirtyTabs = new ArrayList<>();
        for (Tab tab : tabPane.getTabs()) {
            if (tab instanceof EditorTab editorTab && editorTab.isDirty()) {
                dirtyTabs.add(editorTab);
            }
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
        return true; // allow close regardless
    }

    public TabPane getTabPane() {
        return tabPane;
    }
}
