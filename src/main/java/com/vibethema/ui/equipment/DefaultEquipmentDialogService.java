package com.vibethema.ui.equipment;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.social.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.logic.*;


import com.vibethema.service.EquipmentDataService;
import com.vibethema.viewmodel.equipment.ArmorDialogViewModel;
import com.vibethema.viewmodel.equipment.WeaponDialogViewModel;
import de.saxsys.mvvmfx.FluentViewLoader;
import de.saxsys.mvvmfx.ViewTuple;
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
            javafx.collections.ObservableList<com.vibethema.model.traits.Specialty> characterSpecialties,
            Window owner) {

        EquipmentDataService svc = (equipmentService != null) ? equipmentService : this.equipmentService;
        List<EquipmentDataService.Tag> allTags = svc.getTagsForCategory("melee"); // Base tags
        allTags.addAll(svc.getTagsForCategory("archery"));
        allTags.addAll(svc.getTagsForCategory("thrown"));
        
        // Remove duplicates if any
        allTags = allTags.stream().distinct().collect(Collectors.toList());

        WeaponDialogViewModel viewModel = new WeaponDialogViewModel(existing, allTags, characterSpecialties);
        
        de.saxsys.mvvmfx.ViewTuple<WeaponDialogView, WeaponDialogViewModel> viewTuple = de.saxsys.mvvmfx.FluentViewLoader
                .javaView(WeaponDialogView.class)
                .viewModel(viewModel)
                .load();

        Dialog<Weapon> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add New Weapon" : "Edit Weapon");
        dialog.initOwner(owner);

        ButtonType saveBtnType = new ButtonType(existing == null ? "Add" : "Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(viewTuple.getView());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveBtnType) {
                return viewModel.applyTo(existing);
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