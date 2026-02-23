package com.arbor.model;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

public class Grove {
    private static final String CONFIG_DIR_NAME = ".arbor";
    private static final String CONFIG_FILE_NAME = "grove.json";

    private final UUID uuid;
    private String name;
    private final LocalDateTime createdAt;
    private final Path rootPath;

    public Grove(Path rootPath, String name) {
        this.rootPath = rootPath;
        this.createdAt = LocalDateTime.now();
        this.name = name;
        this.uuid = UUID.randomUUID();
    }

    /** Deserialization constructor */
    public Grove(UUID uuid, String name, LocalDateTime createdAt, Path rootPath) {
        this.uuid = uuid;
        this.name = name;
        this.createdAt = createdAt;
        this.rootPath = rootPath;
    }

    public Path getConfigDir() {
        return rootPath.resolve(CONFIG_DIR_NAME);
    }

    public Path getConfigFile() {
        return getConfigDir().resolve(CONFIG_FILE_NAME);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Path getRootPath() {
        return rootPath;
    }

    @Override
    public String toString() {
        return "Grove{" +
                "uuid=" + uuid +
                ", name='" + name + '\'' +
                ", createdAt=" + createdAt +
                ", rootPath=" + rootPath +
                '}';
    }
}
