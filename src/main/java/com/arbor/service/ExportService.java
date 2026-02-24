package com.arbor.service;

import javafx.print.PrinterJob;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ExportService {
    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    private static final Parser MD_PARSER = Parser.builder().build();
    private static final HtmlRenderer MD_RENDERER = HtmlRenderer.builder().build();

    public String toStyledHtml(String markdownContent, boolean darkMode) {
        String html = MD_RENDERER.render(MD_PARSER.parse(markdownContent));
        String css = loadCss(darkMode);

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                %s
                    </style>
                </head>
                <body>
                %s
                </body>
                </html>
                """.formatted(css, html);
    }

    public void exportHtml(String html, Path outputPath) throws IOException {
        Files.writeString(outputPath, html);
        log.debug("Exported HTML to: {}", outputPath);
    }

    public void exportPdf(String html, Stage owner) {
        WebView webView = new WebView();
        webView.getEngine().loadContent(html, "text/html");
        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                PrinterJob job = PrinterJob.createPrinterJob();
                if (job != null && job.showPrintDialog(owner)) {
                    webView.getEngine().print(job);
                    job.endJob();
                }
            }
        });
    }

    private String loadCss(boolean darkMode) {
        String cssFile = darkMode ? "/css/markdown-preview-dark.css" : "/css/markdown-preview.css";
        try (InputStream is = getClass().getResourceAsStream(cssFile)) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.error("Failed to load CSS for export", e);
        }
        return "";
    }
}
