package com.vibethema.viewmodel.equipment;

import com.vibethema.model.CharacterData;
import com.vibethema.model.Weapon;
import com.vibethema.service.CharmDataService;
import com.vibethema.service.EquipmentDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class EquipmentViewModelTest {
    private CharacterData data;
    private EquipmentViewModel viewModel;

    @BeforeEach
    void setUp() {
        data = new CharacterData();
        // Mocking services with new instances for testing purposes
        viewModel = new EquipmentViewModel(
            data, 
            new EquipmentDataService(), 
            new CharmDataService(),
            new HashMap<>(), 
            () -> {}, 
            (id, name) -> {}
        );
    }

    @Test
    void testAddingWeaponUpdatesViewModel() {
        Weapon weapon = new Weapon("Test Blade");
        viewModel.addWeapon(weapon);

        assertEquals(2, data.getWeapons().size()); // Base has 1 (Unarmed), so now 2.
        assertEquals(2, viewModel.getWeapons().size());
        assertEquals("Test Blade", viewModel.getWeapons().get(1).nameProperty().get());
    }

    @Test
    void testRemovingWeaponUpdatesViewModel() {
        Weapon weapon = data.getWeapons().get(0); // Unarmed
        viewModel.removeWeapon(weapon);

        assertEquals(0, data.getWeapons().size());
        assertEquals(0, viewModel.getWeapons().size());
    }

    @Test
    void testModelUpdateSyncsToViewModel() {
        Weapon weapon = new Weapon("Background Added Blade");
        data.getWeapons().add(weapon);

        assertEquals(2, viewModel.getWeapons().size());
        assertEquals("Background Added Blade", viewModel.getWeapons().get(1).nameProperty().get());
    }
}
