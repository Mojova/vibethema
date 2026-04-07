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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WeaponRowViewModelTest {

    private Weapon weapon;
    private ObservableList<Specialty> characterSpecialties;
    private WeaponRowViewModel viewModel;

    @BeforeEach
    void setUp() {
        weapon = new Weapon("Unarmed");
        weapon.setRange(Weapon.WeaponRange.CLOSE);
        weapon.getTags().add("Bashing");
        weapon.getTags().add("Brawl");
        weapon.getTags().add("Grappling");
        weapon.getTags().add("Natural");

        characterSpecialties = FXCollections.observableArrayList();
        characterSpecialties.add(new Specialty("1", "Melee Specialty", "Melee"));
        characterSpecialties.add(new Specialty("2", "Archery Specialty", "Archery"));
        characterSpecialties.add(new Specialty("3", "Brawl Specialty", "Brawl"));
        characterSpecialties.add(new Specialty("4", "Thrown Specialty", "Thrown"));

        viewModel = new WeaponRowViewModel(weapon, characterSpecialties);
    }

    @Test
    void testInitialFilteringByBrawlTag() {
        // Brawl tag is present, so Brawl Specialty should be available
        assertTrue(
                viewModel.getAvailableSpecialties().stream()
                        .anyMatch(s -> s != null && s.getName().equals("Brawl Specialty")));
        assertFalse(
                viewModel.getAvailableSpecialties().stream()
                        .anyMatch(s -> s != null && s.getName().equals("Melee Specialty")));
    }

    @Test
    void testReactiveFilteringByTagChange() {
        // Change tags from Brawl to Melee
        weapon.getTags().clear();
        weapon.getTags().add("Melee");

        assertTrue(
                viewModel.getAvailableSpecialties().stream()
                        .anyMatch(s -> s != null && s.getName().equals("Melee Specialty")));
        assertFalse(
                viewModel.getAvailableSpecialties().stream()
                        .anyMatch(s -> s != null && s.getName().equals("Brawl Specialty")));
    }

    @Test
    void testSelectedSpecialtyProperty() {
        Specialty brawlSpec =
                characterSpecialties.stream()
                        .filter(s -> s.getAbility().equals("Brawl"))
                        .findFirst()
                        .get();
        viewModel.selectedSpecialtyProperty().set(brawlSpec);

        assertEquals(brawlSpec.getId(), weapon.getSpecialtyId());
    }
}
