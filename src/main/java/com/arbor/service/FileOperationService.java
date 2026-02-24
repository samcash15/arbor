package com.arbor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileOperationService {
    private static final Logger log = LoggerFactory.getLogger(FileOperationService.class);

    public String readFile(Path path) throws IOException {
        return Files.readString(path);
    }

    public void writeFile(Path path, String content) throws IOException {
        Files.writeString(path, content);
        log.debug("Wrote file: {}", path);
    }

    public Path createFileWithContent(Path parent, String fileName, String content) throws IOException {
        Path newFile = parent.resolve(fileName);
        Files.writeString(newFile, content);
        log.debug("Created file with content: {}", newFile);
        return newFile;
    }

    public Path createFile(Path parent, String fileName) throws IOException {
        Path newFile = parent.resolve(fileName);
        Files.createFile(newFile);
        log.debug("Created file: {}", newFile);
        return newFile;
    }

    public Path createDirectory(Path parent, String dirName) throws IOException {
        Path newDir = parent.resolve(dirName);
        Files.createDirectories(newDir);
        log.debug("Created directory: {}", newDir);
        return newDir;
    }

    public Path rename(Path path, String newName) throws IOException {
        Path target = path.getParent().resolve(newName);
        Files.move(path, target, StandardCopyOption.ATOMIC_MOVE);
        log.debug("Renamed {} to {}", path, target);
        return target;
    }

    public void delete(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var stream = Files.walk(path)) {
                stream.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                log.error("Failed to delete: {}", p, e);
                            }
                        });
            }
        } else {
            Files.deleteIfExists(path);
        }
        log.debug("Deleted: {}", path);
    }
}
