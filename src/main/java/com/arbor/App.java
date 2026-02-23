package com.arbor;

import com.arbor.controller.TabController;
import com.arbor.model.ArborConfig;
import com.arbor.model.Grove;
import com.arbor.service.*;
import com.arbor.view.FileTreePanel;
import com.arbor.view.SearchBar;
import com.arbor.view.SettingsDialog;
import com.arbor.view.StatusBar;
import com.arbor.view.ToolbarView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
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
    private TabController tabController;
    private SearchBar searchBar;
    private Grove grove;
    private Scene mainScene;
    private TabPane mainTabPane;

    @Override
    public void start(Stage primaryStage) {
        // Initialize services
        configService = new ConfigService();
        groveService = new GroveService();
        fileOps = new FileOperationService();
        treeService = new FileTreeService();
        searchService = new SearchService();

        ArborConfig config = configService.getConfig();

        // Determine grove path
        Path grovePath = resolveGrovePath(primaryStage, config);
        if (grovePath == null) {
            // User cancelled — exit
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

        // Build UI
        BorderPane root = new BorderPane();

        // Tab pane + controller
        TabPane tabPane = new TabPane();
        tabController = new TabController(tabPane, fileOps);

        // File tree panel
        FileTreePanel fileTreePanel = new FileTreePanel(treeService, fileOps, tabController::openFile);
        fileTreePanel.loadGrove(grovePath);

        // Search bar (inline, hidden by default)
        searchBar = new SearchBar(searchService);
        searchBar.setRootPath(grovePath);
        searchBar.setOnFileOpen(tabController::openFile);

        // Toolbar (use array to allow self-reference in lambda)
        ToolbarView[] toolbarHolder = new ToolbarView[1];
        ToolbarView toolbar = new ToolbarView(
                searchBar::toggle,
                () -> switchGrove(primaryStage, toolbarHolder[0], fileTreePanel, tabPane, config),
                () -> openSettings(primaryStage, toolbarHolder[0], fileTreePanel, tabPane, config)
        );
        toolbarHolder[0] = toolbar;
        toolbar.setGroveName(grove.getName());

        // Place toolbar + search bar in a VBox at the top
        VBox topArea = new VBox(toolbar, searchBar);
        root.setTop(topArea);

        // Status bar
        StatusBar statusBar = new StatusBar();
        statusBar.bindToTabPane(tabPane);
        root.setBottom(statusBar);

        // Split pane
        SplitPane splitPane = new SplitPane(fileTreePanel, tabPane);
        splitPane.setDividerPositions(config.getDividerPosition());
        root.setCenter(splitPane);

        // Add subtle border for undecorated window
        root.getStyleClass().add("app-root");

        // Clip to rounded corners so child content doesn't overflow
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

        // Enable window resizing on edges
        enableWindowResize(scene, primaryStage);

        // Load CSS
        var cssUrl = getClass().getResource("/css/arbor.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        // Store references for theme switching
        this.mainScene = scene;
        this.mainTabPane = tabPane;

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

        // Window close handler
        primaryStage.setOnCloseRequest(event -> {
            tabController.promptSaveAllDirty();

            // Persist window state
            config.setWindowWidth(primaryStage.getWidth());
            config.setWindowHeight(primaryStage.getHeight());
            if (!splitPane.getDividers().isEmpty()) {
                config.setDividerPosition(splitPane.getDividerPositions()[0]);
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

    private Path resolveGrovePath(Stage stage, ArborConfig config) {
        // Try last grove path
        if (config.getLastGrovePath() != null && Files.isDirectory(config.getLastGrovePath())) {
            return config.getLastGrovePath();
        }

        // Show directory chooser
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
                               TabPane tabPane, ArborConfig config) {
        SettingsDialog dialog = new SettingsDialog(stage, configService,
                () -> switchGrove(stage, toolbar, fileTreePanel, tabPane, config),
                () -> applyTheme(config.getTheme()));
        dialog.show();
    }

    private void applyTheme(String theme) {
        boolean dark = "dark".equals(theme);

        // Toggle "dark" style class on the scene root
        mainScene.getRoot().getStyleClass().remove("dark");
        if (dark) {
            mainScene.getRoot().getStyleClass().add("dark");
        }

        // Update all open editor tabs
        for (var tab : mainTabPane.getTabs()) {
            if (tab instanceof com.arbor.view.EditorTab editorTab) {
                editorTab.setDarkMode(dark);
            }
        }
    }

    private void switchGrove(Stage stage, ToolbarView toolbar, FileTreePanel fileTreePanel,
                              TabPane tabPane, ArborConfig config) {
        // Prompt to save dirty tabs first
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

        // Reload UI
        toolbar.setGroveName(grove.getName());
        stage.setTitle("Arbor - " + grove.getName());
        fileTreePanel.loadGrove(newPath);

        // Clear editor tabs and show welcome
        tabPane.getTabs().clear();
        tabController = new TabController(tabPane, fileOps);

        // Rewire search bar to new grove and tab controller
        searchBar.setRootPath(newPath);
        searchBar.setOnFileOpen(tabController::openFile);
        searchBar.hide();

        // Rewire file tree to new tab controller
        fileTreePanel.getTreeView().setCellFactory(tv ->
                new com.arbor.view.PathTreeCell(fileOps, treeService, tabController::openFile));
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

        final double[] dragStart = new double[4]; // startX, startY, stageW, stageH

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
