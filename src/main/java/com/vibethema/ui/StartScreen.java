package com.vibethema.ui;

import com.vibethema.viewmodel.MainViewModel;
import com.vibethema.viewmodel.StartScreenViewModel;
import com.vibethema.viewmodel.util.Messenger;
import de.saxsys.mvvmfx.FluentViewLoader;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import de.saxsys.mvvmfx.ViewTuple;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.io.File;

/**
 * View for the initial start screen, implementing JavaView for StartScreenViewModel.
 */
public class StartScreen extends StackPane implements JavaView<StartScreenViewModel> {

    @InjectViewModel
    private StartScreenViewModel viewModel;

    public StartScreen() {
        getStyleClass().add("start-screen");

        VBox card = new VBox(15);
        card.getStyleClass().add("start-card");
        card.setMaxSize(800, 500);
        card.setAlignment(Pos.CENTER);

        Label title = new Label("VIBETHEMA");
        title.getStyleClass().add("start-title");

        Label subtitle = new Label("Exalted 3rd Edition Character Builder");
        subtitle.getStyleClass().add("start-subtitle");

        Region spacer = new Region();
        spacer.setPrefHeight(60);

        HBox buttonBox = new HBox(30);
        buttonBox.setAlignment(Pos.CENTER);

        Button newBtn = new Button("Create New Character");
        newBtn.getStyleClass().add("start-button");
        newBtn.setPrefWidth(280);
        newBtn.setOnAction(e -> viewModel.onNewCharacter());

        Button loadBtn = new Button("Load Existing Character");
        loadBtn.getStyleClass().add("start-button-secondary");
        loadBtn.setPrefWidth(280);
        loadBtn.setOnAction(e -> viewModel.onLoadCharacter());

        Button importBtn = new Button("Import Core PDF");
        importBtn.getStyleClass().add("start-button-secondary");
        importBtn.setPrefWidth(280);
        importBtn.setOnAction(e -> viewModel.onImportPdf());

        buttonBox.getChildren().addAll(newBtn, loadBtn);

        card.getChildren().addAll(title, subtitle, spacer, buttonBox, importBtn);
        getChildren().add(card);
    }

    /**
     * Initializes the view listeners and subscriptions.
     */
    public void initialize() {
        // Handle transition to main view (either new or loaded)
        Messenger.subscribe("show_main_view", (name, payload) -> {
            ViewTuple<MainView, MainViewModel> viewTuple = FluentViewLoader.javaView(MainView.class).load();
            MainView mainView = (MainView) viewTuple.getView();
            
            // If a file was provided as payload, load it into the new view model
            if (payload != null && payload.length > 0 && payload[0] instanceof File file) {
                viewTuple.getViewModel().loadCharacter(file);
            }
            
            getScene().setRoot(mainView);
        });

        // Handle file open dialog request
        Messenger.subscribe("request_load_file_start", (name, payload) -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Load Character");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Vibethema Save File", "*.vbtm"));
            File file = fileChooser.showOpenDialog(getScene().getWindow());
            viewModel.onLoadFileSelected(file);
        });

        // Handle PDF import request
        Messenger.subscribe("request_import_pdf", (name, payload) -> {
            PdfImportHelper.importCorePdf(getScene().getWindow(), null);
        });
    }
}
