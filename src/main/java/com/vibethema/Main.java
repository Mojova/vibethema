package com.vibethema;

import com.vibethema.model.CharacterData;
import com.vibethema.ui.BuilderUI;
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
        
        primaryStage.setTitle("Vibethema");
        primaryStage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/icon.png")));
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        System.setProperty("apple.awt.application.name", "Vibethema");
        launch(args);
    }
}
