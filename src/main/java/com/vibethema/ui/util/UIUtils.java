package com.vibethema.ui.util;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Common UI utilities for the Vibethema builder.
 */
public class UIUtils {
    
    /**
     * Creates a titled VBox section with specific styling.
     * @param titleText The title to display
     * @return A VBox containing the title Label
     */
    public static VBox createSection(String titleText) {
        VBox section = new VBox(10);
        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");
        section.getChildren().add(title);
        return section;
    }
}
