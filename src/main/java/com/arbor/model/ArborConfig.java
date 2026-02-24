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
    private List<String> openTabs = new ArrayList<>();
    private int selectedTabIndex = 0;
    private String dailyNotesFolder = "Journal";
    private boolean focusModeEnabled = false;
    private boolean typewriterModeEnabled = false;
    private boolean showTagsInTree = true;

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

    public List<String> getOpenTabs() {
        return openTabs;
    }

    public void setOpenTabs(List<String> openTabs) {
        this.openTabs = openTabs;
    }

    public int getSelectedTabIndex() {
        return selectedTabIndex;
    }

    public void setSelectedTabIndex(int selectedTabIndex) {
        this.selectedTabIndex = selectedTabIndex;
    }

    public String getDailyNotesFolder() {
        return dailyNotesFolder;
    }

    public void setDailyNotesFolder(String dailyNotesFolder) {
        this.dailyNotesFolder = dailyNotesFolder;
    }

    public boolean isFocusModeEnabled() {
        return focusModeEnabled;
    }

    public void setFocusModeEnabled(boolean focusModeEnabled) {
        this.focusModeEnabled = focusModeEnabled;
    }

    public boolean isTypewriterModeEnabled() {
        return typewriterModeEnabled;
    }

    public void setTypewriterModeEnabled(boolean typewriterModeEnabled) {
        this.typewriterModeEnabled = typewriterModeEnabled;
    }

    public boolean isShowTagsInTree() {
        return showTagsInTree;
    }

    public void setShowTagsInTree(boolean showTagsInTree) {
        this.showTagsInTree = showTagsInTree;
    }

    public void addRecentGrove(Path grovePath) {
        recentGroves.remove(grovePath);
        recentGroves.addFirst(grovePath);
        if (recentGroves.size() > 10) {
            recentGroves = new ArrayList<>(recentGroves.subList(0, 10));
        }
    }
}
