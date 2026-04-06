package com.vibethema.ui.equipment;

import com.vibethema.model.Armor;
import com.vibethema.viewmodel.equipment.ArmorDialogViewModel;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import javafx.collections.FXCollections;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import java.net.URL;
import java.util.ResourceBundle;

public class ArmorDialogView extends GridPane implements JavaView<ArmorDialogViewModel>, Initializable {

    @InjectViewModel
    private ArmorDialogViewModel viewModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setHgap(10);
        setVgap(10);
        setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.textProperty().bindBidirectional(viewModel.nameProperty());

        ComboBox<Armor.ArmorType> typeCombo = new ComboBox<>(FXCollections.observableArrayList(Armor.ArmorType.values()));
        typeCombo.valueProperty().bindBidirectional(viewModel.typeProperty());

        ComboBox<Armor.ArmorWeight> weightCombo = new ComboBox<>(FXCollections.observableArrayList(Armor.ArmorWeight.values()));
        weightCombo.valueProperty().bindBidirectional(viewModel.weightProperty());

        ListView<ArmorDialogViewModel.TagSelectionViewModel> tagsList = new ListView<>(viewModel.getAvailableTags());
        tagsList.setCellFactory(CheckBoxListCell.forListView(ArmorDialogViewModel.TagSelectionViewModel::selectedProperty));
        tagsList.setPrefHeight(150);
        GridPane.setVgrow(tagsList, Priority.ALWAYS);

        add(new Label("Name:"), 0, 0);
        add(nameField, 1, 0);
        add(new Label("Type:"), 0, 1);
        add(typeCombo, 1, 1);
        add(new Label("Weight:"), 0, 2);
        add(weightCombo, 1, 2);
        add(new Label("Tags:"), 0, 3);
        add(tagsList, 1, 3);
        
        // Tooltip for tags (optional, but premium)
        tagsList.setTooltip(new Tooltip("Check to select tags for this armor"));
    }
}
