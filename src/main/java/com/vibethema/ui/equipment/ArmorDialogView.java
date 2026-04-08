package com.vibethema.ui.equipment;

import com.vibethema.model.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;
import com.vibethema.model.traits.*;
import com.vibethema.viewmodel.equipment.ArmorDialogViewModel;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class ArmorDialogView extends GridPane
        implements JavaView<ArmorDialogViewModel>, Initializable {

    @InjectViewModel private ArmorDialogViewModel viewModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setHgap(10);
        setVgap(10);
        setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.textProperty().bindBidirectional(viewModel.nameProperty());

        ComboBox<Armor.ArmorType> typeCombo =
                new ComboBox<>(FXCollections.observableArrayList(Armor.ArmorType.values()));
        typeCombo.valueProperty().bindBidirectional(viewModel.typeProperty());

        ComboBox<Armor.ArmorWeight> weightCombo =
                new ComboBox<>(FXCollections.observableArrayList(Armor.ArmorWeight.values()));
        weightCombo.valueProperty().bindBidirectional(viewModel.weightProperty());

        ListView<ArmorDialogViewModel.TagSelectionViewModel> tagsList =
                new ListView<>(viewModel.getAvailableTags());
        tagsList.setCellFactory(
                CheckBoxListCell.forListView(
                        ArmorDialogViewModel.TagSelectionViewModel::selectedProperty));
        tagsList.setPrefHeight(150);
        GridPane.setVgrow(tagsList, Priority.ALWAYS);

        Label nameLabel = new Label("Name:");
        nameLabel.setLabelFor(nameField);
        nameField.setAccessibleText("Armor Name");
        add(nameLabel, 0, 0);
        add(nameField, 1, 0);

        Label typeLabel = new Label("Type:");
        typeLabel.setLabelFor(typeCombo);
        typeCombo.setAccessibleText("Armor Type");
        add(typeLabel, 0, 1);
        add(typeCombo, 1, 1);

        Label weightLabel = new Label("Weight:");
        weightLabel.setLabelFor(weightCombo);
        weightCombo.setAccessibleText("Armor Weight Class");
        add(weightLabel, 0, 2);
        add(weightCombo, 1, 2);

        Label tagsLabel = new Label("Tags:");
        tagsLabel.setLabelFor(tagsList);
        tagsList.setAccessibleText("Armor Tags List");
        add(tagsLabel, 0, 3);
        add(tagsList, 1, 3);

        // Tooltip for tags (optional, but premium)
        tagsList.setTooltip(new Tooltip("Check to select tags for this armor"));
    }
}
