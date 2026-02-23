package com.arbor.view;

import com.arbor.model.ArborConfig;
import com.arbor.service.ConfigService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class SettingsDialog extends Stage {

    public SettingsDialog(Stage owner, ConfigService configService, Runnable onSwitchGrove,
                          Runnable onThemeChange) {
        initModality(Modality.WINDOW_MODAL);
        initOwner(owner);
        setTitle("Settings");

        ArborConfig config = configService.getConfig();

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.TOP_LEFT);
        root.getStyleClass().add("settings-dialog");

        Label heading = new Label("Settings");
        heading.getStyleClass().add("settings-heading");

        // Appearance section
        Label appearanceLabel = new Label("Appearance");
        appearanceLabel.getStyleClass().add("settings-section-label");

        ToggleGroup themeGroup = new ToggleGroup();

        ToggleButton lightBtn = new ToggleButton("Light");
        lightBtn.setToggleGroup(themeGroup);
        lightBtn.getStyleClass().add("theme-toggle");

        ToggleButton darkBtn = new ToggleButton("Dark");
        darkBtn.setToggleGroup(themeGroup);
        darkBtn.getStyleClass().add("theme-toggle");

        if ("dark".equals(config.getTheme())) {
            darkBtn.setSelected(true);
        } else {
            lightBtn.setSelected(true);
        }

        // Prevent deselecting all toggles
        themeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true);
            }
        });

        lightBtn.setOnAction(e -> {
            config.setTheme("light");
            configService.save();
            onThemeChange.run();
        });

        darkBtn.setOnAction(e -> {
            config.setTheme("dark");
            configService.save();
            onThemeChange.run();
        });

        HBox themeButtons = new HBox(8, lightBtn, darkBtn);
        themeButtons.setAlignment(Pos.CENTER_LEFT);

        Separator sepAppearance = new Separator();

        // Window section
        Label windowLabel = new Label("Window");
        windowLabel.getStyleClass().add("settings-section-label");

        Button minimizeBtn = new Button("Minimize");
        minimizeBtn.getStyleClass().add("settings-action-button");
        minimizeBtn.setOnAction(e -> {
            close();
            owner.setIconified(true);
        });

        Button maximizeBtn = new Button("Maximize / Restore");
        maximizeBtn.getStyleClass().add("settings-action-button");
        maximizeBtn.setOnAction(e -> {
            close();
            owner.setMaximized(!owner.isMaximized());
        });

        Button closeAppBtn = new Button("Close Arbor");
        closeAppBtn.getStyleClass().addAll("settings-action-button", "settings-close-button");
        closeAppBtn.setOnAction(e -> {
            close();
            owner.fireEvent(new WindowEvent(owner, WindowEvent.WINDOW_CLOSE_REQUEST));
        });

        HBox windowButtons = new HBox(8, minimizeBtn, maximizeBtn, closeAppBtn);
        windowButtons.setAlignment(Pos.CENTER_LEFT);

        Separator sep1 = new Separator();

        // Grove section
        Label groveLabel = new Label("Current Grove");
        groveLabel.getStyleClass().add("settings-section-label");

        String grovePath = config.getLastGrovePath() != null
                ? config.getLastGrovePath().toString() : "None";
        Label grovePathLabel = new Label(grovePath);
        grovePathLabel.getStyleClass().add("settings-value");
        grovePathLabel.setWrapText(true);

        Button switchGroveBtn = new Button("Switch Grove");
        switchGroveBtn.setOnAction(e -> {
            close();
            onSwitchGrove.run();
        });

        Separator sep2 = new Separator();

        // About section
        Label aboutLabel = new Label("About");
        aboutLabel.getStyleClass().add("settings-section-label");

        Label version = new Label("Arbor v0.1.0");
        version.getStyleClass().add("settings-value");

        Label tagline = new Label("A place where your ideas take root.");
        tagline.getStyleClass().add("settings-value-muted");

        root.getChildren().addAll(heading,
                appearanceLabel, themeButtons, sepAppearance,
                windowLabel, windowButtons, sep1,
                groveLabel, grovePathLabel, switchGroveBtn, sep2,
                aboutLabel, version, tagline);

        Scene scene = new Scene(root, 440, 480);
        var cssUrl = getClass().getResource("/css/arbor.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        setScene(scene);
    }
}
