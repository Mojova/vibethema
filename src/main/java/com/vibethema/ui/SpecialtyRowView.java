package com.vibethema.ui;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.social.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.logic.*;


import com.vibethema.viewmodel.stats.SpecialtyRowViewModel;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.util.stream.Collectors;

/**
 * UI row for an ability specialty.
 */
public class SpecialtyRowView extends HBox {
    public SpecialtyRowView(SpecialtyRowViewModel viewModel) {
        setSpacing(12);
        setAlignment(Pos.CENTER_LEFT);
        
        TextField nameField = new TextField();
        nameField.setPromptText("Specialty Name");
        nameField.textProperty().bindBidirectional(viewModel.nameProperty());
        
        ComboBox<String> abilityCombo = new ComboBox<>();
        abilityCombo.getItems().addAll(SystemData.ABILITIES.stream()
                .map(Ability::getDisplayName)
                .collect(Collectors.toList()));
        abilityCombo.valueProperty().bindBidirectional(viewModel.abilityProperty());
        
        getChildren().addAll(nameField, abilityCombo);
    }
}