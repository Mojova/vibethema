package com.vibethema.model;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.social.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.logic.*;


import com.vibethema.service.EquipmentDataService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class CharacterFactoryTest {

    @Test
    void testCreateNewCharacter() {
        CharacterData character = CharacterFactory.createNewCharacter();
        assertNotNull(character);
        assertTrue(character.getWeapons().isEmpty());
    }

    @Test
    void testInitializeDefaultEquipment() {
        // Setup mock service
        EquipmentDataService mockService = Mockito.mock(EquipmentDataService.class);
        Weapon unarmedStored = new Weapon(Weapon.UNARMED_ID, "Unarmed", Weapon.WeaponRange.CLOSE, Weapon.WeaponType.MORTAL, Weapon.WeaponCategory.LIGHT);
        
        when(mockService.loadWeapon(Weapon.UNARMED_ID)).thenReturn(unarmedStored);
        
        CharacterData character = CharacterFactory.createNewCharacter();
        
        // Execute initialization
        new CharacterFactory().initializeDefaultEquipment(character, mockService);
        
        // Verify
        assertEquals(1, character.getWeapons().size());
        Weapon weaponInChar = character.getWeapons().get(0);
        assertEquals("Unarmed", weaponInChar.getName());
        assertEquals(Weapon.UNARMED_ID, weaponInChar.getId());
        assertNotSame(unarmedStored, weaponInChar, "Factory should add a copy, not the original reference");
    }

    @Test
    void testInitializeDefaultEquipmentEmptyDB() {
        // Setup mock service where loadWeapon returns null
        EquipmentDataService mockService = Mockito.mock(EquipmentDataService.class);
        when(mockService.loadWeapon(anyString())).thenReturn(null);
        
        CharacterData character = CharacterFactory.createNewCharacter();
        
        new CharacterFactory().initializeDefaultEquipment(character, mockService);
        
        assertTrue(character.getWeapons().isEmpty());
    }
}