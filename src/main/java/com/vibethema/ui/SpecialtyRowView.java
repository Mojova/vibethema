package com.vibethema.ui;

import com.vibethema.viewmodel.stats.SpecialtyRowViewModel;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** UI row for an ability specialty. */
public class SpecialtyRowView extends HBox {
    private final Button editBtn;
    private final Button deleteBtn;

    public SpecialtyRowView(SpecialtyRowViewModel viewModel) {
        setSpacing(12);
        setAlignment(Pos.CENTER_LEFT);

        Label specialtyLabel = new Label();
        specialtyLabel.getStyleClass().add("merit-name");
        specialtyLabel.setMinWidth(200);

        // Display as "Name (Ability)"
        specialtyLabel
                .textProperty()
                .bind(
                        Bindings.concat(
                                viewModel.nameProperty(), " (", viewModel.abilityProperty(), ")"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        editBtn = new Button("✎");
        editBtn.getStyleClass().add("remove-btn");
        editBtn.setTooltip(new Tooltip("Edit Specialty"));

        deleteBtn = new Button("✕");
        deleteBtn.getStyleClass().add("remove-btn");
        deleteBtn.setTooltip(new Tooltip("Delete Specialty"));

        getChildren().addAll(specialtyLabel, spacer, editBtn, deleteBtn);
    }

    public void setOnEdit(javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        editBtn.setOnAction(handler);
    }

    public void setOnDelete(javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        deleteBtn.setOnAction(handler);
    }

    public void setOnNameClick(
            javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> value) {
        getChildren().get(0).setCursor(javafx.scene.Cursor.HAND);
        getChildren().get(0).setOnMouseClicked(value);
    }
}
