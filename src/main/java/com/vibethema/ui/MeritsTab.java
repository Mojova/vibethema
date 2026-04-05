package com.vibethema.ui;

import com.vibethema.model.Merit;
import com.vibethema.viewmodel.MeritsViewModel;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.collections.ListChangeListener;

import java.net.URL;
import java.util.ResourceBundle;

public class MeritsTab extends ScrollPane implements JavaView<MeritsViewModel>, Initializable {

    @InjectViewModel
    private MeritsViewModel viewModel;

    private VBox meritsList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setFitToWidth(true);
        getStyleClass().add("scroll-pane-custom");

        VBox content = new VBox(20);
        content.getStyleClass().add("content-area");
        content.setPadding(new Insets(20));

        Label title = new Label("Merits");
        title.getStyleClass().add("section-title");

        meritsList = new VBox(12);
        meritsList.getStyleClass().add("merits-list");

        refreshMerits();
        viewModel.getMerits().addListener((ListChangeListener<Merit>) c -> refreshMerits());

        Button addBtn = new Button("+ Add Merit");
        addBtn.getStyleClass().add("action-btn");
        addBtn.setOnAction(e -> viewModel.addMerit());

        content.getChildren().addAll(title, meritsList, addBtn);
        setContent(content);
    }

    private void refreshMerits() {
        if (meritsList == null) return;
        meritsList.getChildren().clear();

        for (Merit merit : viewModel.getMerits()) {
            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("merit-row");
            row.setPadding(new Insets(10));

            TextField nameField = new TextField();
            nameField.setPromptText("Merit Name (e.g. Artifact)");
            nameField.textProperty().bindBidirectional(merit.nameProperty());
            nameField.setPrefWidth(250);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            DotSelector selector = new DotSelector(merit.ratingProperty(), 1, 5);

            Button removeBtn = new Button("✕");
            removeBtn.getStyleClass().add("remove-btn");
            removeBtn.setOnAction(e -> viewModel.removeMerit(merit));

            row.getChildren().addAll(nameField, spacer, selector, removeBtn);
            meritsList.getChildren().add(row);
        }
    }
}
