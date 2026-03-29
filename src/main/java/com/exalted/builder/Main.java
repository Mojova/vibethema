package com.exalted.builder;

import com.exalted.builder.model.CharacterData;
import com.exalted.builder.ui.BuilderUI;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        CharacterData data = new CharacterData();
        BuilderUI root = new BuilderUI(data);
        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        
        primaryStage.setTitle("Exalted 3 Builder");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
