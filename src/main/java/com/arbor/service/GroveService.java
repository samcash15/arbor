package com.arbor.service;

import com.arbor.model.Grove;
import com.arbor.util.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GroveService {
    private static final Logger log = LoggerFactory.getLogger(GroveService.class);

    public Grove createGrove(Path rootPath) throws IOException {
        String name = rootPath.getFileName().toString();
        Grove grove = new Grove(rootPath, name);
        saveGrove(grove);
        return grove;
    }

    public void saveGrove(Grove grove) throws IOException {
        Path configDir = grove.getConfigDir();
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }
        String json = GsonFactory.gson().toJson(grove);
        Files.writeString(grove.getConfigFile(), json);
        log.debug("Saved grove config to {}", grove.getConfigFile());
    }

    public Grove loadGrove(Path rootPath) throws IOException {
        Path configFile = rootPath.resolve(".arbor").resolve("grove.json");
        if (!Files.exists(configFile)) {
            return null;
        }
        String json = Files.readString(configFile);
        Grove grove = GsonFactory.gson().fromJson(json, Grove.class);
        log.debug("Loaded grove from {}", configFile);
        return grove;
    }

    public Grove loadOrCreateGrove(Path rootPath) throws IOException {
        Grove grove = loadGrove(rootPath);
        if (grove == null) {
            grove = createGrove(rootPath);
        }
        return grove;
    }
}
