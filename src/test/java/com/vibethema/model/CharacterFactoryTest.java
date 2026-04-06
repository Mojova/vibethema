package com.vibethema.model;

import com.vibethema.service.EquipmentDataService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class CharacterFactoryTest {

    @Test
    void testCreateNewCharacterWithUnarmed() {
        // Setup mock service
        EquipmentDataService mockService = Mockito.mock(EquipmentDataService.class);
        Weapon unarmedStored = new Weapon(Weapon.UNARMED_ID, "Unarmed", Weapon.WeaponRange.CLOSE, Weapon.WeaponType.MORTAL, Weapon.WeaponCategory.LIGHT);
        
        when(mockService.loadWeapon(Weapon.UNARMED_ID)).thenReturn(unarmedStored);
        
        // Execute factory
        CharacterData character = CharacterFactory.createNewCharacter(mockService);
        
        // Verify
        assertNotNull(character);
        assertEquals(1, character.getWeapons().size());
        Weapon weaponInChar = character.getWeapons().get(0);
        assertEquals("Unarmed", weaponInChar.getName());
        assertEquals(Weapon.UNARMED_ID, weaponInChar.getId());
        assertNotSame(unarmedStored, weaponInChar, "Factory should add a copy, not the original reference");
    }

    @Test
    void testCreateNewCharacterWithoutUnarmed() {
        // Setup mock service where loadWeapon returns null
        EquipmentDataService mockService = Mockito.mock(EquipmentDataService.class);
        when(mockService.loadWeapon(anyString())).thenReturn(null);
        
        // Execute factory
        CharacterData character = CharacterFactory.createNewCharacter(mockService);
        
        // Verify character starts empty if DB has no Unarmed weapon
        assertNotNull(character);
        assertTrue(character.getWeapons().isEmpty());
    }
}
