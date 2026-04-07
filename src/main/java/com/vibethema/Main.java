package com.vibethema;

import com.vibethema.ui.MainView;
import com.vibethema.ui.StartScreen;
import com.vibethema.viewmodel.MainViewModel;
import de.saxsys.mvvmfx.FluentViewLoader;
import de.saxsys.mvvmfx.ViewTuple;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

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
            showSplashScreen(primaryStage);
        }
    }

    private void showSplashScreen(Stage mainStage) {
        Stage splashStage = new Stage(StageStyle.UNDECORATED);
        javafx.scene.image.Image splashImg = new javafx.scene.image.Image(getClass().getResourceAsStream("/splash.jpg"));
        ImageView splashView = new ImageView(splashImg);
        splashView.setPreserveRatio(true);
        splashView.setFitWidth(800); // Standard splash width

        StackPane root = new StackPane(splashView);
        Scene splashScene = new Scene(root);
        splashStage.setScene(splashScene);
        splashStage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/icon.png")));
        splashStage.show();

        // Load content in background
        Task<Scene> loadTask = new Task<>() {
            @Override
            protected Scene call() {
                // Heavy lifting here: Load the StartScreen and CSS
                StartScreen startRoot = (StartScreen) FluentViewLoader.javaView(StartScreen.class).load().getView();
                Scene scene = new Scene(startRoot, 1200, 800);
                scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
                return scene;
            }
        };

        loadTask.setOnSucceeded(e -> {
            Scene mainScene = loadTask.getValue();
            mainStage.setTitle("Vibethema");
            mainStage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/icon.png")));
            mainStage.setScene(mainScene);
            mainStage.setOnCloseRequest(ev -> {
                if (mainScene.getRoot() instanceof MainView view) {
                    if (!view.confirmDiscardChanges())
                        ev.consume();
                }
            });

            // Instant transition
            splashStage.close();
            mainStage.show();
        });

        new Thread(loadTask).start();
    }

    private void loadAndStart(Stage stage, File file) {
        if (!new com.vibethema.service.SystemDataService().isCoreDataImported()) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Data Missing");
            alert.setHeaderText("Core Book Data Missing");
            alert.setContentText("You must import the Exalted 3rd Edition Core PDF before loading characters.");
            alert.showAndWait();
            pendingFile = null;
            start(stage);
            return;
        }
        try {
            ViewTuple<MainView, MainViewModel> viewTuple = FluentViewLoader.javaView(MainView.class).load();
            MainView view = (MainView) viewTuple.getView();
            MainViewModel vm = viewTuple.getViewModel();
            vm.loadCharacter(file);

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
