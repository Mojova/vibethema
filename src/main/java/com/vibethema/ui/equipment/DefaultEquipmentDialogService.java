package com.vibethema.ui.equipment;

import com.vibethema.model.Armor;
import com.vibethema.model.Hearthstone;
import com.vibethema.model.OtherEquipment;
import com.vibethema.model.Weapon;
import com.vibethema.service.EquipmentDataService;
import com.vibethema.viewmodel.equipment.ArmorDialogViewModel;
import de.saxsys.mvvmfx.FluentViewLoader;
import de.saxsys.mvvmfx.ViewTuple;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DefaultEquipmentDialogService implements EquipmentDialogService {

    private final EquipmentDataService equipmentService;

    public DefaultEquipmentDialogService(EquipmentDataService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @Override
    public Optional<Weapon> showWeaponDialog(
            Weapon existing,
            EquipmentDataService equipmentService,
            Map<String, String> tagDescriptions,
            Window owner) {

        EquipmentDataService svc = (equipmentService != null) ? equipmentService : this.equipmentService;
        
        Dialog<Weapon> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add New Weapon" : "Edit Weapon");
        dialog.initOwner(owner);

        ButtonType saveBtnType = new ButtonType(existing == null ? "Add" : "Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(existing == null ? "" : existing.getName());
        nameField.setPromptText("Weapon Name");

        ComboBox<Weapon.WeaponRange> rangeCombo = new ComboBox<>(FXCollections.observableArrayList(Weapon.WeaponRange.values()));
        rangeCombo.setValue(existing == null ? Weapon.WeaponRange.CLOSE : existing.getRange());

        ComboBox<Weapon.WeaponType> typeCombo = new ComboBox<>(FXCollections.observableArrayList(Weapon.WeaponType.values()));
        typeCombo.setValue(existing == null ? Weapon.WeaponType.MORTAL : existing.getType());

        ComboBox<Weapon.WeaponCategory> categoryCombo = new ComboBox<>(FXCollections.observableArrayList(Weapon.WeaponCategory.values()));
        categoryCombo.setValue(existing == null ? Weapon.WeaponCategory.MEDIUM : existing.getCategory());

        CheckBox equippedField = new CheckBox("Equipped");
        equippedField.setSelected(existing != null && existing.isEquipped());

        ListView<EquipmentDataService.Tag> tagsList = new ListView<>();
        tagsList.setPrefHeight(150);
        
        List<String> currentTagNames = (existing != null) ? existing.getTags() : List.of();

        Runnable updateTagsList = () -> {
            String cat = rangeCombo.getValue() == Weapon.WeaponRange.CLOSE ? "melee" :
                    (rangeCombo.getValue() == Weapon.WeaponRange.THROWN ? "thrown" : "archery");
            if (svc != null) {
                List<EquipmentDataService.Tag> available = svc.getTagsForCategory(cat);
                tagsList.getItems().setAll(available);
                tagsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                for (EquipmentDataService.Tag t : available) {
                    if (currentTagNames.contains(t.getName())) {
                        tagsList.getSelectionModel().select(t);
                    }
                }
            }
        };

        rangeCombo.setOnAction(e -> updateTagsList.run());
        updateTagsList.run();

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Range:"), 0, 1);
        grid.add(rangeCombo, 1, 1);
        grid.add(new Label("Type:"), 0, 2);
        grid.add(typeCombo, 1, 2);
        grid.add(new Label("Category:"), 0, 3);
        grid.add(categoryCombo, 1, 3);
        grid.add(new Label("Tags:"), 0, 4);
        grid.add(tagsList, 1, 4);
        grid.add(equippedField, 1, 5);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(nameField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveBtnType) {
                Weapon nw = (existing != null) ? existing : new Weapon(nameField.getText());
                nw.setName(nameField.getText());
                nw.setRange(rangeCombo.getValue());
                nw.setType(typeCombo.getValue());
                nw.setCategory(categoryCombo.getValue());
                nw.setEquipped(equippedField.isSelected());
                nw.getTags().setAll(tagsList.getSelectionModel().getSelectedItems().stream()
                        .map(EquipmentDataService.Tag::getName)
                        .collect(Collectors.toList()));
                return nw;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    @Override
    public Optional<Armor> showArmorDialog(Armor existing, Map<String, String> tagDescriptions, Window owner) {
        List<EquipmentDataService.Tag> tags = equipmentService.getTagsForCategory("armor");
        ArmorDialogViewModel viewModel = new ArmorDialogViewModel(existing, tags);
        
        ViewTuple<ArmorDialogView, ArmorDialogViewModel> vt = FluentViewLoader
                .javaView(ArmorDialogView.class)
                .viewModel(viewModel)
                .load();

        Dialog<Armor> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Armor" : "Edit Armor");
        dialog.initOwner(owner);
        
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(vt.getView());

        dialog.setResultConverter(b -> {
            if (b == saveBtn) {
                return viewModel.applyTo(existing);
            }
            return null;
        });

        return dialog.showAndWait();
    }

    @Override
    public Optional<Hearthstone> showHearthstoneDialog(Hearthstone existing, Window owner) {
        Dialog<Hearthstone> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Hearthstone" : "Edit Hearthstone");
        dialog.initOwner(owner);
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        TextField name = new TextField(existing == null ? "" : existing.getName());
        TextArea description = new TextArea(existing == null ? "" : existing.getDescription());
        description.setWrapText(true);
        description.setPrefRowCount(3);
        grid.add(new Label("Name:"), 0, 0); grid.add(name, 1, 0);
        grid.add(new Label("Description:"), 0, 1); grid.add(description, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(b -> {
            if (b == saveBtn) {
                Hearthstone h = (existing != null) ? existing : new Hearthstone(name.getText(), description.getText());
                h.setName(name.getText());
                h.setDescription(description.getText());
                return h;
            }
            return null;
        });
        return dialog.showAndWait();
    }

    @Override
    public Optional<OtherEquipment> showOtherEquipmentDialog(OtherEquipment existing, Window owner) {
        Dialog<OtherEquipment> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Item" : "Edit Item");
        dialog.initOwner(owner);
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        TextField name = new TextField(existing == null ? "" : existing.getName());
        TextArea description = new TextArea(existing == null ? "" : existing.getDescription());
        description.setWrapText(true);
        description.setPrefRowCount(3);
        CheckBox isArtifact = new CheckBox("Artifact");
        isArtifact.setSelected(existing != null && existing.isArtifact());
        
        Spinner<Integer> attunement = new Spinner<>(0, 20, existing == null ? 0 : existing.getAttunement());
        attunement.setEditable(true);
        attunement.disableProperty().bind(isArtifact.selectedProperty().not());

        grid.add(new Label("Name:"), 0, 0); grid.add(name, 1, 0);
        grid.add(new Label("Description:"), 0, 1); grid.add(description, 1, 1);
        grid.add(isArtifact, 1, 2);
        grid.add(new Label("Attunement:"), 0, 3); grid.add(attunement, 1, 3);
        grid.add(new Label("Attunement:"), 0, 3); grid.add(attunement, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(b -> {
            if (b == saveBtn) {
                OtherEquipment oe = (existing != null) ? existing : new OtherEquipment(name.getText(), description.getText());
                oe.setName(name.getText());
                oe.setDescription(description.getText());
                oe.setArtifact(isArtifact.isSelected());
                oe.setAttunement(attunement.getValue());
                return oe;
            }
            return null;
        });
        return dialog.showAndWait();
    }
}
