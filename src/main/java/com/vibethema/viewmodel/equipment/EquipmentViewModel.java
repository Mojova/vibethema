package com.vibethema.viewmodel.equipment;

import com.vibethema.model.*;
import com.vibethema.model.equipment.*;
import com.vibethema.service.CharmDataService;
import com.vibethema.service.EquipmentDataService;
import com.vibethema.viewmodel.MainViewModel.CheckpointRequest;
import com.vibethema.viewmodel.util.Messenger;
import de.saxsys.mvvmfx.ViewModel;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main ViewModel for the Equipment tab. Manages the high-level collections and service interactions
 * for equipment.
 */
public class EquipmentViewModel implements ViewModel {
    private static final Logger logger = LoggerFactory.getLogger(EquipmentViewModel.class);
    private final CharacterData data;
    private final EquipmentDataService equipmentService;
    private final CharmDataService dataService;
    private final Map<String, String> tagDescriptions;
    private final Runnable refreshSummary;

    private final ObservableList<WeaponRowViewModel> weapons = FXCollections.observableArrayList();
    private final ObservableList<ArmorRowViewModel> armors = FXCollections.observableArrayList();
    private final ObservableList<HearthstoneRowViewModel> hearthstones =
            FXCollections.observableArrayList();
    private final ObservableList<OtherEquipmentRowViewModel> otherEquipment =
            FXCollections.observableArrayList();

    public EquipmentViewModel(
            CharacterData data,
            EquipmentDataService equipmentService,
            CharmDataService dataService,
            Map<String, String> tagDescriptions,
            Runnable refreshSummary) {
        this.data = data;
        this.equipmentService = equipmentService;
        this.dataService = dataService;
        this.tagDescriptions = tagDescriptions;
        this.refreshSummary = refreshSummary;

        syncCollections();
        setupModelListeners();
    }

    private void syncCollections() {
        weapons.setAll(
                data.getWeapons().stream()
                        .map(w -> new WeaponRowViewModel(w, data.getSpecialties()))
                        .collect(Collectors.toList()));
        armors.setAll(
                data.getArmors().stream().map(ArmorRowViewModel::new).collect(Collectors.toList()));
        hearthstones.setAll(
                data.getHearthstones().stream()
                        .map(HearthstoneRowViewModel::new)
                        .collect(Collectors.toList()));
        otherEquipment.setAll(
                data.getOtherEquipment().stream()
                        .map(OtherEquipmentRowViewModel::new)
                        .collect(Collectors.toList()));
    }

    private void setupModelListeners() {
        data.getWeapons()
                .addListener(
                        (ListChangeListener<Weapon>)
                                c -> {
                                    while (c.next()) {
                                        if (c.wasAdded()) {
                                            weapons.addAll(
                                                    c.getAddedSubList().stream()
                                                            .map(w ->
                                                                    new WeaponRowViewModel(
                                                                            w,
                                                                            data.getSpecialties()))
                                                            .collect(Collectors.toList()));
                                        }
                                        if (c.wasRemoved()) {
                                            weapons.removeIf(
                                                    vm -> c.getRemoved().contains(vm.getWeapon()));
                                        }
                                    }
                                    if (refreshSummary != null) refreshSummary.run();
                                });

        data.getArmors()
                .addListener(
                        (ListChangeListener<Armor>)
                                c -> {
                                    while (c.next()) {
                                        if (c.wasAdded()) {
                                            armors.addAll(
                                                    c.getAddedSubList().stream()
                                                            .map(ArmorRowViewModel::new)
                                                            .collect(Collectors.toList()));
                                        }
                                        if (c.wasRemoved()) {
                                            armors.removeIf(
                                                    vm -> c.getRemoved().contains(vm.getArmor()));
                                        }
                                    }
                                    if (refreshSummary != null) refreshSummary.run();
                                });

        data.getHearthstones()
                .addListener(
                        (ListChangeListener<Hearthstone>)
                                c -> {
                                    while (c.next()) {
                                        if (c.wasAdded()) {
                                            hearthstones.addAll(
                                                    c.getAddedSubList().stream()
                                                            .map(HearthstoneRowViewModel::new)
                                                            .collect(Collectors.toList()));
                                        }
                                        if (c.wasRemoved()) {
                                            hearthstones.removeIf(vm ->
                                                    c.getRemoved().contains(vm.getHearthstone()));
                                        }
                                    }
                                });

        data.getOtherEquipment()
                .addListener(
                        (ListChangeListener<OtherEquipment>)
                                c -> {
                                    while (c.next()) {
                                        if (c.wasAdded()) {
                                            otherEquipment.addAll(
                                                    c.getAddedSubList().stream()
                                                            .map(OtherEquipmentRowViewModel::new)
                                                            .collect(Collectors.toList()));
                                        }
                                        if (c.wasRemoved()) {
                                            otherEquipment.removeIf(vm ->
                                                    c.getRemoved()
                                                            .contains(vm.getOtherEquipment()));
                                        }
                                    }
                                });
    }

    // List accessors for the View
    public ObservableList<WeaponRowViewModel> getWeapons() {
        return weapons;
    }

    public ObservableList<ArmorRowViewModel> getArmors() {
        return armors;
    }

    public ObservableList<HearthstoneRowViewModel> getHearthstones() {
        return hearthstones;
    }

    public ObservableList<OtherEquipmentRowViewModel> getOtherEquipment() {
        return otherEquipment;
    }

    // Database access
    public ObservableList<Weapon> getGlobalWeapons() {
        return FXCollections.observableArrayList(equipmentService.getGlobalWeapons());
    }

    public ObservableList<Armor> getGlobalArmors() {
        return FXCollections.observableArrayList(equipmentService.getGlobalArmors());
    }

    public Set<String> getArtifactIdsWithEvocationCollections() {
        return dataService.getArtifactIdsWithEvocationCollections();
    }

    public ObservableList<Hearthstone> getGlobalHearthstones() {
        return FXCollections.observableArrayList(equipmentService.getGlobalHearthstones());
    }

    public ObservableList<OtherEquipment> getGlobalOtherEquipment() {
        return FXCollections.observableArrayList(equipmentService.getGlobalOtherEquipment());
    }

    public void addWeaponFromDatabase(Weapon w) {
        Messenger.publish(
                "RECORD_UNDO_CHECKPOINT",
                new CheckpointRequest(
                        "Equipment", "Add Weapon: " + w.getName(), "equipment.weapons"));
        var item = w.copy();
        if (item.getType() == Weapon.WeaponType.ARTIFACT)
            seedEvocations(item.getId(), item.getInstanceId());
        data.getWeapons().add(item);
        data.setDirty(true);
    }

    public void addArmorFromDatabase(Armor a) {
        Messenger.publish(
                "RECORD_UNDO_CHECKPOINT",
                new CheckpointRequest("Equipment", "Add Armor: " + a.getName(), "equipment.armor"));
        var item = a.copy();
        if (item.getType() == Armor.ArmorType.ARTIFACT)
            seedEvocations(item.getId(), item.getInstanceId());
        data.getArmors().add(item);
        data.setDirty(true);
    }

    public void addHearthstoneFromDatabase(Hearthstone h) {
        Messenger.publish(
                "RECORD_UNDO_CHECKPOINT",
                new CheckpointRequest(
                        "Equipment", "Add Hearthstone: " + h.getName(), "equipment.hearthstones"));
        data.getHearthstones().add(h.copy());
        data.setDirty(true);
    }

    public void addOtherEquipmentFromDatabase(OtherEquipment oe) {
        Messenger.publish(
                "RECORD_UNDO_CHECKPOINT",
                new CheckpointRequest("Equipment", "Add Item: " + oe.getName(), "equipment.other"));
        var item = oe.copy();
        if (item.isArtifact()) seedEvocations(item.getId(), item.getInstanceId());
        data.getOtherEquipment().add(item);
        data.setDirty(true);
    }

    private void seedEvocations(String definitionId, String instanceId) {
        var collection = dataService.loadEvocations(definitionId);
        if (collection != null) data.copyEvocationTree(instanceId, collection.evocations);
    }

    // Actions
    public void saveWeapon(Weapon weapon, boolean isNew) {
        saveItem(
                data.getWeapons(),
                weapon,
                Weapon::getInstanceId,
                isNew,
                "Weapon: " + weapon.getName(),
                "equipment.weapons");
    }

    public void removeWeapon(Weapon weapon) {
        Messenger.publish(
                "RECORD_UNDO_CHECKPOINT",
                new CheckpointRequest(
                        "Equipment", "Remove Weapon: " + weapon.getName(), "equipment.weapons"));
        data.getWeapons().remove(weapon);
        data.setDirty(true);
        Messenger.publish("refresh_all_ui");
    }

    public void saveArmor(Armor armor, boolean isNew) {
        saveItem(
                data.getArmors(),
                armor,
                Armor::getInstanceId,
                isNew,
                "Armor: " + armor.getName(),
                "equipment.armor");
    }

    public void removeArmor(Armor armor) {
        Messenger.publish(
                "RECORD_UNDO_CHECKPOINT",
                new CheckpointRequest(
                        "Equipment", "Remove Armor: " + armor.getName(), "equipment.armor"));
        data.getArmors().remove(armor);
        data.setDirty(true);
        Messenger.publish("refresh_all_ui");
    }

    public void saveHearthstone(Hearthstone hearthstone, boolean isNew) {
        saveItem(
                data.getHearthstones(),
                hearthstone,
                Hearthstone::getInstanceId,
                isNew,
                "Hearthstone: " + hearthstone.getName(),
                "equipment.hearthstones");
    }

    public void removeHearthstone(Hearthstone hearthstone) {
        Messenger.publish(
                "RECORD_UNDO_CHECKPOINT",
                new CheckpointRequest(
                        "Equipment",
                        "Remove Hearthstone: " + hearthstone.getName(),
                        "equipment.hearthstones"));
        data.getHearthstones().remove(hearthstone);
        data.setDirty(true);
    }

    public void saveOtherEquipment(OtherEquipment equipment, boolean isNew) {
        saveItem(
                data.getOtherEquipment(),
                equipment,
                OtherEquipment::getInstanceId,
                isNew,
                "Item: " + equipment.getName(),
                "equipment.other");
    }

    public void removeOtherEquipment(OtherEquipment equipment) {
        Messenger.publish(
                "RECORD_UNDO_CHECKPOINT",
                new CheckpointRequest(
                        "Equipment", "Remove Item: " + equipment.getName(), "equipment.other"));
        data.getOtherEquipment().remove(equipment);
        data.setDirty(true);
        Messenger.publish("refresh_all_ui");
    }

    private <T> void saveItem(
            ObservableList<T> inventory,
            T item,
            java.util.function.Function<T, String> identity,
            boolean isNew,
            String label,
            String target) {
        Messenger.publish(
                "RECORD_UNDO_CHECKPOINT",
                new CheckpointRequest("Equipment", (isNew ? "Add " : "Edit ") + label, target));
        if (isNew) inventory.add(item);
        else {
            String instanceId = identity.apply(item);
            for (int i = 0; i < inventory.size(); i++) {
                if (instanceId.equals(identity.apply(inventory.get(i)))) {
                    inventory.set(i, item);
                    break;
                }
            }
        }
        data.setDirty(true);
        Messenger.publish("refresh_all_ui");
    }

    // Specialized Logic
    public void callEvocations(String id, String name) {
        Messenger.publish("jump_to_evocations", id, name);
    }

    public Map<String, String> getTagDescriptions() {
        return tagDescriptions;
    }

    public EquipmentDataService getEquipmentService() {
        return equipmentService;
    }

    public CharmDataService getDataService() {
        return dataService;
    }

    public CharacterData getCharacterData() {
        return data;
    }

    public void markDirty() {
        data.setDirty(true);
    }

    // Dialog Requests
    public void requestAddWeapon() {
        Messenger.publish("show_weapon_dialog", (Object) null);
    }

    public void requestEditWeapon(Weapon w) {
        Messenger.publish("show_weapon_dialog", w);
    }

    public void requestWeaponDatabase() {
        Messenger.publish("show_weapon_database");
    }

    public void requestAddArmor() {
        Messenger.publish("show_armor_dialog", (Object) null);
    }

    public void requestEditArmor(Armor a) {
        Messenger.publish("show_armor_dialog", a);
    }

    public void requestArmorDatabase() {
        Messenger.publish("show_armor_database");
    }

    public void requestAddHearthstone() {
        Messenger.publish("show_hearthstone_dialog", (Object) null);
    }

    public void requestEditHearthstone(Hearthstone h) {
        Messenger.publish("show_hearthstone_dialog", h);
    }

    public void requestHearthstoneDatabase() {
        Messenger.publish("show_hearthstone_database");
    }

    public void requestAddOtherEquipment() {
        Messenger.publish("show_other_equipment_dialog", (Object) null);
    }

    public void requestEditOtherEquipment(OtherEquipment oe) {
        Messenger.publish("show_other_equipment_dialog", oe);
    }

    public void requestOtherEquipmentDatabase() {
        Messenger.publish("show_other_equipment_database");
    }
}
