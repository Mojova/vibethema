package com.vibethema.ui.util;

import com.vibethema.model.mystic.*;
import com.vibethema.service.CharmDataService;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Common UI utilities for the Vibethema builder. */
public class UIUtils {

    /**
     * Creates a titled VBox section with specific styling.
     *
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

    /**
     * Shows a modal dialog for selecting multiple keywords from the database.
     *
     * @param owner The owner window for modality
     * @param dataService The service to load/save keywords
     * @param current The list of currently selected keyword names
     * @param onResult Callback for the final selection
     */
    public static void showKeywordSelectionDialog(
            Window owner,
            CharmDataService dataService,
            List<String> current,
            java.util.function.Consumer<List<String>> onResult) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle("Select Keywords");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #1e1e1e;");

        Label title = new Label("Choose Keywords:");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        List<Keyword> allKeywords = dataService.loadKeywords();
        VBox listContainer = new VBox(8);
        Map<String, CheckBox> checkBoxes = new HashMap<>();

        for (Keyword k : allKeywords) {
            CheckBox cb = new CheckBox(k.getName());
            cb.setStyle("-fx-text-fill: #f9f6e6;");
            if (current.contains(k.getName())) cb.setSelected(true);
            checkBoxes.put(k.getName(), cb);
            listContainer.getChildren().add(cb);
        }

        ScrollPane sp = new ScrollPane(listContainer);
        sp.setPrefHeight(300);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("scroll-pane-custom");

        Separator sep = new Separator();

        HBox customBox = new HBox(5);
        TextField customField = new TextField();
        customField.setPromptText("Custom keyword name...");
        HBox.setHgrow(customField, Priority.ALWAYS);
        Button addBtn = new Button("Add");
        addBtn.getStyleClass().add("secondary-btn");
        addBtn.setOnAction(
                e -> {
                    String name = customField.getText().trim();
                    if (!name.isEmpty() && !checkBoxes.containsKey(name)) {
                        try {
                            dataService.saveCustomKeyword(name);
                            CheckBox cb = new CheckBox(name);
                            cb.setStyle("-fx-text-fill: #f9f6e6;");
                            cb.setSelected(true);
                            checkBoxes.put(name, cb);
                            listContainer.getChildren().add(cb);
                            customField.clear();
                        } catch (IOException ex) {
                            new Alert(
                                            Alert.AlertType.ERROR,
                                            "Could not save custom keyword: " + ex.getMessage())
                                    .showAndWait();
                        }
                    }
                });
        customBox.getChildren().addAll(customField, addBtn);

        Button okBtn = new Button("Confirm Selection");
        okBtn.getStyleClass().add("action-btn");
        okBtn.setMaxWidth(Double.MAX_VALUE);
        okBtn.setOnAction(
                e -> {
                    List<String> selected =
                            checkBoxes.entrySet().stream()
                                    .filter(entry -> entry.getValue().isSelected())
                                    .map(Map.Entry::getKey)
                                    .collect(Collectors.toList());
                    onResult.accept(selected);
                    stage.close();
                });

        root.getChildren().addAll(title, sp, sep, new Label("Add New Keyword:"), customBox, okBtn);
        for (javafx.scene.Node n : root.getChildren())
            if (n instanceof Label l) l.setStyle("-fx-text-fill: #cccccc;");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Scene scene = new Scene(root, 400, 550);
        scene.addEventHandler(
                javafx.scene.input.KeyEvent.KEY_PRESSED,
                e -> {
                    if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                        stage.close();
                    }
                });
        if (owner.getScene() != null)
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        stage.setScene(scene);
        stage.show();
    }
}
