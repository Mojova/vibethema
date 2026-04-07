package com.vibethema.ui;

import com.vibethema.model.AttributePriority;
import com.vibethema.model.CharacterMode;
import com.vibethema.viewmodel.stats.AttributeCategoryViewModel;
import com.vibethema.viewmodel.stats.AttributeRowViewModel;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * UI for an attribute category (Physical, Social, or Mental),
 * managing its priority selector and its constituent rows.
 */
public class AttributeCategoryView extends VBox {
    public AttributeCategoryView(AttributeCategoryViewModel viewModel) {
        setSpacing(10);
        
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label();
        titleLabel.textProperty().bind(viewModel.titleProperty());
        titleLabel.getStyleClass().add("subsection-title");
        
        ComboBox<AttributePriority> priorityBox = new ComboBox<>();
        priorityBox.getItems().setAll(AttributePriority.values());
        priorityBox.valueProperty().bindBidirectional(viewModel.priorityProperty());
        priorityBox.visibleProperty().bind(viewModel.modeProperty().isEqualTo(CharacterMode.CREATION));
        priorityBox.managedProperty().bind(priorityBox.visibleProperty());

        Label priorityLabelText = new Label();
        priorityLabelText.textProperty().bind(Bindings.createStringBinding(() -> {
            AttributePriority p = viewModel.priorityProperty().get();
            return p == null ? "" : p.name();
        }, viewModel.priorityProperty()));
        priorityLabelText.visibleProperty().bind(viewModel.modeProperty().isEqualTo(CharacterMode.EXPERIENCED));
        priorityLabelText.managedProperty().bind(priorityLabelText.visibleProperty());

        header.getChildren().addAll(titleLabel, priorityBox, priorityLabelText);
        getChildren().add(header);

        for (AttributeRowViewModel rowVm : viewModel.getAttributeRows()) {
            getChildren().add(new AttributeRowView(rowVm));
        }
    }
}
