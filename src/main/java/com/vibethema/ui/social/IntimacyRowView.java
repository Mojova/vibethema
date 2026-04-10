package com.vibethema.ui.social;

import com.vibethema.model.social.Intimacy;
import com.vibethema.viewmodel.social.IntimacyRowViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** UI component for a single Intimacy row, bound to an IntimacyRowViewModel. */
public class IntimacyRowView extends HBox {
    private final Button removeBtn;

    public IntimacyRowView(IntimacyRowViewModel viewModel) {
        setSpacing(15);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("merit-row");
        setPadding(new Insets(10));

        TextField nameField = new TextField();
        nameField.setPromptText("Intimacy Name (e.g. \"Love for my Sister\")");
        nameField.textProperty().bindBidirectional(viewModel.nameProperty());
        HBox.setHgrow(nameField, Priority.ALWAYS);

        ComboBox<Intimacy.Type> typeBox = new ComboBox<>();
        typeBox.getItems().setAll(Intimacy.Type.values());
        typeBox.valueProperty().bindBidirectional(viewModel.typeProperty());
        typeBox.setPrefWidth(100);

        ComboBox<Intimacy.Intensity> intensityBox = new ComboBox<>();
        intensityBox.getItems().setAll(Intimacy.Intensity.values());
        intensityBox.valueProperty().bindBidirectional(viewModel.intensityProperty());
        intensityBox.setPrefWidth(120);

        removeBtn = new Button("✕");
        removeBtn.getStyleClass().add("remove-btn");

        getChildren().addAll(nameField, typeBox, intensityBox, removeBtn);
    }

    public void setOnRemove(javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        removeBtn.setOnAction(handler);
    }
}
