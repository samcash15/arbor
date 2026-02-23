package com.arbor.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class WelcomeView extends ScrollPane {

    public WelcomeView() {
        getStyleClass().add("welcome-view");
        setFitToWidth(true);
        setFitToHeight(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);

        VBox content = new VBox(16);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(32, 60, 32, 60));
        content.getStyleClass().add("welcome-content");
        content.setMaxWidth(560);

        // Heading
        Label heading = new Label("Welcome to Arbor");
        heading.getStyleClass().add("label-heading");

        // Subheading
        Label subheading = new Label("A place where your ideas take root.");
        subheading.getStyleClass().add("label-subheading");

        // Tree illustration using tree.png
        ImageView treeImage = new ImageView(
                new Image(getClass().getResourceAsStream("/images/tree.png")));
        treeImage.setFitHeight(72);
        treeImage.setPreserveRatio(true);
        VBox illustrationBox = new VBox(treeImage);
        illustrationBox.setAlignment(Pos.CENTER);
        illustrationBox.setPadding(new Insets(8, 0, 8, 0));

        // Feature list (vertical, icon left, text right)
        VBox featureList = new VBox(4);
        featureList.setAlignment(Pos.CENTER_LEFT);
        featureList.setPadding(new Insets(8, 0, 8, 0));

        featureList.getChildren().addAll(
                createImageFeatureRow("/images/folder.png", "Organize Your Notes",
                        "Create folders and files to structure your thoughts."),
                createImageFeatureRow("/images/writeedit.png", "Write & Edit",
                        "Use our simple markdown editor to capture your ideas."),
                createImageFeatureRow("/images/tree.png", "Grow Your Knowledge",
                        "Build your own personal knowledge tree.")
        );

        // Get started prompt
        Label getStarted = new Label("Get Started by creating your first note!");
        getStarted.getStyleClass().add("welcome-get-started");

        content.getChildren().addAll(heading, subheading, illustrationBox, featureList, getStarted);

        // Wrap in a centering container
        VBox wrapper = new VBox(content);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.getStyleClass().add("welcome-content");
        setContent(wrapper);
    }

    private VBox createImageFeatureRow(String imagePath, String title, String description) {
        VBox row = new VBox(2);
        row.getStyleClass().add("feature-row");

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(imagePath)));
        icon.setFitHeight(18);
        icon.setPreserveRatio(true);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("feature-card-title");

        titleRow.getChildren().addAll(icon, titleLabel);

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("feature-card-desc");
        descLabel.setWrapText(true);
        descLabel.setPadding(new Insets(0, 0, 0, 42));

        row.getChildren().addAll(titleRow, descLabel);
        return row;
    }

    private VBox createFeatureRow(String icon, String title, String description) {
        VBox row = new VBox(2);
        row.getStyleClass().add("feature-row");

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Text iconText = new Text(icon);
        iconText.getStyleClass().add("feature-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("feature-card-title");

        titleRow.getChildren().addAll(iconText, titleLabel);

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("feature-card-desc");
        descLabel.setWrapText(true);
        descLabel.setPadding(new Insets(0, 0, 0, 42));

        row.getChildren().addAll(titleRow, descLabel);
        return row;
    }
}
