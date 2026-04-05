package com.vibethema.ui.equipment;

import com.vibethema.model.Armor;
import com.vibethema.model.Hearthstone;
import com.vibethema.model.OtherEquipment;
import com.vibethema.model.Specialty;
import com.vibethema.model.Weapon;
import com.vibethema.service.EquipmentDataService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DefaultEquipmentDialogService implements EquipmentDialogService {

    @Override
    public Optional<Weapon> showWeaponDialog(
            Weapon existing,
            List<Specialty> availableSpecialties,
            EquipmentDataService equipmentService,
            Map<String, String> tagDescriptions,
            Window owner) {

        Dialog<Weapon> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add New Weapon" : "Edit Weapon");
        dialog.initOwner(owner);

        ButtonType saveBtnType = new ButtonType(existing == null ? "Add" : "Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(existing == null ? "" : existing.getName());
        nameField.setPromptText("Weapon Name (e.g. Reaper Daiklave)");

        ComboBox<Weapon.WeaponRange> rangeCombo = new ComboBox<>(FXCollections.observableArrayList(Weapon.WeaponRange.values()));
        rangeCombo.setValue(existing == null ? Weapon.WeaponRange.CLOSE : existing.getRange());

        ComboBox<Weapon.WeaponType> typeCombo = new ComboBox<>(FXCollections.observableArrayList(Weapon.WeaponType.values()));
        typeCombo.setValue(existing == null ? Weapon.WeaponType.MORTAL : existing.getType());

        ComboBox<Weapon.WeaponCategory> categoryCombo = new ComboBox<>(FXCollections.observableArrayList(Weapon.WeaponCategory.values()));
        categoryCombo.setValue(existing == null ? Weapon.WeaponCategory.MEDIUM : existing.getCategory());

        ListView<EquipmentDataService.Tag> tagsList = new ListView<>();
        tagsList.setPrefHeight(150);
        ObservableList<EquipmentDataService.Tag> selectedTags = FXCollections.observableArrayList();

        ComboBox<Specialty> specialtyCombo = new ComboBox<>();
        specialtyCombo.setPromptText("Select Specialty");
        specialtyCombo.setConverter(new StringConverter<Specialty>() {
            @Override public String toString(Specialty s) { return (s == null) ? "None" : s.getName(); }
            @Override public Specialty fromString(String string) { return null; }
        });

        Runnable updateSpecs = () -> {
            Specialty current = specialtyCombo.getValue();
            ObservableList<Specialty> options = FXCollections.observableArrayList();
            options.add(null);

            boolean melee = selectedTags.stream().anyMatch(t -> t.getName().equalsIgnoreCase("Melee"));
            boolean archery = selectedTags.stream().anyMatch(t -> t.getName().equalsIgnoreCase("Archery"));
            boolean brawl = selectedTags.stream().anyMatch(t -> t.getName().equalsIgnoreCase("Brawl"));

            for (Specialty s : availableSpecialties) {
                String abil = s.getAbility();
                if (melee && "Melee".equals(abil)) options.add(s);
                else if (archery && "Archery".equals(abil)) options.add(s);
                else if (brawl && ("Brawl".equals(abil) || abil.contains("Martial Arts"))) options.add(s); // Note: Simple check for MA
            }
            specialtyCombo.setItems(options);
            if (options.contains(current)) specialtyCombo.setValue(current);
            else specialtyCombo.setValue(null);
        };

        selectedTags.addListener((ListChangeListener<? super EquipmentDataService.Tag>) c -> updateSpecs.run());

        if (existing != null) {
            String specId = existing.getSpecialtyId();
            if (specId != null && !specId.isEmpty()) {
                specialtyCombo.setValue(availableSpecialties.stream().filter(s -> s.getId().equals(specId)).findFirst().orElse(null));
            }
            String cat = existing.getRange() == Weapon.WeaponRange.CLOSE ? "melee" :
                    (existing.getRange() == Weapon.WeaponRange.THROWN ? "thrown" : "archery");
            List<EquipmentDataService.Tag> availableTags = equipmentService.getTagsForCategory(cat);
            for (EquipmentDataService.Tag t : availableTags) {
                if (existing.getTags().contains(t.getName())) {
                    selectedTags.add(t);
                }
            }
        }
        updateSpecs.run();

        tagsList.setCellFactory(lv -> new ListCell<>() {
            private final CheckBox cb = new CheckBox();
            @Override
            protected void updateItem(EquipmentDataService.Tag item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    cb.setText(item.getName());
                    cb.setTooltip(new Tooltip(item.getDescription()));
                    cb.setSelected(selectedTags.stream().anyMatch(st -> st.getName().equals(item.getName())));
                    cb.setOnAction(e -> {
                        if (cb.isSelected()) {
                            if (selectedTags.stream().noneMatch(st -> st.getName().equals(item.getName()))) {
                                selectedTags.add(item);
                            }
                        } else {
                            selectedTags.removeIf(st -> st.getName().equals(item.getName()));
                        }
                    });
                    setGraphic(cb);
                }
            }
        });

        Runnable updateTagsList = () -> {
            String cat = rangeCombo.getValue() == Weapon.WeaponRange.CLOSE ? "melee" :
                    (rangeCombo.getValue() == Weapon.WeaponRange.THROWN ? "thrown" : "archery");
            List<EquipmentDataService.Tag> available = equipmentService.getTagsForCategory(cat);
            tagsList.getItems().setAll(available);
            tagsList.refresh();
        };

        rangeCombo.setOnAction(e -> {
            selectedTags.clear();
            updateTagsList.run();
        });
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
        grid.add(new Label("Specialty:"), 0, 5);
        grid.add(specialtyCombo, 1, 5);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(nameField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveBtnType) {
                Weapon nw = (existing != null) ? existing : new Weapon(nameField.getText());
                nw.setName(nameField.getText());
                nw.setRange(rangeCombo.getValue());
                nw.setType(typeCombo.getValue());
                nw.setCategory(categoryCombo.getValue());
                nw.getTags().setAll(selectedTags.stream().map(EquipmentDataService.Tag::getName).collect(Collectors.toList()));
                nw.setSpecialtyId(specialtyCombo.getValue() == null ? "" : specialtyCombo.getValue().getId());
                return nw;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    @Override
    public Optional<Armor> showArmorDialog(Armor existing, Map<String, String> tagDescriptions, Window owner) {
        Dialog<Armor> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Armor" : "Edit Armor");
        dialog.initOwner(owner);
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        TextField name = new TextField();
        ComboBox<Armor.ArmorType> type = new ComboBox<>(FXCollections.observableArrayList(Armor.ArmorType.values()));
        if (existing != null) { name.setText(existing.getName()); type.setValue(existing.getType()); }
        grid.add(new Label("Name:"), 0, 0); grid.add(name, 1, 0); grid.add(new Label("Type:"), 0, 1); grid.add(type, 1, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(b -> {
            if (b == saveBtn) {
                Armor a = (existing != null) ? existing : new Armor(name.getText());
                a.setName(name.getText()); a.setType(type.getValue()); return a;
            } return null;
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
        VBox root = new VBox(10); root.setPadding(new Insets(10));
        TextField name = new TextField(); if (existing != null) name.setText(existing.getName());
        TextArea desc = new TextArea(); if (existing != null) desc.setText(existing.getDescription());
        root.getChildren().addAll(new Label("Name:"), name, new Label("Description:"), desc);
        dialog.getDialogPane().setContent(root);
        dialog.setResultConverter(b -> {
            if (b == saveBtn) {
                Hearthstone h = (existing != null) ? existing : new Hearthstone(name.getText(), desc.getText());
                h.setName(name.getText()); h.setDescription(desc.getText()); return h;
            } return null;
        });
        return dialog.showAndWait();
    }

    @Override
    public Optional<OtherEquipment> showOtherEquipmentDialog(OtherEquipment existing, Window owner) {
        Dialog<OtherEquipment> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Equipment" : "Edit Equipment");
        dialog.initOwner(owner);
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        VBox root = new VBox(10); root.setPadding(new Insets(10));
        TextField name = new TextField(); if (existing != null) name.setText(existing.getName());
        TextArea desc = new TextArea(); if (existing != null) desc.setText(existing.getDescription());
        CheckBox artifact = new CheckBox("Artifact Status"); if (existing != null) artifact.setSelected(existing.isArtifact());
        root.getChildren().addAll(new Label("Name:"), name, artifact, new Label("Description:"), desc);
        dialog.getDialogPane().setContent(root);
        dialog.setResultConverter(b -> {
            if (b == saveBtn) {
                OtherEquipment o = (existing != null) ? existing : new OtherEquipment(name.getText(), desc.getText());
                o.setName(name.getText()); o.setDescription(desc.getText()); o.setArtifact(artifact.isSelected()); return o;
            } return null;
        });
        return dialog.showAndWait();
    }
}
