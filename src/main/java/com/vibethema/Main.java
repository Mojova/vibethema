package com.vibethema;

import com.vibethema.model.CharacterData;
import com.vibethema.model.CharacterSaveState;
import com.vibethema.ui.MainView;
import com.vibethema.ui.StartScreen;
import com.vibethema.viewmodel.MainViewModel;
import com.google.gson.Gson;
import de.saxsys.mvvmfx.FluentViewLoader;
import de.saxsys.mvvmfx.ViewTuple;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;

public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static File pendingFile;

    public static void setPendingFile(File file) {
        pendingFile = file;
    }

    @Override
    public void start(Stage primaryStage) {
        if (pendingFile != null) {
            loadAndStart(primaryStage, pendingFile);
        } else {
            StartScreen root = new StartScreen();
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

            primaryStage.setTitle("Vibethema");
            primaryStage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/icon.png")));
            primaryStage.setScene(scene);
            primaryStage.setOnCloseRequest(e -> {
                if (scene.getRoot() instanceof MainView view) {
                    if (!view.confirmDiscardChanges())
                        e.consume();
                }
            });
            primaryStage.show();
        }
    }

    private void loadAndStart(Stage stage, File file) {
        try (FileReader reader = new FileReader(file)) {
            CharacterSaveState state = new Gson().fromJson(reader, CharacterSaveState.class);
            CharacterData data = new CharacterData();
            data.importState(state, new com.vibethema.service.EquipmentDataService());

            ViewTuple<MainView, MainViewModel> viewTuple = FluentViewLoader.javaView(MainView.class).load();
            MainView view = (MainView) viewTuple.getView();
            MainViewModel vm = viewTuple.getViewModel();
            vm.init(data);
            vm.currentFileProperty().set(file);
            data.setDirty(false);

            Scene scene = new Scene(view, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

            stage.setTitle("Vibethema - " + file.getName());
            stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/icon.png")));
            stage.setScene(scene);
            stage.setOnCloseRequest(e -> {
                if (view.confirmDiscardChanges()) {
                    view.cleanup();
                } else {
                    e.consume();
                }
            });
            stage.show();
        } catch (Exception e) {
            logger.error("Failed to load character file: {}", file.getName(), e);
            // If load fails, fall back to start screen
            pendingFile = null;
            start(stage);
        }
    }

    public static void main(String[] args) {
        System.setProperty("apple.awt.application.name", "Vibethema");
        launch(args);
    }
}
