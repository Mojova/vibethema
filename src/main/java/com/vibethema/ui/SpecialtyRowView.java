package com.vibethema.ui;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.viewmodel.stats.SpecialtyRowViewModel;
import java.util.stream.Collectors;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/** UI row for an ability specialty. */
public class SpecialtyRowView extends HBox {
    public SpecialtyRowView(SpecialtyRowViewModel viewModel) {
        setSpacing(12);
        setAlignment(Pos.CENTER_LEFT);

        TextField nameField = new TextField();
        nameField.setPromptText("Specialty Name");
        nameField.textProperty().bindBidirectional(viewModel.nameProperty());
        nameField.setAccessibleText("Specialty Name");

        ComboBox<String> abilityCombo = new ComboBox<>();
        abilityCombo
                .getItems()
                .addAll(
                        SystemData.ABILITIES.stream()
                                .map(Ability::getDisplayName)
                                .collect(Collectors.toList()));
        abilityCombo.valueProperty().bindBidirectional(viewModel.abilityProperty());
        abilityCombo.setAccessibleText("Associated Ability");

        getChildren().addAll(nameField, abilityCombo);
    }
}
