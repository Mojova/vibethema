package com.vibethema.viewmodel.equipment;

import com.vibethema.model.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;
import com.vibethema.model.traits.*;
import com.vibethema.service.EquipmentDataService;
import de.saxsys.mvvmfx.ViewModel;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

public class WeaponDialogViewModel implements ViewModel {
    private final StringProperty name = new SimpleStringProperty("");
    private final ObjectProperty<Weapon.WeaponRange> range =
            new SimpleObjectProperty<>(Weapon.WeaponRange.CLOSE);
    private final ObjectProperty<Weapon.WeaponType> type =
            new SimpleObjectProperty<>(Weapon.WeaponType.MORTAL);
    private final ObjectProperty<Weapon.WeaponCategory> category =
            new SimpleObjectProperty<>(Weapon.WeaponCategory.MEDIUM);
    private final BooleanProperty equipped = new SimpleBooleanProperty(false);

    private final ObservableList<TagSelectionViewModel> availableTags =
            FXCollections.observableArrayList();
    private final ObservableList<Specialty> characterSpecialties;
    private final ObservableList<Specialty> filteredSpecialties =
            FXCollections.observableArrayList();
    private final ObjectProperty<Specialty> selectedSpecialty = new SimpleObjectProperty<>();

    public WeaponDialogViewModel(
            Weapon existing,
            List<EquipmentDataService.Tag> allWeaponTags,
            ObservableList<Specialty> characterSpecialties) {
        this.characterSpecialties = characterSpecialties;

        if (existing != null) {
            name.set(existing.getName());
            range.set(existing.getRange());
            type.set(existing.getType());
            category.set(existing.getCategory());
            equipped.set(existing.isEquipped());
        }

        List<String> currentTags = (existing != null) ? existing.getTags() : List.of();
        availableTags.setAll(
                allWeaponTags.stream()
                        .map(t -> new TagSelectionViewModel(t, currentTags.contains(t.getName())))
                        .collect(Collectors.toList()));

        // Setup reactive specialty filtering
        setupSpecialtyFiltering();

        // Initial specialty selection
        if (existing != null
                && existing.getSpecialtyId() != null
                && !existing.getSpecialtyId().isEmpty()) {
            for (Specialty s : characterSpecialties) {
                if (s.getId().equals(existing.getSpecialtyId())) {
                    selectedSpecialty.set(s);
                    break;
                }
            }
        }

        // Add listeners to tags to trigger re-filtering
        for (TagSelectionViewModel tvm : availableTags) {
            tvm.selectedProperty().addListener((obs, ov, nv) -> updateFilteredSpecialties());
        }

        // Also listen to range changes as they might affect defaults (though tags are the primary
        // filter)
        range.addListener((obs, ov, nv) -> updateFilteredSpecialties());

        updateFilteredSpecialties();
    }

    private void setupSpecialtyFiltering() {
        // We update whenever the character's specialty list changes too (e.g. they add one
        // mid-dialog if that's possible, though unlikely)
        characterSpecialties.addListener(
                (ListChangeListener<Specialty>) c -> updateFilteredSpecialties());
    }

    private void updateFilteredSpecialties() {
        filteredSpecialties.clear();
        filteredSpecialties.add(null); // "None" option

        boolean melee = hasTag("Melee");
        boolean archery = hasTag("Archery");
        boolean brawl = hasTag("Brawl");
        boolean thrown = hasTag("Thrown");

        // Fallback to range if no ability tags are explicitly selected
        if (!melee && !archery && !brawl && !thrown) {
            if (range.get() == Weapon.WeaponRange.CLOSE) melee = true;
            else if (range.get() == Weapon.WeaponRange.ARCHERY) archery = true;
            else if (range.get() == Weapon.WeaponRange.THROWN) thrown = true;
        }

        for (Specialty s : characterSpecialties) {
            if (s == null || s.getName().isEmpty()) continue;
            String abil = s.getAbility();
            if (melee && "Melee".equals(abil)) filteredSpecialties.add(s);
            else if (archery && "Archery".equals(abil)) filteredSpecialties.add(s);
            else if (thrown && "Thrown".equals(abil)) filteredSpecialties.add(s);
            else if (brawl
                    && ("Brawl".equals(abil) || (abil != null && abil.contains("Martial Arts"))))
                filteredSpecialties.add(s);
        }

        // If the currently selected specialty is no longer in the filtered list, clear it
        if (selectedSpecialty.get() != null
                && !filteredSpecialties.contains(selectedSpecialty.get())) {
            selectedSpecialty.set(null);
        }
    }

    private boolean hasTag(String tagName) {
        return availableTags.stream()
                .anyMatch(t -> t.getName().equalsIgnoreCase(tagName) && t.isSelected());
    }

    public StringProperty nameProperty() {
        return name;
    }

    public ObjectProperty<Weapon.WeaponRange> rangeProperty() {
        return range;
    }

    public ObjectProperty<Weapon.WeaponType> typeProperty() {
        return type;
    }

    public ObjectProperty<Weapon.WeaponCategory> categoryProperty() {
        return category;
    }

    public BooleanProperty equippedProperty() {
        return equipped;
    }

    public ObservableList<TagSelectionViewModel> getAvailableTags() {
        return availableTags;
    }

    public ObservableList<Specialty> getFilteredSpecialties() {
        return filteredSpecialties;
    }

    public ObjectProperty<Specialty> selectedSpecialtyProperty() {
        return selectedSpecialty;
    }

    public Weapon applyTo(Weapon weapon) {
        if (weapon == null) {
            weapon = new Weapon(name.get());
        }
        weapon.setName(name.get());
        weapon.setRange(range.get());
        weapon.setType(type.get());
        weapon.setCategory(category.get());
        weapon.setEquipped(equipped.get());

        List<String> selectedTags =
                availableTags.stream()
                        .filter(TagSelectionViewModel::isSelected)
                        .map(TagSelectionViewModel::getName)
                        .collect(Collectors.toList());
        weapon.getTags().setAll(selectedTags);

        weapon.setSpecialtyId(
                selectedSpecialty.get() == null ? "" : selectedSpecialty.get().getId());

        return weapon;
    }

    public static class TagSelectionViewModel implements ViewModel {
        private final EquipmentDataService.Tag tag;
        private final BooleanProperty selected = new SimpleBooleanProperty();

        public TagSelectionViewModel(EquipmentDataService.Tag tag, boolean initial) {
            this.tag = tag;
            this.selected.set(initial);
        }

        public String getName() {
            return tag.getName();
        }

        public String getDescription() {
            return tag.getDescription();
        }

        public BooleanProperty selectedProperty() {
            return selected;
        }

        public boolean isSelected() {
            return selected.get();
        }

        @Override
        public String toString() {
            return getName();
        }
    }
}
