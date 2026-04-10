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

        TextField nameField = new TextField();
        nameField.setPromptText("Merit Name (e.g. Artifact)");
        nameField.textProperty().bindBidirectional(viewModel.nameProperty());
        nameField.setPrefWidth(250);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Hidden label for DotSelector accessibility/title logic
        Label hiddenLabel = new Label();
        hiddenLabel.textProperty().bind(nameField.textProperty());
        hiddenLabel.setVisible(false);
        hiddenLabel.setManaged(false);

        selector = new DotSelector(hiddenLabel, viewModel.ratingProperty(), 1, 5);
        selector.descriptionProperty().bind(viewModel.nameProperty());

        removeBtn = new Button("✕");
        removeBtn.getStyleClass().add("remove-btn");

        getChildren().addAll(nameField, spacer, hiddenLabel, selector, removeBtn);
    }

    public void setOnRemove(javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        removeBtn.setOnAction(handler);
    }

    public DotSelector getSelector() {
        return selector;
    }
}
