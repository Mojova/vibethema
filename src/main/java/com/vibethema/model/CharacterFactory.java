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
    public static CharacterData createNewCharacter(EquipmentDataService equipmentService) {
        CharacterData character = new CharacterData();
        
        // Attempt to add default Unarmed weapon from database
        if (equipmentService != null) {
            Weapon unarmed = equipmentService.loadWeapon(Weapon.UNARMED_ID);
            if (unarmed != null) {
                character.getWeapons().add(unarmed.copy());
            }
        }
        
        return character;
    }
}
