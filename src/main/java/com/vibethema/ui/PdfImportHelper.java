package com.vibethema.ui;

import com.vibethema.service.PdfExtractor;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;

public class PdfImportHelper {

    public static void importCorePdf(Window owner, Runnable onComplete) {
        importPdf(owner, "Select Exalted Core 3e PDF", "", true, PdfExtractor.PdfSource.CORE, onComplete);
    }

    public static void importMosePdf(Window owner, Runnable onComplete) {
        importPdf(owner, "Select Miracles of the Solar Exalted PDF", "-mose", false, PdfExtractor.PdfSource.MOSE, onComplete);
    }

    private static void importPdf(Window owner, String title, String suffix, boolean extractKeywords, PdfExtractor.PdfSource source, Runnable onComplete) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showOpenDialog(owner);

        if (file != null) {
            showImportProgress(owner, file, suffix, extractKeywords, source, onComplete);
        }
    }

    private static void showImportProgress(Window owner, File pdfFile, String suffix, boolean extractKeywords, PdfExtractor.PdfSource source, Runnable onComplete) {
        Stage progressStage = new Stage();
        progressStage.initModality(Modality.WINDOW_MODAL);
        progressStage.initOwner(owner);
        progressStage.setTitle("Importing PDF...");

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #1e1e1e;");

        Label label = new Label("Extracting data from PDF...\nThis may take a minute.");
        label.setStyle("-fx-text-fill: #f9f6e6; -fx-text-alignment: center;");
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);

        layout.getChildren().addAll(label, progressBar);
        progressStage.setScene(new Scene(layout));
        progressStage.show();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                PdfExtractor extractor = new PdfExtractor();
                extractor.extractAll(pdfFile, suffix, extractKeywords, source, progress -> {
                    Platform.runLater(() -> progressBar.setProgress(progress));
                });
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            progressStage.close();
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Import complete! Charms, spells, and keywords have been updated.", ButtonType.OK);
            alert.showAndWait();
            if (onComplete != null) {
                onComplete.run();
            }
        });

        task.setOnFailed(e -> {
            progressStage.close();
            Throwable ex = task.getException();
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Import failed: " + ex.getMessage(), ButtonType.OK);
            alert.showAndWait();
        });

        new Thread(task).start();
    }
}
