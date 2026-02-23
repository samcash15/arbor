package com.arbor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class SearchService {
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    public List<Path> searchByName(Path root, String query) {
        List<Path> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String name = dir.getFileName().toString();
                    if (name.equals(".arbor") || name.startsWith(".")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (!dir.equals(root) && name.toLowerCase().contains(lowerQuery)) {
                        results.add(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.getFileName().toString().toLowerCase().contains(lowerQuery)) {
                        results.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Search failed", e);
        }
        return results;
    }

    public List<SearchResult> searchByContent(Path root, String query) {
        List<SearchResult> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String name = dir.getFileName().toString();
                    if (name.equals(".arbor") || name.startsWith(".")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        String content = Files.readString(file);
                        if (content.toLowerCase().contains(lowerQuery)) {
                            results.add(new SearchResult(file, file.getFileName().toString()));
                        }
                    } catch (IOException e) {
                        // Skip binary/unreadable files
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Content search failed", e);
        }
        return results;
    }

    public record SearchResult(Path path, String displayName) {
    }
}
