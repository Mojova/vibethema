package com.vibethema.ui;

import com.vibethema.viewmodel.stats.CraftRowViewModel;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/** UI row for a craft expertise. */
public class CraftRowView extends HBox {
    public CraftRowView(CraftRowViewModel viewModel) {
        setSpacing(12);
        setAlignment(Pos.CENTER_LEFT);

        TextField expertiseField = new TextField();
        expertiseField.setPromptText("Expertise");
        expertiseField.textProperty().bindBidirectional(viewModel.expertiseProperty());

        Label hiddenLabel = new Label();
        hiddenLabel.textProperty().bind(expertiseField.textProperty());
        hiddenLabel.setVisible(false);
        hiddenLabel.setManaged(false);

        DotSelector selector = new DotSelector(hiddenLabel, viewModel.ratingProperty(), 0);
        selector.descriptionProperty().bind(viewModel.expertiseProperty());

        getChildren().addAll(expertiseField, hiddenLabel, selector);
    }
}
