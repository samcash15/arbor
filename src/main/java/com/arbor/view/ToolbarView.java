package com.arbor.view;

import com.arbor.util.IconFactory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

public class ToolbarView extends HBox {
    private final Label groveNameLabel;
    private double dragOffsetX;
    private double dragOffsetY;

    public ToolbarView(Runnable onSearch, Runnable onSwitchGrove, Runnable onSettings, Runnable onOutlineToggle) {
        getStyleClass().add("toolbar");
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(0, 12, 0, 16));
        setSpacing(10);

        // Left: branding with tree.png logo
        ImageView treeLogo = new ImageView(
                new Image(getClass().getResourceAsStream("/images/tree.png")));
        treeLogo.setFitHeight(20);
        treeLogo.setPreserveRatio(true);
        Label brand = new Label("Arbor");
        brand.getStyleClass().add("toolbar-brand");
        HBox brandBox = new HBox(6, treeLogo, brand);
        brandBox.setAlignment(Pos.CENTER_LEFT);

        // Center spacer
        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);

        // Center: grove name
        groveNameLabel = new Label("");
        groveNameLabel.getStyleClass().add("toolbar-grove-name");

        // Right spacer
        Region rightSpacer = new Region();
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        // Action icons
        Label searchIcon = new Label();
        searchIcon.setGraphic(IconFactory.searchIcon());
        searchIcon.getStyleClass().add("toolbar-icon");
        searchIcon.setOnMouseClicked(e -> onSearch.run());

        ImageView gearImageView = new ImageView();
        gearImageView.setFitHeight(18);
        gearImageView.setPreserveRatio(true);
        Label gearIcon = new Label("\u2699");
        gearIcon.getStyleClass().add("toolbar-icon");
        gearIcon.setStyle("-fx-font-size: 18px;");
        gearIcon.setOnMouseEntered(e -> {
            gearIcon.setText(null);
            gearImageView.setImage(new Image(getClass().getResourceAsStream("/images/gear.gif")));
            gearIcon.setGraphic(gearImageView);
        });
        gearIcon.setOnMouseExited(e -> {
            gearIcon.setGraphic(null);
            gearImageView.setImage(null);
            gearIcon.setText("\u2699");
        });
        gearIcon.setOnMouseClicked(e -> onSettings.run());

        Label outlineIcon = new Label("\u2630");
        outlineIcon.getStyleClass().add("toolbar-icon");
        outlineIcon.setStyle("-fx-font-size: 16px;");
        outlineIcon.setOnMouseClicked(e -> onOutlineToggle.run());

        HBox appActions = new HBox(12, searchIcon, outlineIcon, gearIcon);
        appActions.setAlignment(Pos.CENTER_RIGHT);

        // Vertical divider between app actions and window controls
        Line divider = new Line(0, 0, 0, 20);
        divider.getStyleClass().add("toolbar-divider");

        // Window controls
        Label minimizeBtn = new Label("\u2013");
        minimizeBtn.getStyleClass().addAll("window-control", "window-minimize");
        minimizeBtn.setOnMouseClicked(e -> getStage().setIconified(true));

        Label maximizeBtn = new Label("\u25A1");
        maximizeBtn.getStyleClass().addAll("window-control", "window-maximize");
        maximizeBtn.setOnMouseClicked(e -> {
            Stage stage = getStage();
            stage.setMaximized(!stage.isMaximized());
        });

        Label closeBtn = new Label("\u2715");
        closeBtn.getStyleClass().addAll("window-control", "window-close");
        closeBtn.setOnMouseClicked(e -> getStage().fireEvent(
                new javafx.stage.WindowEvent(getStage(), javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST)));

        HBox windowControls = new HBox(2, minimizeBtn, maximizeBtn, closeBtn);
        windowControls.setAlignment(Pos.CENTER_RIGHT);

        HBox rightGroup = new HBox(10, appActions, divider, windowControls);
        rightGroup.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(brandBox, leftSpacer, groveNameLabel, rightSpacer, rightGroup);

        // Make toolbar draggable to move the window
        setOnMousePressed(event -> {
            dragOffsetX = event.getScreenX() - getStage().getX();
            dragOffsetY = event.getScreenY() - getStage().getY();
        });

        setOnMouseDragged(event -> {
            Stage stage = getStage();
            if (!stage.isMaximized()) {
                stage.setX(event.getScreenX() - dragOffsetX);
                stage.setY(event.getScreenY() - dragOffsetY);
            }
        });

        // Double-click toolbar to toggle maximize
        setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Stage stage = getStage();
                stage.setMaximized(!stage.isMaximized());
            }
        });
    }

    public void setGroveName(String name) {
        groveNameLabel.setText(name);
    }

    private Stage getStage() {
        return (Stage) getScene().getWindow();
    }
}
