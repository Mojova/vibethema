package com.vibethema.ui;

import com.vibethema.viewmodel.stats.CraftRowViewModel;
import javafx.geometry.Pos;
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

        DotSelector selector = new DotSelector(viewModel.ratingProperty(), 0);

        getChildren().addAll(expertiseField, selector);
    }
}
