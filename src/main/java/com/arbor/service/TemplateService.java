package com.arbor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class TemplateService {
    private static final Logger log = LoggerFactory.getLogger(TemplateService.class);

    private static final Path TEMPLATES_DIR = Path.of(System.getProperty("user.home"), ".arbor", "templates");

    public void ensureTemplatesDir() {
        try {
            Files.createDirectories(TEMPLATES_DIR);
        } catch (IOException e) {
            log.error("Failed to create templates directory", e);
        }
    }

    public void initDefaults() {
        try {
            if (listTemplates().isEmpty()) {
                Files.writeString(TEMPLATES_DIR.resolve("Blank Note.md"),
                        "# Untitled\n\n");

                Files.writeString(TEMPLATES_DIR.resolve("Meeting Notes.md"),
                        """
                        # Meeting Notes

                        **Date:** \s
                        **Attendees:**\s

                        ## Agenda

                        - \s

                        ## Discussion

                        \s

                        ## Action Items

                        - [ ] \s
                        """);

                Files.writeString(TEMPLATES_DIR.resolve("Bug Report.md"),
                        """
                        # Bug Report

                        ## Description

                        \s

                        ## Steps to Reproduce

                        1. \s
                        2. \s
                        3. \s

                        ## Expected Behavior

                        \s

                        ## Actual Behavior

                        \s

                        ## Environment

                        - OS: \s
                        - Version: \s
                        """);
            }
        } catch (IOException e) {
            log.error("Failed to write default templates", e);
        }
    }

    public List<Path> listTemplates() {
        try (Stream<Path> stream = Files.list(TEMPLATES_DIR)) {
            return stream
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".md"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            log.error("Failed to list templates", e);
            return List.of();
        }
    }

    public String readTemplate(Path templatePath) throws IOException {
        return Files.readString(templatePath);
    }

    public Path getTemplatesDir() {
        return TEMPLATES_DIR;
    }
}
