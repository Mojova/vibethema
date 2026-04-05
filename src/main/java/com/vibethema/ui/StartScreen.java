package com.vibethema.ui;

import com.google.gson.Gson;
import com.vibethema.model.CharacterData;
import com.vibethema.model.CharacterSaveState;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileReader;

public class StartScreen extends StackPane {

    public StartScreen() {
        getStyleClass().add("start-screen");

        VBox card = new VBox(15); // Add spacing between title and subtitle
        card.getStyleClass().add("start-card");
        card.setMaxSize(800, 500); // Taller card for the extra spacing
        card.setAlignment(Pos.CENTER); // Centralize children

        Label title = new Label("VIBETHEMA");
        title.getStyleClass().add("start-title");

        Label subtitle = new Label("Exalted 3rd Edition Character Builder");
        subtitle.getStyleClass().add("start-subtitle");

        // Spacer between text and buttons
        Region spacer = new Region();
        spacer.setPrefHeight(60);

        HBox buttonBox = new HBox(30);
        buttonBox.setAlignment(Pos.CENTER);

        Button newBtn = new Button("Create New Character");
        newBtn.getStyleClass().add("start-button");
        newBtn.setPrefWidth(280);
        newBtn.setOnAction(e -> startNewCharacter());

        Button loadBtn = new Button("Load Existing Character");
        loadBtn.getStyleClass().add("start-button-secondary");
        loadBtn.setPrefWidth(280);
        loadBtn.setOnAction(e -> loadCharacter());

        Button importBtn = new Button("Import Core PDF");
        importBtn.getStyleClass().add("start-button-secondary");
        importBtn.setPrefWidth(280);
        importBtn.setOnAction(e -> PdfImportHelper.importCorePdf(getScene().getWindow(), null));

        buttonBox.getChildren().addAll(newBtn, loadBtn);

        card.getChildren().addAll(title, subtitle, spacer, buttonBox, importBtn);
        getChildren().add(card);
    }

    private void startNewCharacter() {
        CharacterData data = new CharacterData();
        BuilderUI builder = new BuilderUI(data);
        getScene().setRoot(builder);
    }

    private void loadCharacter() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Character");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Vibethema Save File", "*.vbtm"));

        File file = fileChooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            try (FileReader reader = new FileReader(file)) {
                CharacterSaveState state = new Gson().fromJson(reader, CharacterSaveState.class);
                if (state != null) {
                    CharacterData data = new CharacterData();
                    data.importState(state, new com.vibethema.service.EquipmentDataService());
                    BuilderUI builder = new BuilderUI(data, file);
                    getScene().setRoot(builder);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
