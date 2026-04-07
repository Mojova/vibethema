package com.vibethema.viewmodel.equipment;

import static org.junit.jupiter.api.Assertions.*;

import com.vibethema.model.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;
import com.vibethema.model.traits.*;
import com.vibethema.service.EquipmentDataService;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WeaponDialogViewModelTest {

    private WeaponDialogViewModel viewModel;
    private ObservableList<Specialty> specialties;
    private List<EquipmentDataService.Tag> allTags;

    @BeforeEach
    void setUp() {
        specialties = FXCollections.observableArrayList();
        specialties.add(new Specialty("1", "Melee Specialty", "Melee"));
        specialties.add(new Specialty("2", "Archery Specialty", "Archery"));
        specialties.add(new Specialty("3", "Brawl Specialty", "Brawl"));
        specialties.add(new Specialty("4", "Thrown Specialty", "Thrown"));
        specialties.add(new Specialty("5", "Misc Specialty", "Resistance"));

        allTags = new ArrayList<>();
        allTags.add(new EquipmentDataService.Tag("Melee", "Melee tag"));
        allTags.add(new EquipmentDataService.Tag("Archery", "Archery tag"));

        viewModel = new WeaponDialogViewModel(null, allTags, specialties);
    }

    @Test
    void testInitialFilteringByRange() {
        // Default range is CLOSE, so should show Melee specialties
        assertTrue(
                viewModel.getFilteredSpecialties().stream()
                        .anyMatch(s -> s != null && s.getName().equals("Melee Specialty")));
        assertFalse(
                viewModel.getFilteredSpecialties().stream()
                        .anyMatch(s -> s != null && s.getName().equals("Archery Specialty")));
    }

    @Test
    void testFilteringByRangeChange() {
        viewModel.rangeProperty().set(Weapon.WeaponRange.ARCHERY);

        assertTrue(
                viewModel.getFilteredSpecialties().stream()
                        .anyMatch(s -> s != null && s.getName().equals("Archery Specialty")));
        assertFalse(
                viewModel.getFilteredSpecialties().stream()
                        .anyMatch(s -> s != null && s.getName().equals("Melee Specialty")));
    }

    @Test
    void testFilteringByTagSelection() {
        // Select Archery tag explicitly
        viewModel.getAvailableTags().stream()
                .filter(t -> t.getName().equals("Archery"))
                .findFirst()
                .ifPresent(t -> t.selectedProperty().set(true));

        assertTrue(
                viewModel.getFilteredSpecialties().stream()
                        .anyMatch(s -> s != null && s.getName().equals("Archery Specialty")));
        // Melee specialties might still be there if range is CLOSE and no tags are selected for
        // ability,
        // but here we HAVE selected Archery, so only Archery should show?
        // Actually the logic is additive if multiple tags are selected.
    }

    @Test
    void testApplyTo() {
        Weapon weapon = new Weapon("Test");
        viewModel.nameProperty().set("New Name");
        viewModel.rangeProperty().set(Weapon.WeaponRange.THROWN);

        Specialty thrownSpec =
                specialties.stream().filter(s -> s.getAbility().equals("Thrown")).findFirst().get();
        viewModel.selectedSpecialtyProperty().set(thrownSpec);

        viewModel.applyTo(weapon);

        assertEquals("New Name", weapon.getName());
        assertEquals(Weapon.WeaponRange.THROWN, weapon.getRange());
        assertEquals(thrownSpec.getId(), weapon.getSpecialtyId());
    }
}
