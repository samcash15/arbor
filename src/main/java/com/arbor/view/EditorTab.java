package com.arbor.view;

import com.arbor.service.BracketMatchService;
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
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.regex.Matcher;

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
    private static final BracketMatchService bracketService = new BracketMatchService();
    private int prevBracketA = -1;
    private int prevBracketB = -1;

    private FindReplaceBar findReplaceBar;
    private ViewMode currentMode = ViewMode.EDIT;
    private boolean dirty = false;
    private String savedContent;
    private Timer autosaveTimer;
    private Timer previewTimer;
    private final String language;
    private boolean darkMode = false;
    private Consumer<String> onBacklinkNavigate;
    private Runnable onSaveCallback;
    private Runnable onSplitRight;
    private boolean focusMode = false;
    private boolean typewriterMode = false;
    private javafx.beans.value.ChangeListener<Integer> typewriterListener;
    private int lastFocusParagraph = -1;

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
        // Ctrl+Click for backlink navigation
        textArea.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.isControlDown() && mouseEvent.getClickCount() == 1 && onBacklinkNavigate != null) {
                int pos = textArea.hit(mouseEvent.getX(), mouseEvent.getY()).getInsertionIndex();
                String link = extractBacklinkAt(textArea.getText(), pos);
                if (link != null) {
                    onBacklinkNavigate.accept(link);
                    mouseEvent.consume();
                }
            }
        });

        // Bracket matching on caret movement
        textArea.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
            updateBracketHighlights(newPos.intValue());
            if (focusMode) {
                updateFocusParagraphStyles();
            }
        });

        // Tab context menu
        javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
        javafx.scene.control.MenuItem splitRightItem = new javafx.scene.control.MenuItem("Split Right");
        splitRightItem.setOnAction(e -> { if (onSplitRight != null) onSplitRight.run(); });
        javafx.scene.control.MenuItem closeItem = new javafx.scene.control.MenuItem("Close");
        closeItem.setOnAction(e -> {
            if (isDirty()) {
                boolean save = com.arbor.util.DialogHelper.showConfirmation("Unsaved Changes",
                        "Save changes before closing?");
                if (save) save();
            }
            if (getTabPane() != null) getTabPane().getTabs().remove(this);
        });
        javafx.scene.control.MenuItem closeOthersItem = new javafx.scene.control.MenuItem("Close Others");
        closeOthersItem.setOnAction(e -> {
            if (getTabPane() != null) {
                getTabPane().getTabs().removeIf(t -> t != this && t.isClosable());
            }
        });
        contextMenu.getItems().addAll(splitRightItem, closeItem, closeOthersItem);
        setContextMenu(contextMenu);

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

        // Apply syntax highlighting (also overlays backlink styles)
        applySyntaxHighlighting();
        textArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(150))
                .subscribe(changes -> applySyntaxHighlighting());

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
            String text = textArea.getText();
            var spans = syntaxService.computeHighlighting(text, language);
            spans = SyntaxHighlightService.overlayBacklinks(spans, text);
            spans = SyntaxHighlightService.overlayTags(spans, text);
            textArea.setStyleSpans(0, spans);
        } catch (Exception e) {
            log.debug("Syntax highlighting failed", e);
        }
    }

    private void updateBracketHighlights(int caretPos) {
        try {
            // Clear previous bracket highlights by re-applying syntax spans at those positions
            if (prevBracketA >= 0 && prevBracketA < textArea.getLength()) {
                clearBracketStyle(prevBracketA);
            }
            if (prevBracketB >= 0 && prevBracketB < textArea.getLength()) {
                clearBracketStyle(prevBracketB);
            }
            prevBracketA = -1;
            prevBracketB = -1;

            BracketMatchService.BracketPair pair = bracketService.findMatchingBracket(textArea.getText(), caretPos);
            if (pair != null) {
                applyBracketStyle(pair.openPos());
                applyBracketStyle(pair.closePos());
                prevBracketA = pair.openPos();
                prevBracketB = pair.closePos();
            }
        } catch (Exception e) {
            log.debug("Bracket highlight failed", e);
        }
    }

    private void applyBracketStyle(int pos) {
        if (pos < 0 || pos >= textArea.getLength()) return;
        StyleSpans<Collection<String>> existing = textArea.getStyleSpans(pos, pos + 1);
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        existing.forEach(span -> {
            java.util.List<String> newStyles = new java.util.ArrayList<>(span.getStyle());
            if (!newStyles.contains("bracket-match")) {
                newStyles.add("bracket-match");
            }
            builder.add(newStyles, span.getLength());
        });
        textArea.setStyleSpans(pos, builder.create());
    }

    private void clearBracketStyle(int pos) {
        if (pos < 0 || pos >= textArea.getLength()) return;
        StyleSpans<Collection<String>> existing = textArea.getStyleSpans(pos, pos + 1);
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        existing.forEach(span -> {
            java.util.List<String> newStyles = new java.util.ArrayList<>(span.getStyle());
            newStyles.remove("bracket-match");
            builder.add(newStyles, span.getLength());
        });
        textArea.setStyleSpans(pos, builder.create());
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
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
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

    public void showFind() {
        findReplaceBar.show(false);
    }

    public void showFindAndReplace() {
        findReplaceBar.show(true);
    }

    public String getLanguage() {
        return language;
    }

    public boolean isMarkdown() {
        return isMarkdown;
    }

    public void setOnBacklinkNavigate(Consumer<String> onBacklinkNavigate) {
        this.onBacklinkNavigate = onBacklinkNavigate;
    }

    public void setOnSaveCallback(Runnable onSaveCallback) {
        this.onSaveCallback = onSaveCallback;
    }

    public void setOnSplitRight(Runnable onSplitRight) {
        this.onSplitRight = onSplitRight;
    }

    public void setBacklinksPanel(BacklinksPanel panel) {
        rootPane.setBottom(panel);
    }

    public BorderPane getRootPane() {
        return rootPane;
    }

    public void setFocusMode(boolean enabled) {
        this.focusMode = enabled;
        if (enabled) {
            textArea.getStyleClass().add("focus-mode");
            updateFocusParagraphStyles();
        } else {
            textArea.getStyleClass().remove("focus-mode");
            // Clear all paragraph styles
            for (int i = 0; i < textArea.getParagraphs().size(); i++) {
                textArea.setParagraphStyle(i, java.util.Collections.emptyList());
            }
            lastFocusParagraph = -1;
        }
    }

    public boolean isFocusMode() {
        return focusMode;
    }

    public void setTypewriterMode(boolean enabled) {
        this.typewriterMode = enabled;
        if (enabled) {
            typewriterListener = (obs, oldVal, newVal) -> scrollCaretToCenter();
            textArea.caretPositionProperty().addListener(typewriterListener);
            scrollCaretToCenter();
        } else {
            if (typewriterListener != null) {
                textArea.caretPositionProperty().removeListener(typewriterListener);
                typewriterListener = null;
            }
        }
    }

    public boolean isTypewriterMode() {
        return typewriterMode;
    }

    private void updateFocusParagraphStyles() {
        int currentParagraph = textArea.getCurrentParagraph();
        if (currentParagraph == lastFocusParagraph) return;

        int totalParagraphs = textArea.getParagraphs().size();

        // Clear old active paragraph
        if (lastFocusParagraph >= 0 && lastFocusParagraph < totalParagraphs) {
            textArea.setParagraphStyle(lastFocusParagraph, java.util.List.of("focus-dimmed"));
        }

        // Set new active paragraph
        if (currentParagraph >= 0 && currentParagraph < totalParagraphs) {
            textArea.setParagraphStyle(currentParagraph, java.util.List.of("focus-active"));
        }

        // On first activation, dim all non-active paragraphs
        if (lastFocusParagraph < 0) {
            for (int i = 0; i < totalParagraphs; i++) {
                if (i != currentParagraph) {
                    textArea.setParagraphStyle(i, java.util.List.of("focus-dimmed"));
                }
            }
        }

        lastFocusParagraph = currentParagraph;
    }

    private void scrollCaretToCenter() {
        int currentParagraph = textArea.getCurrentParagraph();
        // Show the current paragraph near ~40% from top
        int targetParagraph = Math.max(0, currentParagraph - 5);
        textArea.showParagraphAtTop(targetParagraph);
    }

    static String extractBacklinkAt(String text, int pos) {
        // Search backwards for [[ and forwards for ]]
        int start = text.lastIndexOf("[[", pos);
        if (start < 0) return null;
        int end = text.indexOf("]]", Math.max(start + 2, pos - 1));
        if (end < 0) return null;
        // Ensure position is between [[ and ]]
        if (pos < start || pos > end + 2) return null;
        String link = text.substring(start + 2, end).trim();
        return link.isEmpty() ? null : link;
    }
}
