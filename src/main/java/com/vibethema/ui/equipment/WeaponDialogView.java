package com.vibethema.ui.equipment;

import com.vibethema.model.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;
import com.vibethema.model.traits.*;
import com.vibethema.viewmodel.equipment.WeaponDialogViewModel;
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
import javafx.util.StringConverter;

public class WeaponDialogView extends GridPane
        implements JavaView<WeaponDialogViewModel>, Initializable {

    @InjectViewModel private WeaponDialogViewModel viewModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setHgap(10);
        setVgap(10);
        setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.textProperty().bindBidirectional(viewModel.nameProperty());
        nameField.setPromptText("Weapon Name");

        ComboBox<Weapon.WeaponRange> rangeCombo =
                new ComboBox<>(FXCollections.observableArrayList(Weapon.WeaponRange.values()));
        rangeCombo.valueProperty().bindBidirectional(viewModel.rangeProperty());

        ComboBox<Weapon.WeaponType> typeCombo =
                new ComboBox<>(FXCollections.observableArrayList(Weapon.WeaponType.values()));
        typeCombo.valueProperty().bindBidirectional(viewModel.typeProperty());

        ComboBox<Weapon.WeaponCategory> categoryCombo =
                new ComboBox<>(FXCollections.observableArrayList(Weapon.WeaponCategory.values()));
        categoryCombo.valueProperty().bindBidirectional(viewModel.categoryProperty());

        CheckBox personalCb = new CheckBox("Equipped");
        personalCb.selectedProperty().bindBidirectional(viewModel.equippedProperty());

        ComboBox<Specialty> specialtyCombo = new ComboBox<>(viewModel.getFilteredSpecialties());
        specialtyCombo.valueProperty().bindBidirectional(viewModel.selectedSpecialtyProperty());
        specialtyCombo.setPromptText("Select Specialty");
        specialtyCombo.setConverter(
                new StringConverter<Specialty>() {
                    @Override
                    public String toString(Specialty s) {
                        return s == null ? "None" : s.getName();
                    }

                    @Override
                    public Specialty fromString(String string) {
                        return null;
                    }
                });

        ListView<WeaponDialogViewModel.TagSelectionViewModel> tagsList =
                new ListView<>(viewModel.getAvailableTags());
        tagsList.setCellFactory(
                CheckBoxListCell.forListView(
                        WeaponDialogViewModel.TagSelectionViewModel::selectedProperty));
        tagsList.setPrefHeight(150);
        GridPane.setVgrow(tagsList, Priority.ALWAYS);

        add(new Label("Name:"), 0, 0);
        add(nameField, 1, 0);
        add(new Label("Range:"), 0, 1);
        add(rangeCombo, 1, 1);
        add(new Label("Type:"), 0, 2);
        add(typeCombo, 1, 2);
        add(new Label("Category:"), 0, 3);
        add(categoryCombo, 1, 3);
        add(new Label("Specialty:"), 0, 4);
        add(specialtyCombo, 1, 4);
        add(new Label("Tags:"), 0, 5);
        add(tagsList, 1, 5);
        add(personalCb, 1, 6);

        tagsList.setTooltip(
                new Tooltip(
                        "Check to select tags for this weapon. Specialty list updates based on"
                                + " tags."));
    }
}
