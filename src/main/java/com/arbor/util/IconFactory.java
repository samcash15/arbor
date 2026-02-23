package com.arbor.util;

import javafx.scene.Node;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;

import java.util.Map;

public final class IconFactory {

    private static final Image LEAF_IMAGE = new Image(
            IconFactory.class.getResourceAsStream("/images/leaf.png"));
    private static final Image TREE_IMAGE = new Image(
            IconFactory.class.getResourceAsStream("/images/tree.png"));

    // Map of file extension to {hue, saturation} adjustments
    private static final Map<String, double[]> HUE_MAP = Map.ofEntries(
            Map.entry("java",  new double[]{-0.26, 0.0}),
            Map.entry("js",    new double[]{-0.19, 0.0}),
            Map.entry("jsx",   new double[]{-0.19, 0.0}),
            Map.entry("mjs",   new double[]{-0.19, 0.0}),
            Map.entry("ts",    new double[]{0.26, 0.0}),
            Map.entry("tsx",   new double[]{0.26, 0.0}),
            Map.entry("py",    new double[]{0.26, 0.0}),
            Map.entry("pyw",   new double[]{0.26, 0.0}),
            Map.entry("json",  new double[]{-0.19, -0.4}),
            Map.entry("html",  new double[]{-0.30, 0.0}),
            Map.entry("htm",   new double[]{-0.30, 0.0}),
            Map.entry("xhtml", new double[]{-0.30, 0.0}),
            Map.entry("xml",   new double[]{-0.30, 0.0}),
            Map.entry("fxml",  new double[]{-0.30, 0.0}),
            Map.entry("svg",   new double[]{-0.30, 0.0}),
            Map.entry("css",   new double[]{0.30, 0.0}),
            Map.entry("scss",  new double[]{0.30, 0.0}),
            Map.entry("less",  new double[]{0.30, 0.0}),
            Map.entry("md",    new double[]{0.0, 0.0})
    );

    private IconFactory() {
    }

    public static Node folderIcon() {
        ImageView icon = new ImageView(TREE_IMAGE);
        icon.setFitHeight(16);
        icon.setPreserveRatio(true);
        return icon;
    }

    public static Node fileIcon() {
        ImageView icon = new ImageView(LEAF_IMAGE);
        icon.setFitHeight(16);
        icon.setPreserveRatio(true);
        return icon;
    }

    public static Node fileIcon(String extension) {
        ImageView icon = new ImageView(LEAF_IMAGE);
        icon.setFitHeight(16);
        icon.setPreserveRatio(true);

        if (extension != null) {
            double[] adjust = HUE_MAP.get(extension.toLowerCase());
            if (adjust != null && (adjust[0] != 0.0 || adjust[1] != 0.0)) {
                ColorAdjust colorAdjust = new ColorAdjust();
                colorAdjust.setHue(adjust[0]);
                colorAdjust.setSaturation(adjust[1]);
                icon.setEffect(colorAdjust);
            }
        }

        return icon;
    }

    public static Node searchIcon() {
        Text icon = new Text("\uD83D\uDD0D");
        icon.setStyle("-fx-font-size: 14px;");
        return icon;
    }

    public static Node treeIcon() {
        Text icon = new Text("\uD83C\uDF33");
        icon.setStyle("-fx-font-size: 24px;");
        return icon;
    }
}
