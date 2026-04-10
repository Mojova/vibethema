package com.vibethema.ui.util;

import com.vibethema.model.Caste;
import javafx.scene.Node;

/**
 * Utility to manage and apply layered themes to the application UI.
 */
public class ThemeManager {

    private static final String BASE_PREFIX = "theme-";

    public static void applyThemes(Node node, String baseTheme, Caste caste) {
        if (node == null) return;
        
        // Remove existing theme classes
        node.getStyleClass().removeIf(s -> s.startsWith(BASE_PREFIX));

        // Apply Base Theme
        if (baseTheme != null) {
            node.getStyleClass().add(BASE_PREFIX + baseTheme.toLowerCase());
        } else {
            node.getStyleClass().add(BASE_PREFIX + "dark"); // Default
        }

        // Apply Character Theme
        if (caste != null && caste != Caste.NONE) {
            // Mapping Solar castes to a general "Solar" theme for now
            String exaltType = getExaltType(caste);
            node.getStyleClass().add(BASE_PREFIX + exaltType.toLowerCase());
        }
    }

    private static String getExaltType(Caste caste) {
        // For now, all castes in the current enum are Solar.
        // As more exalts are added, this mapping will expand.
        switch (caste) {
            case DAWN:
            case ZENITH:
            case TWILIGHT:
            case NIGHT:
            case ECLIPSE:
                return "solar";
            case NONE:
            default:
                return "default";
        }
    }
}
