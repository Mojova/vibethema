package com.vibethema.ui;

import com.vibethema.model.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;
import com.vibethema.model.traits.*;
import com.vibethema.viewmodel.stats.AttributeRowViewModel;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** UI row for an attribute, bound to an AttributeRowViewModel. */
public class AttributeRowView extends HBox {
    public AttributeRowView(AttributeRowViewModel viewModel) {
        setSpacing(10);
        setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label();
        nameLabel.textProperty().bind(viewModel.displayNameProperty());
        nameLabel.setPrefWidth(80);

        DotSelector selector = new DotSelector(viewModel.ratingProperty(), 1);

        getChildren().addAll(nameLabel, selector);
    }
}
