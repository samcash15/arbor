package com.arbor;

import com.arbor.controller.TabController;
import com.arbor.model.ArborConfig;
import com.arbor.model.CommandEntry;
import com.arbor.model.Grove;
import com.arbor.service.*;
import com.arbor.view.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Arbor - A place where your ideas take root.
 *
 * Main application entry point.
 */
public class App extends Application {
    private static final Logger log = LoggerFactory.getLogger(App.class);

    private ConfigService configService;
    private GroveService groveService;
    private FileOperationService fileOps;
    private FileTreeService treeService;
    private SearchService searchService;
    private OutlineService outlineService;
    private BacklinkService backlinkService;
    private TabController tabController;
    private SearchBar searchBar;
    private OutlinePanel outlinePanel;
    private Grove grove;
    private Scene mainScene;
    private SplitEditorPane splitEditorPane;
    private SplitPane mainSplitPane;
    private CommandRegistry commandRegistry;
    private CommandPalette commandPalette;
    private boolean outlineVisible = false;

    @Override
    public void start(Stage primaryStage) {
        // Initialize services
        configService = new ConfigService();
        groveService = new GroveService();
        fileOps = new FileOperationService();
        treeService = new FileTreeService();
        searchService = new SearchService();
        outlineService = new OutlineService();
        backlinkService = new BacklinkService();

        ArborConfig config = configService.getConfig();

        // Determine grove path
        Path grovePath = resolveGrovePath(primaryStage, config);
        if (grovePath == null) {
            System.exit(0);
            return;
        }

        // Load or create grove
        try {
            grove = groveService.loadOrCreateGrove(grovePath);
        } catch (IOException e) {
            log.error("Failed to load grove", e);
            grove = new Grove(grovePath, grovePath.getFileName().toString());
        }

        // Update config
        config.setLastGrovePath(grovePath);
        config.addRecentGrove(grovePath);
        configService.save();

        // Initialize backlink service
        backlinkService.setGrovePath(grovePath);
        Thread.startVirtualThread(backlinkService::fullScan);

        // Build UI
        BorderPane root = new BorderPane();

        // Tab pane + split editor + controller
        DraggableTabPane primaryTabPane = new DraggableTabPane();
        splitEditorPane = new SplitEditorPane(primaryTabPane);
        tabController = new TabController(splitEditorPane, fileOps);

        // Restore previous session tabs
        if (config.getOpenTabs() != null && !config.getOpenTabs().isEmpty()) {
            java.util.List<Path> tabPaths = config.getOpenTabs().stream()
                    .map(Path::of)
                    .toList();
            tabController.restoreSession(tabPaths, config.getSelectedTabIndex());
        }

        // File tree panel
        FileTreePanel fileTreePanel = new FileTreePanel(treeService, fileOps, tabController::openFile);
        fileTreePanel.loadGrove(grovePath);

        // Outline panel (starts hidden)
        outlinePanel = new OutlinePanel(outlineService);

        // Search bar (inline, hidden by default)
        searchBar = new SearchBar(searchService);
        searchBar.setRootPath(grovePath);
        searchBar.setOnFileOpen(tabController::openFile);

        // Toolbar
        ToolbarView[] toolbarHolder = new ToolbarView[1];
        ToolbarView toolbar = new ToolbarView(
                searchBar::toggle,
                () -> switchGrove(primaryStage, toolbarHolder[0], fileTreePanel, config),
                () -> openSettings(primaryStage, toolbarHolder[0], fileTreePanel, config),
                this::toggleOutlinePanel
        );
        toolbarHolder[0] = toolbar;
        toolbar.setGroveName(grove.getName());

        // Place toolbar + search bar in a VBox at the top
        VBox topArea = new VBox(toolbar, searchBar);
        root.setTop(topArea);

        // Status bar
        StatusBar statusBar = new StatusBar();
        statusBar.bindToSplitEditorPane(splitEditorPane);
        root.setBottom(statusBar);

        // Command palette
        commandRegistry = new CommandRegistry();
        commandPalette = new CommandPalette(commandRegistry);
        commandPalette.setOnHide(() -> {
            Tab active = splitEditorPane.getActivePane().getSelectionModel().getSelectedItem();
            if (active instanceof EditorTab editorTab) {
                editorTab.getTextArea().requestFocus();
            }
        });

        // Wrap editor in StackPane for command palette overlay
        StackPane editorStack = new StackPane(splitEditorPane, commandPalette);
        StackPane.setAlignment(commandPalette, Pos.TOP_CENTER);
        StackPane.setMargin(commandPalette, new Insets(10, 0, 0, 0));

        // Split pane
        mainSplitPane = new SplitPane(fileTreePanel, editorStack);
        mainSplitPane.setDividerPositions(config.getDividerPosition());
        root.setCenter(mainSplitPane);

        // Wire tab selection listener for outline and backlinks
        wireTabListeners(primaryTabPane, grovePath);

        // Add subtle border for undecorated window
        root.getStyleClass().add("app-root");

        // Clip to rounded corners
        Rectangle clip = new Rectangle();
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        clip.widthProperty().bind(root.widthProperty());
        clip.heightProperty().bind(root.heightProperty());
        root.setClip(clip);

        // Scene
        double width = config.getWindowWidth();
        double height = config.getWindowHeight();
        Scene scene = new Scene(root, width, height, javafx.scene.paint.Color.TRANSPARENT);

        enableWindowResize(scene, primaryStage);

        // Load CSS
        var cssUrl = getClass().getResource("/css/arbor.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        // Store references
        this.mainScene = scene;

        // Apply theme from config
        if ("dark".equals(config.getTheme())) {
            root.getStyleClass().add("dark");
        }

        // Keyboard shortcuts
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN),
                tabController::saveCurrentTab
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN),
                tabController::closeCurrentTab
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN),
                () -> createNewFile(grovePath, fileTreePanel)
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
                () -> createNewFolder(grovePath, fileTreePanel)
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
                searchBar::toggle
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
                this::toggleOutlinePanel
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
                commandPalette::toggle
        );

        // Register commands
        registerCommands(grovePath, fileTreePanel, primaryStage, toolbarHolder[0], config);

        // Window close handler
        primaryStage.setOnCloseRequest(event -> {
            tabController.promptSaveAllDirty();

            // Save open tabs for session restore
            java.util.List<String> openTabPaths = new java.util.ArrayList<>();
            DraggableTabPane primary = splitEditorPane.getPrimaryPane();
            int selectedIdx = primary.getSelectionModel().getSelectedIndex();
            for (var tab : primary.getTabs()) {
                if (tab instanceof EditorTab editorTab) {
                    openTabPaths.add(editorTab.getFilePath().toString());
                }
            }
            // Also save secondary pane tabs
            if (splitEditorPane.isSplit() && splitEditorPane.getSecondaryPane() != null) {
                for (var tab : splitEditorPane.getSecondaryPane().getTabs()) {
                    if (tab instanceof EditorTab editorTab) {
                        openTabPaths.add(editorTab.getFilePath().toString());
                    }
                }
            }
            config.setOpenTabs(openTabPaths);
            config.setSelectedTabIndex(selectedIdx);

            // Persist window state
            config.setWindowWidth(primaryStage.getWidth());
            config.setWindowHeight(primaryStage.getHeight());
            if (!mainSplitPane.getDividers().isEmpty()) {
                config.setDividerPosition(mainSplitPane.getDividerPositions()[0]);
            }
            configService.save();
        });

        // Stage — undecorated for modern look
        primaryStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        primaryStage.getIcons().add(new javafx.scene.image.Image(
                getClass().getResourceAsStream("/images/tree.png")));
        primaryStage.setTitle("Arbor - " + grove.getName());
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    private void wireTabListeners(DraggableTabPane tabPane, Path grovePath) {
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab instanceof EditorTab editorTab) {
                // Outline panel
                if (outlineVisible) {
                    outlinePanel.bindToTab(editorTab);
                }

                // Backlink navigation
                editorTab.setOnBacklinkNavigate(linkText -> {
                    Path target = backlinkService.resolveLink(linkText, grove.getRootPath());
                    if (target != null) {
                        tabController.openFile(target);
                    }
                });

                // Save callback for backlink re-indexing
                editorTab.setOnSaveCallback(() -> {
                    backlinkService.rescanFile(editorTab.getFilePath());
                    // Update backlinks panel for any visible tab
                    Tab activeTab = splitEditorPane.getActivePane().getSelectionModel().getSelectedItem();
                    if (activeTab instanceof EditorTab activeEditor) {
                        updateBacklinksForTab(activeEditor);
                    }
                });

                // Split right action
                editorTab.setOnSplitRight(() -> splitEditorPane.splitRight(editorTab));

                // Backlinks panel
                updateBacklinksForTab(editorTab);
            } else {
                outlinePanel.clear();
            }
        });
    }

    private void updateBacklinksForTab(EditorTab editorTab) {
        BacklinksPanel backlinksPanelForTab = new BacklinksPanel(backlinkService);
        backlinksPanelForTab.setOnFileOpen(tabController::openFile);
        backlinksPanelForTab.updateForFile(editorTab.getFilePath());
        editorTab.setBacklinksPanel(backlinksPanelForTab);
    }

    private void registerCommands(Path grovePath, FileTreePanel fileTreePanel,
                                    Stage stage, ToolbarView toolbar, ArborConfig config) {
        commandRegistry.clear();
        commandRegistry.registerAll(List.of(
                // File
                new CommandEntry("New File", "File", "Ctrl+N",
                        () -> createNewFile(grovePath, fileTreePanel)),
                new CommandEntry("New Folder", "File", "Ctrl+Shift+N",
                        () -> createNewFolder(grovePath, fileTreePanel)),
                new CommandEntry("Save", "File", "Ctrl+S",
                        tabController::saveCurrentTab),
                new CommandEntry("Close Tab", "File", "Ctrl+W",
                        tabController::closeCurrentTab),

                // Editor
                new CommandEntry("Find", "Editor", "Ctrl+F", () -> {
                    Tab active = splitEditorPane.getActivePane().getSelectionModel().getSelectedItem();
                    if (active instanceof EditorTab editorTab) {
                        editorTab.showFind();
                    }
                }),
                new CommandEntry("Find and Replace", "Editor", "Ctrl+H", () -> {
                    Tab active = splitEditorPane.getActivePane().getSelectionModel().getSelectedItem();
                    if (active instanceof EditorTab editorTab) {
                        editorTab.showFindAndReplace();
                    }
                }),
                new CommandEntry("Split Right", "Editor", null,
                        tabController::splitRight),

                // View
                new CommandEntry("Toggle Search", "View", "Ctrl+Shift+F",
                        searchBar::toggle),
                new CommandEntry("Toggle Outline", "View", "Ctrl+Shift+O",
                        this::toggleOutlinePanel),
                new CommandEntry("Toggle Dark Theme", "View", null, () -> {
                    boolean isDark = mainScene.getRoot().getStyleClass().contains("dark");
                    String newTheme = isDark ? "light" : "dark";
                    config.setTheme(newTheme);
                    configService.save();
                    applyTheme(newTheme);
                }),
                new CommandEntry("Command Palette", "View", "Ctrl+Shift+P",
                        commandPalette::toggle),

                // Grove
                new CommandEntry("Switch Grove", "Grove", null,
                        () -> switchGrove(stage, toolbar, fileTreePanel, config)),
                new CommandEntry("Open Settings", "Grove", null,
                        () -> openSettings(stage, toolbar, fileTreePanel, config))
        ));
    }

    private void toggleOutlinePanel() {
        if (outlineVisible) {
            mainSplitPane.getItems().remove(outlinePanel);
            outlinePanel.clear();
            outlineVisible = false;
        } else {
            mainSplitPane.getItems().add(outlinePanel);
            // Set divider to ~75%
            if (mainSplitPane.getDividers().size() >= 2) {
                mainSplitPane.setDividerPosition(1, 0.75);
            }
            outlineVisible = true;

            // Bind to current tab
            Tab selected = splitEditorPane.getActivePane().getSelectionModel().getSelectedItem();
            if (selected instanceof EditorTab editorTab) {
                outlinePanel.bindToTab(editorTab);
            }
        }
    }

    private Path resolveGrovePath(Stage stage, ArborConfig config) {
        if (config.getLastGrovePath() != null && Files.isDirectory(config.getLastGrovePath())) {
            return config.getLastGrovePath();
        }

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose a Grove Directory");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File selected = chooser.showDialog(stage);
        return selected != null ? selected.toPath() : null;
    }

    private void createNewFile(Path grovePath, FileTreePanel panel) {
        com.arbor.util.DialogHelper.showTextInput("New File", "File name:", "untitled.md")
                .ifPresent(name -> {
                    try {
                        Path newFile = fileOps.createFile(grovePath, name);
                        panel.refresh();
                        tabController.openFile(newFile);
                    } catch (IOException e) {
                        com.arbor.util.DialogHelper.showError("Error", "Could not create file: " + e.getMessage());
                    }
                });
    }

    private void createNewFolder(Path grovePath, FileTreePanel panel) {
        com.arbor.util.DialogHelper.showTextInput("New Folder", "Folder name:", "New Folder")
                .ifPresent(name -> {
                    try {
                        fileOps.createDirectory(grovePath, name);
                        panel.refresh();
                    } catch (IOException e) {
                        com.arbor.util.DialogHelper.showError("Error", "Could not create folder: " + e.getMessage());
                    }
                });
    }

    private void openSettings(Stage stage, ToolbarView toolbar, FileTreePanel fileTreePanel,
                               ArborConfig config) {
        SettingsDialog dialog = new SettingsDialog(stage, configService,
                () -> switchGrove(stage, toolbar, fileTreePanel, config),
                () -> applyTheme(config.getTheme()));
        dialog.show();
    }

    private void applyTheme(String theme) {
        boolean dark = "dark".equals(theme);

        mainScene.getRoot().getStyleClass().remove("dark");
        if (dark) {
            mainScene.getRoot().getStyleClass().add("dark");
        }

        // Update all open editor tabs in both panes
        applyThemeToPane(splitEditorPane.getPrimaryPane(), dark);
        if (splitEditorPane.isSplit() && splitEditorPane.getSecondaryPane() != null) {
            applyThemeToPane(splitEditorPane.getSecondaryPane(), dark);
        }
    }

    private void applyThemeToPane(DraggableTabPane pane, boolean dark) {
        for (var tab : pane.getTabs()) {
            if (tab instanceof EditorTab editorTab) {
                editorTab.setDarkMode(dark);
            }
        }
    }

    private void switchGrove(Stage stage, ToolbarView toolbar, FileTreePanel fileTreePanel,
                              ArborConfig config) {
        tabController.promptSaveAllDirty();

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Switch Grove — Choose a Directory");
        if (grove != null && Files.isDirectory(grove.getRootPath())) {
            chooser.setInitialDirectory(grove.getRootPath().toFile());
        }
        File selected = chooser.showDialog(stage);
        if (selected == null) {
            return;
        }

        Path newPath = selected.toPath();
        try {
            grove = groveService.loadOrCreateGrove(newPath);
        } catch (IOException e) {
            log.error("Failed to load grove", e);
            grove = new Grove(newPath, newPath.getFileName().toString());
        }

        // Update config
        config.setLastGrovePath(newPath);
        config.addRecentGrove(newPath);
        configService.save();

        config.setOpenTabs(new java.util.ArrayList<>());
        config.setSelectedTabIndex(0);

        // Reload UI
        toolbar.setGroveName(grove.getName());
        stage.setTitle("Arbor - " + grove.getName());
        fileTreePanel.loadGrove(newPath);

        // Clear editor tabs and recreate controller
        splitEditorPane.collapseSplit();
        splitEditorPane.getPrimaryPane().getTabs().clear();
        tabController = new TabController(splitEditorPane, fileOps);

        // Re-init backlink service
        backlinkService.setGrovePath(newPath);
        Thread.startVirtualThread(backlinkService::fullScan);

        // Rebind outline
        if (outlineVisible) {
            outlinePanel.clear();
        }

        // Rewire search bar
        searchBar.setRootPath(newPath);
        searchBar.setOnFileOpen(tabController::openFile);
        searchBar.hide();

        // Rewire file tree
        fileTreePanel.getTreeView().setCellFactory(tv ->
                new PathTreeCell(fileOps, treeService, tabController::openFile));

        // Rewire tab listeners
        wireTabListeners(splitEditorPane.getPrimaryPane(), newPath);

        // Re-register commands with new grove references
        registerCommands(newPath, fileTreePanel, stage, toolbar, config);
    }

    private void enableWindowResize(Scene scene, Stage stage) {
        final int RESIZE_MARGIN = 6;
        final double MIN_WIDTH = 600;
        final double MIN_HEIGHT = 400;

        scene.setOnMouseMoved(event -> {
            double x = event.getX(), y = event.getY();
            double w = scene.getWidth(), h = scene.getHeight();

            if (stage.isMaximized()) {
                scene.setCursor(javafx.scene.Cursor.DEFAULT);
                return;
            }

            if (x < RESIZE_MARGIN && y < RESIZE_MARGIN) scene.setCursor(javafx.scene.Cursor.NW_RESIZE);
            else if (x > w - RESIZE_MARGIN && y < RESIZE_MARGIN) scene.setCursor(javafx.scene.Cursor.NE_RESIZE);
            else if (x < RESIZE_MARGIN && y > h - RESIZE_MARGIN) scene.setCursor(javafx.scene.Cursor.SW_RESIZE);
            else if (x > w - RESIZE_MARGIN && y > h - RESIZE_MARGIN) scene.setCursor(javafx.scene.Cursor.SE_RESIZE);
            else if (x < RESIZE_MARGIN) scene.setCursor(javafx.scene.Cursor.W_RESIZE);
            else if (x > w - RESIZE_MARGIN) scene.setCursor(javafx.scene.Cursor.E_RESIZE);
            else if (y < RESIZE_MARGIN) scene.setCursor(javafx.scene.Cursor.N_RESIZE);
            else if (y > h - RESIZE_MARGIN) scene.setCursor(javafx.scene.Cursor.S_RESIZE);
            else scene.setCursor(javafx.scene.Cursor.DEFAULT);
        });

        final double[] dragStart = new double[4];

        scene.setOnMousePressed(event -> {
            dragStart[0] = event.getScreenX();
            dragStart[1] = event.getScreenY();
            dragStart[2] = stage.getWidth();
            dragStart[3] = stage.getHeight();
        });

        scene.setOnMouseDragged(event -> {
            javafx.scene.Cursor cursor = scene.getCursor();
            if (cursor == javafx.scene.Cursor.DEFAULT || stage.isMaximized()) return;

            double dx = event.getScreenX() - dragStart[0];
            double dy = event.getScreenY() - dragStart[1];

            if (cursor == javafx.scene.Cursor.E_RESIZE || cursor == javafx.scene.Cursor.SE_RESIZE || cursor == javafx.scene.Cursor.NE_RESIZE) {
                double newW = dragStart[2] + dx;
                if (newW >= MIN_WIDTH) stage.setWidth(newW);
            }
            if (cursor == javafx.scene.Cursor.S_RESIZE || cursor == javafx.scene.Cursor.SE_RESIZE || cursor == javafx.scene.Cursor.SW_RESIZE) {
                double newH = dragStart[3] + dy;
                if (newH >= MIN_HEIGHT) stage.setHeight(newH);
            }
            if (cursor == javafx.scene.Cursor.W_RESIZE || cursor == javafx.scene.Cursor.SW_RESIZE || cursor == javafx.scene.Cursor.NW_RESIZE) {
                double newW = dragStart[2] - dx;
                if (newW >= MIN_WIDTH) {
                    stage.setX(event.getScreenX());
                    stage.setWidth(newW);
                }
            }
            if (cursor == javafx.scene.Cursor.N_RESIZE || cursor == javafx.scene.Cursor.NW_RESIZE || cursor == javafx.scene.Cursor.NE_RESIZE) {
                double newH = dragStart[3] - dy;
                if (newH >= MIN_HEIGHT) {
                    stage.setY(event.getScreenY());
                    stage.setHeight(newH);
                }
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
