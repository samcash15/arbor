package com.arbor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TagService {
    private static final Logger log = LoggerFactory.getLogger(TagService.class);

    public static final Pattern TAG_PATTERN = Pattern.compile("(?<=\\s|^)#([a-zA-Z][a-zA-Z0-9_-]*)");

    private Path grovePath;
    private final Map<String, Set<Path>> tagIndex = new ConcurrentHashMap<>();

    public void setGrovePath(Path grovePath) {
        this.grovePath = grovePath;
    }

    public void fullScan() {
        if (grovePath == null) return;
        tagIndex.clear();

        try {
            Files.walkFileTree(grovePath, new SimpleFileVisitor<>() {
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
                    if (!attrs.isRegularFile() || file.getFileName().toString().startsWith(".")) {
                        return FileVisitResult.CONTINUE;
                    }
                    scanFile(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Failed to scan for tags", e);
        }
    }

    public void rescanFile(Path file) {
        // Remove old entries for this file
        tagIndex.values().forEach(paths -> paths.remove(file));
        tagIndex.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        // Re-scan
        scanFile(file);
    }

    private void scanFile(Path file) {
        try {
            String content = Files.readString(file);
            Matcher matcher = TAG_PATTERN.matcher(content);
            while (matcher.find()) {
                String tag = matcher.group(1).toLowerCase();
                tagIndex.computeIfAbsent(tag, k -> ConcurrentHashMap.newKeySet()).add(file);
            }
        } catch (IOException e) {
            log.debug("Failed to scan file for tags: {}", file, e);
        }
    }

    public Set<Path> getFilesForTag(String tag) {
        Set<Path> files = tagIndex.get(tag.toLowerCase());
        return files != null ? Set.copyOf(files) : Set.of();
    }

    public List<String> searchTags(String query) {
        String lower = query.toLowerCase();
        return tagIndex.keySet().stream()
                .filter(tag -> tag.startsWith(lower))
                .sorted()
                .toList();
    }

    public Map<String, Set<Path>> getAllTags() {
        return tagIndex.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Set.copyOf(e.getValue()),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }
}
