package com.vibethema.ui;

import com.vibethema.model.Intimacy;
import com.vibethema.viewmodel.IntimaciesViewModel;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import javafx.collections.ListChangeListener;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class IntimaciesTab extends ScrollPane implements JavaView<IntimaciesViewModel>, Initializable {

    @InjectViewModel
    private IntimaciesViewModel viewModel;

    private VBox intimaciesList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setFitToWidth(true);
        getStyleClass().add("scroll-pane-custom");

        VBox content = new VBox(20);
        content.getStyleClass().add("content-area");
        content.setPadding(new Insets(20));

        Label title = new Label("Intimacies");
        title.getStyleClass().add("section-title");

        intimaciesList = new VBox(12);
        intimaciesList.getStyleClass().add("merit-row-container");

        refreshIntimacies();
        viewModel.getIntimacies().addListener((ListChangeListener<Intimacy>) c -> refreshIntimacies());

        Button addBtn = new Button("+ Add Intimacy");
        addBtn.getStyleClass().add("action-btn");
        addBtn.setOnAction(e -> viewModel.addIntimacy());

        content.getChildren().addAll(title, intimaciesList, addBtn);
        setContent(content);
    }

    private void refreshIntimacies() {
        if (intimaciesList == null) return;
        intimaciesList.getChildren().clear();

        for (Intimacy intimacy : viewModel.getIntimacies()) {
            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("merit-row");
            row.setPadding(new Insets(10));

            TextField nameField = new TextField();
            nameField.setPromptText("Intimacy Name (e.g. \"Love for my Sister\")");
            nameField.textProperty().bindBidirectional(intimacy.nameProperty());
            HBox.setHgrow(nameField, Priority.ALWAYS);

            ComboBox<Intimacy.Type> typeBox = new ComboBox<>();
            typeBox.getItems().setAll(Intimacy.Type.values());
            typeBox.valueProperty().bindBidirectional(intimacy.typeProperty());
            typeBox.setPrefWidth(100);

            ComboBox<Intimacy.Intensity> intensityBox = new ComboBox<>();
            intensityBox.getItems().setAll(Intimacy.Intensity.values());
            intensityBox.valueProperty().bindBidirectional(intimacy.intensityProperty());
            intensityBox.setPrefWidth(120);

            Button removeBtn = new Button("✕");
            removeBtn.getStyleClass().add("remove-btn");
            removeBtn.setOnAction(e -> viewModel.removeIntimacy(intimacy));

            row.getChildren().addAll(nameField, typeBox, intensityBox, removeBtn);
            intimaciesList.getChildren().add(row);
        }
    }
}
