package com.vibethema.viewmodel.equipment;

import com.vibethema.model.*;
import com.vibethema.service.CharmDataService;
import com.vibethema.service.EquipmentDataService;
import de.saxsys.mvvmfx.ViewModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Main ViewModel for the Equipment tab.
 * Manages the high-level collections and service interactions for equipment.
 */
public class EquipmentViewModel implements ViewModel {
    private final CharacterData data;
    private final EquipmentDataService equipmentService;
    private final CharmDataService dataService;
    private final Map<String, String> tagDescriptions;
    private final Runnable refreshSummary;
    private final BiConsumer<String, String> evocationsCaller;

    private final ObservableList<WeaponRowViewModel> weapons = FXCollections.observableArrayList();
    private final ObservableList<ArmorRowViewModel> armors = FXCollections.observableArrayList();
    private final ObservableList<HearthstoneRowViewModel> hearthstones = FXCollections.observableArrayList();
    private final ObservableList<OtherEquipmentRowViewModel> otherEquipment = FXCollections.observableArrayList();

    public EquipmentViewModel(CharacterData data, 
                              EquipmentDataService equipmentService, 
                              CharmDataService dataService,
                              Map<String, String> tagDescriptions, 
                              Runnable refreshSummary, 
                              BiConsumer<String, String> evocationsCaller) {
        this.data = data;
        this.equipmentService = equipmentService;
        this.dataService = dataService;
        this.tagDescriptions = tagDescriptions;
        this.refreshSummary = refreshSummary;
        this.evocationsCaller = evocationsCaller;

        syncCollections();
        setupModelListeners();
    }

    private void syncCollections() {
        weapons.setAll(data.getWeapons().stream().map(WeaponRowViewModel::new).collect(Collectors.toList()));
        armors.setAll(data.getArmors().stream().map(ArmorRowViewModel::new).collect(Collectors.toList()));
        hearthstones.setAll(data.getHearthstones().stream().map(HearthstoneRowViewModel::new).collect(Collectors.toList()));
        otherEquipment.setAll(data.getOtherEquipment().stream().map(OtherEquipmentRowViewModel::new).collect(Collectors.toList()));
    }

    private void setupModelListeners() {
        data.getWeapons().addListener((ListChangeListener<Weapon>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    weapons.addAll(c.getAddedSubList().stream().map(WeaponRowViewModel::new).collect(Collectors.toList()));
                }
                if (c.wasRemoved()) {
                    weapons.removeIf(vm -> c.getRemoved().contains(vm.getWeapon()));
                }
            }
            if (refreshSummary != null) refreshSummary.run();
        });

        data.getArmors().addListener((ListChangeListener<Armor>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    armors.addAll(c.getAddedSubList().stream().map(ArmorRowViewModel::new).collect(Collectors.toList()));
                }
                if (c.wasRemoved()) {
                    armors.removeIf(vm -> c.getRemoved().contains(vm.getArmor()));
                }
            }
            if (refreshSummary != null) refreshSummary.run();
        });

        data.getHearthstones().addListener((ListChangeListener<Hearthstone>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    hearthstones.addAll(c.getAddedSubList().stream().map(HearthstoneRowViewModel::new).collect(Collectors.toList()));
                }
                if (c.wasRemoved()) {
                    hearthstones.removeIf(vm -> c.getRemoved().contains(vm.getHearthstone()));
                }
            }
        });

        data.getOtherEquipment().addListener((ListChangeListener<OtherEquipment>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    otherEquipment.addAll(c.getAddedSubList().stream().map(OtherEquipmentRowViewModel::new).collect(Collectors.toList()));
                }
                if (c.wasRemoved()) {
                    otherEquipment.removeIf(vm -> c.getRemoved().contains(vm.getOtherEquipment()));
                }
            }
        });
    }

    // List accessors for the View
    public ObservableList<WeaponRowViewModel> getWeapons() { return weapons; }
    public ObservableList<ArmorRowViewModel> getArmors() { return armors; }
    public ObservableList<HearthstoneRowViewModel> getHearthstones() { return hearthstones; }
    public ObservableList<OtherEquipmentRowViewModel> getOtherEquipment() { return otherEquipment; }

    // Actions
    public void addWeapon(Weapon weapon) {
        data.getWeapons().add(weapon);
        data.setDirty(true);
    }

    public void removeWeapon(Weapon weapon) {
        data.getWeapons().remove(weapon);
        data.setDirty(true);
    }

    public void addArmor(Armor armor) {
        data.getArmors().add(armor);
        data.setDirty(true);
    }

    public void removeArmor(Armor armor) {
        data.getArmors().remove(armor);
        data.setDirty(true);
    }

    public void addHearthstone(Hearthstone hearthstone) {
        data.getHearthstones().add(hearthstone);
        data.setDirty(true);
    }

    public void removeHearthstone(Hearthstone hearthstone) {
        data.getHearthstones().remove(hearthstone);
        data.setDirty(true);
    }

    public void addOtherEquipment(OtherEquipment equipment) {
        data.getOtherEquipment().add(equipment);
        data.setDirty(true);
    }

    public void removeOtherEquipment(OtherEquipment equipment) {
        data.getOtherEquipment().remove(equipment);
        data.setDirty(true);
    }

    // Specialized Logic
    public void callEvocations(String id, String name) {
        if (evocationsCaller != null) {
            evocationsCaller.accept(id, name);
        }
    }

    public Map<String, String> getTagDescriptions() {
        return tagDescriptions;
    }
    
    public EquipmentDataService getEquipmentService() { return equipmentService; }
    public CharmDataService getDataService() { return dataService; }
    public CharacterData getCharacterData() { return data; }

    public void markDirty() {
        data.setDirty(true);
    }
}
