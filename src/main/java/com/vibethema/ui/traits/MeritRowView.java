package com.vibethema.ui.traits;

import com.vibethema.ui.DotSelector;
import com.vibethema.viewmodel.stats.MeritRowViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** UI component for a single Merit row, bound to a MeritRowViewModel. */
public class MeritRowView extends HBox {
    private final Button removeBtn;
    private final DotSelector selector;

    public MeritRowView(MeritRowViewModel viewModel) {
        setSpacing(15);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("merit-row");
        setPadding(new Insets(10));

        // Display components
        Label nameLabel = new Label();
        nameLabel.textProperty().bind(viewModel.nameProperty());
        nameLabel.setMinWidth(150);

        Button editBtn = new Button("✎");
        editBtn.getStyleClass().add("edit-btn");
        editBtn.setOnAction(e -> viewModel.beginEdit());

        HBox displayBox = new HBox(8, nameLabel, editBtn);
        displayBox.setAlignment(Pos.CENTER_LEFT);
        displayBox.visibleProperty().bind(viewModel.editingProperty().not());
        displayBox.managedProperty().bind(displayBox.visibleProperty());

        // Edit components
        TextField nameField = new TextField();
        nameField.setPromptText("Merit Name (e.g. Artifact)");
        nameField.textProperty().bindBidirectional(viewModel.draftNameProperty());
        nameField.setPrefWidth(200);

        Button confirmBtn = new Button("✓");
        confirmBtn.getStyleClass().add("confirm-btn");
        confirmBtn.setOnAction(e -> viewModel.commitEdit());

        Button cancelBtn = new Button("✕");
        cancelBtn.getStyleClass().add("cancel-btn");
        cancelBtn.setOnAction(e -> viewModel.cancelEdit());

        HBox editBox = new HBox(5, nameField, confirmBtn, cancelBtn);
        editBox.setAlignment(Pos.CENTER_LEFT);
        editBox.visibleProperty().bind(viewModel.editingProperty());
        editBox.managedProperty().bind(editBox.visibleProperty());

        // Keyboard handling
        nameField.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER -> viewModel.commitEdit();
                case ESCAPE -> viewModel.cancelEdit();
            }
        });

        // Auto-focus on edit
        viewModel.editingProperty().addListener((obs, oldV, newV) -> {
            if (newV) {
                javafx.application.Platform.runLater(nameField::requestFocus);
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Hidden label for DotSelector accessibility/title logic
        Label hiddenLabel = new Label();
        hiddenLabel.textProperty().bind(viewModel.nameProperty());
        hiddenLabel.setVisible(false);
        hiddenLabel.setManaged(false);

        selector = new DotSelector(hiddenLabel, viewModel.ratingProperty(), 1, 5);
        selector.descriptionProperty().bind(viewModel.nameProperty());

        removeBtn = new Button("✕");
        removeBtn.getStyleClass().add("remove-btn");

        getChildren().addAll(displayBox, editBox, spacer, hiddenLabel, selector, removeBtn);
    }

    public void setOnRemove(javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        removeBtn.setOnAction(handler);
    }

    public DotSelector getSelector() {
        return selector;
    }
}
