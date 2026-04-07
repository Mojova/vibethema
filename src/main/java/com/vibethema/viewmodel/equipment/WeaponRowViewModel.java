package com.vibethema.viewmodel.equipment;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.social.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.logic.*;


import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * ViewModel for a single weapon row in the equipment list.
 */
public class WeaponRowViewModel implements ViewModel {
    private final Weapon weapon;
    private final ObservableList<Specialty> characterSpecialties;
    private final ObservableList<Specialty> availableSpecialties = FXCollections.observableArrayList();
    private final ObjectProperty<Specialty> selectedSpecialty = new SimpleObjectProperty<>();

    public WeaponRowViewModel(Weapon weapon, ObservableList<Specialty> characterSpecialties) {
        this.weapon = weapon;
        this.characterSpecialties = characterSpecialties;
        
        // Initial specialty matching
        updateAvailableSpecialties();
        String currentSpecId = weapon.getSpecialtyId();
        if (currentSpecId != null && !currentSpecId.isEmpty()) {
            for (Specialty s : characterSpecialties) {
                if (s.getId().equals(currentSpecId)) {
                    selectedSpecialty.set(s);
                    break;
                }
            }
        }
        
        // Listeners for reactive updates
        weapon.getTags().addListener((ListChangeListener<String>) c -> updateAvailableSpecialties());
        weapon.rangeProperty().addListener((obs, ov, nv) -> updateAvailableSpecialties());
        characterSpecialties.addListener((ListChangeListener<Specialty>) c -> updateAvailableSpecialties());
        
        selectedSpecialty.addListener((obs, ov, nv) -> {
            weapon.setSpecialtyId(nv == null ? "" : nv.getId());
        });
    }

    private void updateAvailableSpecialties() {
        availableSpecialties.clear();
        availableSpecialties.add(null); // None

        boolean melee = weapon.getTags().stream().anyMatch(t -> t.equalsIgnoreCase("Melee"));
        boolean archery = weapon.getTags().stream().anyMatch(t -> t.equalsIgnoreCase("Archery"));
        boolean brawl = weapon.getTags().stream().anyMatch(t -> t.equalsIgnoreCase("Brawl"));
        boolean thrown = weapon.getTags().stream().anyMatch(t -> t.equalsIgnoreCase("Thrown"));

        // Fallback to range if no ability tags
        if (!melee && !archery && !brawl && !thrown) {
            if (weapon.getRange() == Weapon.WeaponRange.CLOSE) melee = true;
            else if (weapon.getRange() == Weapon.WeaponRange.ARCHERY) archery = true;
            else if (weapon.getRange() == Weapon.WeaponRange.THROWN) thrown = true;
        }

        for (Specialty s : characterSpecialties) {
            if (s == null || s.getName().isEmpty()) continue;
            String abil = s.getAbility();
            if (melee && "Melee".equals(abil)) availableSpecialties.add(s);
            else if (archery && "Archery".equals(abil)) availableSpecialties.add(s);
            else if (thrown && "Thrown".equals(abil)) availableSpecialties.add(s);
            else if (brawl && ("Brawl".equals(abil) || (abil != null && abil.contains("Martial Arts")))) availableSpecialties.add(s);
        }
        
        // Ensure selection remains valid
        if (selectedSpecialty.get() != null && !availableSpecialties.contains(selectedSpecialty.get())) {
            selectedSpecialty.set(null);
        }
    }

    public Weapon getWeapon() {
        return weapon;
    }

    public StringProperty nameProperty() { return weapon.nameProperty(); }
    public StringProperty idProperty() { return weapon.idProperty(); }
    public IntegerProperty accuracyProperty() { return weapon.accuracyProperty(); }
    public IntegerProperty damageProperty() { return weapon.damageProperty(); }
    public IntegerProperty defenseProperty() { return weapon.defenseProperty(); }
    public IntegerProperty overwhelmingProperty() { return weapon.overwhelmingProperty(); }
    public IntegerProperty attunementProperty() { return weapon.attunementProperty(); }
    public StringProperty specialtyIdProperty() { return weapon.specialtyIdProperty(); }
    public BooleanProperty equippedProperty() { return weapon.equippedProperty(); }
    
    public ObservableList<Specialty> getAvailableSpecialties() { return availableSpecialties; }
    public ObjectProperty<Specialty> selectedSpecialtyProperty() { return selectedSpecialty; }
    
    public boolean isArtifact() {
        return weapon.getType() == Weapon.WeaponType.ARTIFACT;
    }
}