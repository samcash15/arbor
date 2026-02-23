package com.arbor.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ArborConfig {
    private Path lastGrovePath;
    private double windowWidth = 900;
    private double windowHeight = 650;
    private double dividerPosition = 0.2;
    private List<Path> recentGroves = new ArrayList<>();
    private String theme = "light";

    public ArborConfig() {
    }

    public Path getLastGrovePath() {
        return lastGrovePath;
    }

    public void setLastGrovePath(Path lastGrovePath) {
        this.lastGrovePath = lastGrovePath;
    }

    public double getWindowWidth() {
        return windowWidth;
    }

    public void setWindowWidth(double windowWidth) {
        this.windowWidth = windowWidth;
    }

    public double getWindowHeight() {
        return windowHeight;
    }

    public void setWindowHeight(double windowHeight) {
        this.windowHeight = windowHeight;
    }

    public double getDividerPosition() {
        return dividerPosition;
    }

    public void setDividerPosition(double dividerPosition) {
        this.dividerPosition = dividerPosition;
    }

    public List<Path> getRecentGroves() {
        return recentGroves;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public void addRecentGrove(Path grovePath) {
        recentGroves.remove(grovePath);
        recentGroves.addFirst(grovePath);
        if (recentGroves.size() > 10) {
            recentGroves = new ArrayList<>(recentGroves.subList(0, 10));
        }
    }
}
