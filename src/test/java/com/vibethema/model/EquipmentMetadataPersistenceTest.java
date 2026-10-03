package com.vibethema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vibethema.model.equipment.Weapon;
import org.junit.jupiter.api.Test;

class EquipmentMetadataPersistenceTest {
    @Test
    void preservesEquipmentSourceIdThroughDtoAndCharacterState() {
        Weapon weapon = new Weapon("Core Sword");
        weapon.setSourceId("exalted-3e-core");

        assertEquals("exalted-3e-core", Weapon.fromData(weapon.toData()).getSourceId());

        CharacterData data = new CharacterData();
        data.getWeapons().add(weapon);
        CharacterSaveState state = data.exportState();

        assertEquals("exalted-3e-core", state.weapons.get(0).sourceId);
    }
}
