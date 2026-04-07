package com.vibethema.model;

import com.vibethema.service.EquipmentDataService;

/**
 * Factory for creating and initializing new character data.
 */
public class CharacterFactory {

    /**
     * Creates a new character and initializes it with default equipment if available.
     * 
     * @param equipmentService The service to fetch global equipment from.
     * @return A newly initialized CharacterData instance.
     */
    public static CharacterData createNewCharacter() {
        CharacterData character = new CharacterData();
        character.setDirty(false);
        return character;
    }

    /**
     * Initializes a character with default equipment. This should be called from a background thread.
     */
    public void initializeDefaultEquipment(CharacterData character, EquipmentDataService equipmentService) {
        if (equipmentService != null) {
            Weapon unarmed = equipmentService.loadWeapon(Weapon.UNARMED_ID);
            if (unarmed != null) {
                character.getWeapons().add(unarmed.copy());
            }
        }
    }
}
