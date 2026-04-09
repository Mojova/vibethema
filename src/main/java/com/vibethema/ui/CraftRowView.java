package com.vibethema.ui;

import com.vibethema.viewmodel.stats.CraftRowViewModel;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** UI row for a craft expertise. */
public class CraftRowView extends HBox {
    private final Button editBtn;
    private final Button deleteBtn;
    private final DotSelector selector;

    public CraftRowView(CraftRowViewModel viewModel) {
        setSpacing(12);
        setAlignment(Pos.CENTER_LEFT);

        Label expertiseLabel = new Label();
        expertiseLabel.textProperty().bind(viewModel.expertiseProperty());
        expertiseLabel.getStyleClass().add("merit-name");
        expertiseLabel.setMinWidth(120);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        selector = new DotSelector(expertiseLabel, viewModel.ratingProperty(), 0);
        selector.descriptionProperty().bind(viewModel.expertiseProperty());

        editBtn = new Button("✎"); // Unicode pencil for edit
        editBtn.getStyleClass().add("remove-btn"); // Reuse or use a specific style
        editBtn.setTooltip(new Tooltip("Edit Expertise"));

        deleteBtn = new Button("✕"); // Unicode cross for delete
        deleteBtn.getStyleClass().add("remove-btn");
        deleteBtn.setTooltip(new Tooltip("Delete Craft"));

        getChildren().addAll(expertiseLabel, spacer, selector, editBtn, deleteBtn);
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

    public DotSelector getSelector() {
        return selector;
    }
}
