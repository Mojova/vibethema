package com.vibethema.model;

import com.vibethema.service.EquipmentDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EquipmentPersistenceTest {

    private CharacterData data;
    
    @Mock
    private EquipmentDataService service;

    @BeforeEach
    void setUp() {
        data = new CharacterData();
    }

    @Test
    void testWeaponEquippedPersistence() {
        data.getWeapons().clear();
        Weapon w = new Weapon("Steel Sword");
        w.setEquipped(true);
        data.getWeapons().add(w);

        // Export state
        CharacterSaveState state = data.exportState();
        
        // Verify state has the weapon as equipped
        boolean found = false;
        for (CharacterSaveState.WeaponLink wl : state.weapons) {
            if (wl.id.equals(w.getId())) {
                assertTrue(wl.equipped, "Exported WeaponLink should be equipped");
                found = true;
            }
        }
        assertTrue(found, "Weapon should be found in exported state");

        // Mock the service to return our weapon when loaded
        when(service.loadWeapon(w.getId())).thenReturn(w);

        // Now import into a clean data object
        CharacterData newData = new CharacterData();
        newData.importState(state, service);
        
        Weapon importedW = newData.getWeapons().stream()
                .filter(weapon -> weapon.getId().equals(w.getId()))
                .findFirst()
                .orElse(null);
        
        assertNotNull(importedW, "Imported weapon should not be null");
        assertTrue(importedW.isEquipped(), "Imported weapon should still be equipped");
    }

    @Test
    void testArmorEquippedPersistence() {
        data.getArmors().clear();
        Armor a = new Armor("Buff Jacket");
        a.setEquipped(true);
        data.getArmors().add(a);

        // Export state
        CharacterSaveState state = data.exportState();
        
        // Verify state has armor as equipped
        boolean found = false;
        for (CharacterSaveState.ArmorLink al : state.armors) {
            if (al.id.equals(a.getId())) {
                assertTrue(al.equipped, "Exported ArmorLink should be equipped");
                found = true;
            }
        }
        assertTrue(found, "Armor should be found in exported state");

        // Mock the service to return our armor when loaded
        when(service.loadArmor(a.getId())).thenReturn(a);

        // Import into clean data object
        CharacterData newData = new CharacterData();
        newData.importState(state, service);
        
        Armor importedA = newData.getArmors().stream()
                .filter(armor -> armor.getId().equals(a.getId()))
                .findFirst()
                .orElse(null);
        
        assertNotNull(importedA);
        assertTrue(importedA.isEquipped(), "Imported armor should still be equipped");
    }
}
