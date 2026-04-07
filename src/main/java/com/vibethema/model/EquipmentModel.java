package com.vibethema.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.function.Consumer;

public class EquipmentModel {
    private final ObservableList<Weapon> weapons = FXCollections.observableArrayList();
    private final ObservableList<Armor> armors = FXCollections.observableArrayList();
    private final ObservableList<Hearthstone> hearthstones = FXCollections.observableArrayList();
    private final ObservableList<OtherEquipment> otherEquipment = FXCollections.observableArrayList();
    
    private final IntegerProperty armorSoak = new SimpleIntegerProperty(0);
    private final IntegerProperty totalHardness = new SimpleIntegerProperty(0);
    
    private final Consumer<Void> dirtyListener;
    private final Runnable statUpdateTrigger;
    private boolean isImporting = false;

    public EquipmentModel(Consumer<Void> dirtyListener, Runnable statUpdateTrigger) {
        this.dirtyListener = dirtyListener;
        this.statUpdateTrigger = statUpdateTrigger;

        armors.addListener((javafx.collections.ListChangeListener.Change<? extends Armor> c) -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (Armor a : c.getAddedSubList()) {
                        a.equippedProperty().addListener((obs, old, nv) -> {
                            if (nv && !isImporting) {
                                for (Armor other : armors) {
                                    if (other != a) other.setEquipped(false);
                                }
                            }
                            updateArmorStats();
                        });
                    }
                }
            }
            updateArmorStats();
        });

        weapons.addListener((javafx.collections.ListChangeListener<? super Weapon>) c -> {
            markDirty();
            statUpdateTrigger.run();
        });
        
        otherEquipment.addListener((javafx.collections.ListChangeListener<? super OtherEquipment>) c -> {
            markDirty();
            while (c.next()) {
                if (c.wasAdded()) {
                    for (OtherEquipment oe : c.getAddedSubList()) {
                        oe.nameProperty().addListener((obs, ov, nv) -> markDirty());
                        oe.descriptionProperty().addListener((obs, ov, nv) -> markDirty());
                        oe.artifactProperty().addListener((obs, ov, nv) -> markDirty());
                    }
                }
            }
        });
        
        hearthstones.addListener((javafx.collections.ListChangeListener<? super Hearthstone>) c -> markDirty());
    }

    public void setImporting(boolean importing) {
        this.isImporting = importing;
    }

    private void markDirty() {
        if (!isImporting) {
            dirtyListener.accept(null);
        }
    }

    private void updateArmorStats() {
        int soakVal = 0;
        int hardnessVal = 0;
        for (Armor a : armors) {
            if (a.isEquipped()) {
                soakVal = a.getSoak();
                hardnessVal = a.getHardness();
                break;
            }
        }
        armorSoak.set(soakVal);
        totalHardness.set(hardnessVal);
        markDirty();
        statUpdateTrigger.run();
    }

    public ObservableList<Weapon> getWeapons() { return weapons; }
    public ObservableList<Armor> getArmors() { return armors; }
    public ObservableList<Hearthstone> getHearthstones() { return hearthstones; }
    public ObservableList<OtherEquipment> getOtherEquipment() { return otherEquipment; }
    public IntegerProperty armorSoakProperty() { return armorSoak; }
    public IntegerProperty totalHardnessProperty() { return totalHardness; }
}
