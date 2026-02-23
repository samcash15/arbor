package com.arbor.view;

import com.arbor.service.FileOperationService;
import com.arbor.service.SyntaxHighlightService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Tab;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

public class EditorTab extends Tab {
    private static final Logger log = LoggerFactory.getLogger(EditorTab.class);
    private static final long AUTOSAVE_DELAY_MS = 2000;
    private static final long PREVIEW_DEBOUNCE_MS = 300;

    private static final Parser MD_PARSER = Parser.builder().build();
    private static final HtmlRenderer MD_RENDERER = HtmlRenderer.builder().build();

    public enum ViewMode { EDIT, SPLIT, PREVIEW }

    private final Path filePath;
    private final StyleClassedTextArea textArea;
    private final FileOperationService fileOps;
    private final boolean isMarkdown;

    private final BorderPane rootPane;
    private final VirtualizedScrollPane<StyleClassedTextArea> editorScrollPane;
    private final StackPane editorWrapper;
    private WebView webView;

    private static final SyntaxHighlightService syntaxService = new SyntaxHighlightService();

    private FindReplaceBar findReplaceBar;
    private ViewMode currentMode = ViewMode.EDIT;
    private boolean dirty = false;
    private String savedContent;
    private Timer autosaveTimer;
    private Timer previewTimer;
    private final String language;
    private boolean darkMode = false;

    public EditorTab(Path filePath, FileOperationService fileOps) {
        this.filePath = filePath;
        this.fileOps = fileOps;
        this.isMarkdown = filePath.getFileName().toString().toLowerCase().endsWith(".md");
        this.language = syntaxService.detectLanguage(filePath);

        textArea = new StyleClassedTextArea();
        textArea.getStyleClass().add("editor-area");
        textArea.setWrapText(true);
        textArea.setParagraphGraphicFactory(org.fxmisc.richtext.LineNumberFactory.get(textArea));

        editorScrollPane = new VirtualizedScrollPane<>(textArea);

        // Wrap in StackPane so VirtualizedScrollPane sizes properly in SplitPane
        editorWrapper = new StackPane(editorScrollPane);

        rootPane = new BorderPane();

        // Add mode toggle bar for markdown files
        if (isMarkdown) {
            rootPane.setTop(createModeToggleBar());
        }

        // Find/Replace bar (hidden by default)
        findReplaceBar = new FindReplaceBar(textArea);
        findReplaceBar.setVisible(false);
        findReplaceBar.setManaged(false);

        // Place find bar between mode toggle and editor
        VBox editorArea = new VBox(findReplaceBar, editorWrapper);
        javafx.scene.layout.VBox.setVgrow(editorWrapper, Priority.ALWAYS);
        rootPane.setCenter(editorArea);

        // Keyboard shortcuts
        textArea.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.HOME) {
                textArea.moveTo(0);
                textArea.requestFollowCaret();
                event.consume();
            } else if (event.isControlDown() && event.getCode() == KeyCode.END) {
                textArea.moveTo(textArea.getLength());
                textArea.requestFollowCaret();
                event.consume();
            } else if (event.isControlDown() && event.getCode() == KeyCode.F) {
                findReplaceBar.show(false);
                event.consume();
            } else if (event.isControlDown() && event.getCode() == KeyCode.H) {
                findReplaceBar.show(true);
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                findReplaceBar.close();
                event.consume();
            }
        });
        setContent(rootPane);
        updateTabTitle();

        // Load file content
        try {
            savedContent = fileOps.readFile(filePath);
            textArea.replaceText(savedContent);
        } catch (IOException e) {
            log.error("Failed to read file: {}", filePath, e);
            savedContent = "";
        }

        // Apply syntax highlighting
        if (language != null) {
            applySyntaxHighlighting();
            textArea.multiPlainChanges()
                    .successionEnds(Duration.ofMillis(150))
                    .subscribe(changes -> applySyntaxHighlighting());
        }

        // Track dirty state, schedule autosave, and update preview
        textArea.textProperty().addListener((obs, oldText, newText) -> {
            boolean wasDirty = dirty;
            dirty = !newText.equals(savedContent);
            if (wasDirty != dirty) {
                updateTabTitle();
            }
            if (dirty) {
                scheduleAutosave();
            }
            if (isMarkdown && currentMode != ViewMode.EDIT) {
                schedulePreviewUpdate();
            }
        });

        // Cancel timers when tab is closed
        setOnClosed(e -> {
            if (autosaveTimer != null) {
                autosaveTimer.cancel();
            }
            if (previewTimer != null) {
                previewTimer.cancel();
            }
        });
    }

    private HBox createModeToggleBar() {
        ToggleGroup group = new ToggleGroup();

        ToggleButton editBtn = new ToggleButton("Edit");
        editBtn.setToggleGroup(group);
        editBtn.setSelected(true);
        editBtn.getStyleClass().add("mode-toggle");

        ToggleButton splitBtn = new ToggleButton("Split");
        splitBtn.setToggleGroup(group);
        splitBtn.getStyleClass().add("mode-toggle");

        ToggleButton previewBtn = new ToggleButton("Preview");
        previewBtn.setToggleGroup(group);
        previewBtn.getStyleClass().add("mode-toggle");

        // Prevent deselecting all toggles
        group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true);
            }
        });

        editBtn.setOnAction(e -> setViewMode(ViewMode.EDIT));
        splitBtn.setOnAction(e -> setViewMode(ViewMode.SPLIT));
        previewBtn.setOnAction(e -> setViewMode(ViewMode.PREVIEW));

        HBox bar = new HBox(2, editBtn, splitBtn, previewBtn);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(4, 8, 4, 8));
        bar.getStyleClass().add("mode-toggle-bar");
        return bar;
    }

    private void applySyntaxHighlighting() {
        try {
            textArea.setStyleSpans(0, syntaxService.computeHighlighting(textArea.getText(), language));
        } catch (Exception e) {
            log.debug("Syntax highlighting failed", e);
        }
    }

    private WebView getOrCreateWebView() {
        if (webView == null) {
            webView = new WebView();
            applyPreviewStylesheet();
        }
        return webView;
    }

    private void applyPreviewStylesheet() {
        if (webView == null) return;
        String cssFile = darkMode ? "/css/markdown-preview-dark.css" : "/css/markdown-preview.css";
        webView.getEngine().setUserStyleSheetLocation(
                getClass().getResource(cssFile).toExternalForm());
    }

    public void setDarkMode(boolean dark) {
        this.darkMode = dark;
        applyPreviewStylesheet();
        if (isMarkdown && currentMode != ViewMode.EDIT) {
            refreshPreview();
        }
    }

    private void setViewMode(ViewMode mode) {
        currentMode = mode;
        javafx.scene.Node mainContent;
        switch (mode) {
            case SPLIT -> {
                WebView wv = getOrCreateWebView();
                javafx.scene.control.SplitPane split = new javafx.scene.control.SplitPane(editorWrapper, wv);
                split.setDividerPositions(0.5);
                mainContent = split;
                refreshPreview();
            }
            case PREVIEW -> {
                mainContent = getOrCreateWebView();
                refreshPreview();
            }
            default -> mainContent = editorWrapper;
        }
        VBox editorArea = new VBox(findReplaceBar, mainContent);
        javafx.scene.layout.VBox.setVgrow(mainContent, Priority.ALWAYS);
        rootPane.setCenter(editorArea);
    }

    private void refreshPreview() {
        if (webView == null) return;
        String markdown = textArea.getText();
        String html = MD_RENDERER.render(MD_PARSER.parse(markdown));
        String fullHtml = "<html><head><meta charset='UTF-8'></head><body>" + html + "</body></html>";
        webView.getEngine().loadContent(fullHtml, "text/html");
    }

    private void schedulePreviewUpdate() {
        if (previewTimer != null) {
            previewTimer.cancel();
        }
        previewTimer = new Timer(true);
        previewTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> refreshPreview());
            }
        }, PREVIEW_DEBOUNCE_MS);
    }

    private void scheduleAutosave() {
        if (autosaveTimer != null) {
            autosaveTimer.cancel();
        }
        autosaveTimer = new Timer(true);
        autosaveTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (dirty) {
                        save();
                        log.debug("Autosaved: {}", filePath);
                    }
                });
            }
        }, AUTOSAVE_DELAY_MS);
    }

    private void updateTabTitle() {
        String name = filePath.getFileName().toString();
        setText(dirty ? "* " + name : name);
    }

    public boolean save() {
        try {
            String content = textArea.getText();
            fileOps.writeFile(filePath, content);
            savedContent = content;
            dirty = false;
            updateTabTitle();
            log.debug("Saved: {}", filePath);
            return true;
        } catch (IOException e) {
            log.error("Failed to save: {}", filePath, e);
            return false;
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public Path getFilePath() {
        return filePath;
    }

    public StyleClassedTextArea getTextArea() {
        return textArea;
    }
}
