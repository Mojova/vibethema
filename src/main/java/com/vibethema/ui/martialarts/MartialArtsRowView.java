package com.vibethema.ui.martialarts;

import com.vibethema.ui.DotSelector;
import com.vibethema.viewmodel.martialarts.MartialArtsRowViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** UI component for a single Martial Arts style row. */
public class MartialArtsRowView extends HBox {
    private final Button removeBtn;
    private final DotSelector selector;

    public MartialArtsRowView(MartialArtsRowViewModel viewModel) {
        setSpacing(15);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("merit-row"); // Reuse styling or use martial-arts-row
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
        nameField.setPromptText("Style Name (e.g. Tiger Style)");
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
        nameField.setOnKeyPressed(
                e -> {
                    switch (e.getCode()) {
                        case ENTER -> viewModel.commitEdit();
                        case ESCAPE -> viewModel.cancelEdit();
                    }
                });

        // Auto-focus on edit
        viewModel
                .editingProperty()
                .addListener(
                        (obs, oldV, newV) -> {
                            if (newV) {
                                javafx.application.Platform.runLater(nameField::requestFocus);
                            }
                        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Caste/Favored indicators (read-only per style, as they are global)
        CheckBox casteBox = new CheckBox("C");
        casteBox.selectedProperty().bind(viewModel.isCaste());
        casteBox.setDisable(true);
        casteBox.getStyleClass().add("caste-favored-box");

        CheckBox favoredBox = new CheckBox("F");
        favoredBox.selectedProperty().bind(viewModel.isFavored());
        favoredBox.setDisable(true);
        favoredBox.getStyleClass().add("caste-favored-box");

        HBox statusBox = new HBox(5, casteBox, favoredBox);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        // Hidden label for DotSelector
        Label hiddenLabel = new Label();
        hiddenLabel.textProperty().bind(viewModel.nameProperty());
        hiddenLabel.setVisible(false);
        hiddenLabel.setManaged(false);

        selector = new DotSelector(hiddenLabel, viewModel.ratingProperty(), 0, 5);
        selector.descriptionProperty().bind(viewModel.nameProperty());

        removeBtn = new Button("✕");
        removeBtn.getStyleClass().add("remove-btn");

        getChildren()
                .addAll(displayBox, editBox, spacer, statusBox, hiddenLabel, selector, removeBtn);
    }

    public void setOnRemove(javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        removeBtn.setOnAction(handler);
    }

    public DotSelector getSelector() {
        return selector;
    }
}
