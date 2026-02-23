package com.arbor.service;

import com.arbor.model.ArborConfig;
import com.arbor.util.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigService {
    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);
    private static final Path CONFIG_DIR = Paths.get(System.getProperty("user.home"), ".arbor");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.json");

    private ArborConfig config;

    public ConfigService() {
        load();
    }

    public ArborConfig getConfig() {
        return config;
    }

    public void load() {
        if (Files.exists(CONFIG_FILE)) {
            try {
                String json = Files.readString(CONFIG_FILE);
                config = GsonFactory.gson().fromJson(json, ArborConfig.class);
                log.debug("Loaded app config from {}", CONFIG_FILE);
            } catch (IOException e) {
                log.error("Failed to load config, using defaults", e);
                config = new ArborConfig();
            }
        } else {
            config = new ArborConfig();
        }
    }

    public void save() {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
            String json = GsonFactory.gson().toJson(config);
            Files.writeString(CONFIG_FILE, json);
            log.debug("Saved app config to {}", CONFIG_FILE);
        } catch (IOException e) {
            log.error("Failed to save config", e);
        }
    }
}
