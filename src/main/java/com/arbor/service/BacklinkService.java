package com.arbor.service;

import com.arbor.model.BacklinkEntry;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BacklinkService {

    public static final Pattern BACKLINK_PATTERN = Pattern.compile("\\[\\[([^\\]]+)]]");

    private Path grovePath;
    private final Map<Path, List<BacklinkEntry>> forwardIndex = new ConcurrentHashMap<>();
    private final Map<String, List<BacklinkEntry>> reverseIndex = new ConcurrentHashMap<>();

    public void setGrovePath(Path grovePath) {
        this.grovePath = grovePath;
    }

    public void fullScan() {
        if (grovePath == null) return;
        forwardIndex.clear();
        reverseIndex.clear();

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
        } catch (IOException ignored) {
        }
    }

    public void rescanFile(Path file) {
        // Remove old entries for this file
        List<BacklinkEntry> oldEntries = forwardIndex.remove(file);
        if (oldEntries != null) {
            for (BacklinkEntry entry : oldEntries) {
                String key = normalizeTarget(entry.linkTarget());
                List<BacklinkEntry> reverseList = reverseIndex.get(key);
                if (reverseList != null) {
                    reverseList.removeIf(e -> e.sourcePath().equals(file));
                    if (reverseList.isEmpty()) {
                        reverseIndex.remove(key);
                    }
                }
            }
        }
        // Re-scan
        scanFile(file);
    }

    private void scanFile(Path file) {
        try {
            String content = Files.readString(file);
            List<BacklinkEntry> entries = new ArrayList<>();
            String[] lines = content.split("\n", -1);

            for (int i = 0; i < lines.length; i++) {
                Matcher matcher = BACKLINK_PATTERN.matcher(lines[i]);
                while (matcher.find()) {
                    String target = matcher.group(1).trim();
                    BacklinkEntry entry = new BacklinkEntry(file, target, i);
                    entries.add(entry);
                }
            }

            if (!entries.isEmpty()) {
                forwardIndex.put(file, entries);
                for (BacklinkEntry entry : entries) {
                    String key = normalizeTarget(entry.linkTarget());
                    reverseIndex.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()))
                            .add(entry);
                }
            }
        } catch (IOException ignored) {
        }
    }

    public List<BacklinkEntry> getBacklinksTo(Path file) {
        String filename = file.getFileName().toString();
        String nameNoExt = removeExtension(filename);

        List<BacklinkEntry> results = new ArrayList<>();

        // Check both with and without extension
        List<BacklinkEntry> exact = reverseIndex.get(normalizeTarget(filename));
        if (exact != null) results.addAll(exact);

        List<BacklinkEntry> noExt = reverseIndex.get(normalizeTarget(nameNoExt));
        if (noExt != null) {
            for (BacklinkEntry e : noExt) {
                if (!results.contains(e)) results.add(e);
            }
        }

        return results;
    }

    public Path resolveLink(String linkText, Path grovePath) {
        if (grovePath == null) return null;
        String normalized = linkText.trim();

        try {
            // Exact name match
            Path[] result = new Path[1];
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
                    String filename = file.getFileName().toString();
                    String nameNoExt = removeExtension(filename);

                    if (filename.equalsIgnoreCase(normalized)) {
                        result[0] = file;
                        return FileVisitResult.TERMINATE;
                    }
                    if (nameNoExt.equalsIgnoreCase(normalized) && result[0] == null) {
                        result[0] = file;
                    }
                    if (filename.toLowerCase().contains(normalized.toLowerCase()) && result[0] == null) {
                        result[0] = file;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            return result[0];
        } catch (IOException e) {
            return null;
        }
    }

    private String normalizeTarget(String target) {
        return target.trim().toLowerCase();
    }

    private String removeExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
