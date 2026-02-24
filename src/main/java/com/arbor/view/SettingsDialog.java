package com.arbor.view;

import com.arbor.App;
import com.arbor.model.ArborConfig;
import com.arbor.service.ConfigService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

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

        // Daily Notes section
        Label dailyNotesLabel = new Label("Daily Notes");
        dailyNotesLabel.getStyleClass().add("settings-section-label");

        Label dailyNotesFolderLabel = new Label("Folder name:");
        dailyNotesFolderLabel.getStyleClass().add("settings-value");

        TextField dailyNotesFolderField = new TextField(config.getDailyNotesFolder());
        dailyNotesFolderField.setPrefWidth(200);
        dailyNotesFolderField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                String value = dailyNotesFolderField.getText().trim();
                if (!value.isEmpty()) {
                    config.setDailyNotesFolder(value);
                    configService.save();
                }
            }
        });

        HBox dailyNotesRow = new HBox(8, dailyNotesFolderLabel, dailyNotesFolderField);
        dailyNotesRow.setAlignment(Pos.CENTER_LEFT);

        Separator sepDailyNotes = new Separator();

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

        Label version = new Label("Arbor v" + App.VERSION);
        version.getStyleClass().add("settings-value");

        Label tagline = new Label("A place where your ideas take root.");
        tagline.getStyleClass().add("settings-value-muted");

        root.getChildren().addAll(heading,
                appearanceLabel, themeButtons, sepAppearance,
                dailyNotesLabel, dailyNotesRow, sepDailyNotes,
                groveLabel, grovePathLabel, switchGroveBtn, sep2,
                aboutLabel, version, tagline);

        Scene scene = new Scene(root, 440, 500);
        var cssUrl = getClass().getResource("/css/arbor.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        setScene(scene);
    }
}
