package com.vibethema.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.vibethema.model.equipment.*;
import com.vibethema.service.EquipmentDataService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class EquipmentPersistenceTest {

    @Test
    public void testMultipleIdenticalWeaponsPersistence() {
        EquipmentDataService mockService = Mockito.mock(EquipmentDataService.class);

        // Mock loading a weapon from the database
        String templateId = "short-sword-id";
        Weapon prototype =
                new Weapon(
                        templateId,
                        "Short Sword",
                        Weapon.WeaponRange.CLOSE,
                        Weapon.WeaponType.MORTAL,
                        Weapon.WeaponCategory.LIGHT);

        // Ensure loadWeapon returns a NEW instance each time (crucial)
        when(mockService.loadWeapon(anyString()))
                .thenAnswer(
                        invocation -> {
                            String id = invocation.getArgument(0);
                            if (id.equals(templateId)) {
                                return new Weapon(
                                        templateId,
                                        "Short Sword",
                                        Weapon.WeaponRange.CLOSE,
                                        Weapon.WeaponType.MORTAL,
                                        Weapon.WeaponCategory.LIGHT);
                            }
                            return null;
                        });

        CharacterData data = new CharacterData();
        data.getWeapons().clear(); // Remove default "Unarmed" if present

        // Add two identical weapons
        Weapon w1 = prototype.copy();
        Weapon w2 = prototype.copy();

        data.getWeapons().add(w1);
        data.getWeapons().add(w2);

        assertEquals(2, data.getWeapons().size());
        assertNotEquals(w1.getInstanceId(), w2.getInstanceId());

        // Export state
        CharacterSaveState state = data.exportState();
        assertEquals(2, state.weapons.size());
        assertEquals(templateId, state.weapons.get(0).id);
        assertEquals(templateId, state.weapons.get(1).id);
        assertNotEquals(state.weapons.get(0).instanceId, state.weapons.get(1).instanceId);

        // Import into a new CharacterData
        CharacterData newData = new CharacterData();
        newData.importState(state, mockService);

        assertEquals(2, newData.getWeapons().size(), "Should have two weapons after import");
        Weapon imported1 = newData.getWeapons().get(0);
        Weapon imported2 = newData.getWeapons().get(1);

        assertEquals(templateId, imported1.getId());
        assertEquals(templateId, imported2.getId());
        assertNotEquals(
                imported1.getInstanceId(),
                imported2.getInstanceId(),
                "Imported instance IDs should be unique");

        // Verify they are different objects
        assertNotSame(imported1, imported2);
    }
}
