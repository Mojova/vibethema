package com.vibethema.ui.util;

import com.vibethema.model.ExaltType;
import javafx.scene.Node;

/** Utility to manage and apply layered themes to the application UI. */
public class ThemeManager {

    private static final String BASE_PREFIX = "theme-";

    public static void applyThemes(Node node, String baseTheme, ExaltType exaltType) {
        if (node == null) return;

        // Remove existing theme classes
        node.getStyleClass().removeIf(s -> s.startsWith(BASE_PREFIX));

        // Apply Base Theme
        if (baseTheme != null) {
            node.getStyleClass().add(BASE_PREFIX + baseTheme.toLowerCase());
        } else {
            node.getStyleClass().add(BASE_PREFIX + "dark"); // Default
        }

        // Apply Exalt Type Theme
        if (exaltType != null) {
            node.getStyleClass().add(BASE_PREFIX + exaltType.name().toLowerCase().replace('_', '-'));
        }
    }
}
