package com.vibethema.model;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.vibethema.model.equipment.*;
import org.junit.jupiter.api.Test;

class IndependentEquipmentTest {
    @Test
    void copiesSurviveTemplateChangesAndLoadWithoutADatabase() {
        Weapon weapon = new Weapon("Sword");
        weapon.getTags().add("Balanced");
        Armor armor = new Armor("Plate");
        armor.getTags().add("Silent");
        Hearthstone stone = new Hearthstone("Gem", "Original power");
        OtherEquipment gear = new OtherEquipment("Rope", "Original rope");
        gear.setArtifact(true);
        gear.setAttunement(3);

        CharacterData character = new CharacterData();
        Weapon first = weapon.copy();
        Weapon second = weapon.copy();
        first.setEquipped(true);
        first.setSpecialtyId("sword-specialty");
        character.getWeapons().addAll(first, second);
        character.getArmors().add(armor.copy());
        character.getHearthstones().add(stone.copy());
        character.getOtherEquipment().add(gear.copy());

        weapon.setName("Changed sword");
        weapon.getTags().clear();
        armor.setName("Changed armor");
        armor.getTags().clear();
        stone.setDescription("Changed power");
        gear.setAttunement(9);

        Gson gson = new Gson();
        CharacterSaveState snapshot = character.exportState();
        CharacterSaveState saved = gson.fromJson(gson.toJson(snapshot), CharacterSaveState.class);
        assertEquals(snapshot, saved);
        CharacterData loaded = new CharacterData();
        loaded.importState(saved, null);
        assertEquals("Sword", loaded.getWeapons().get(0).getName());
        assertEquals(first.getTags(), loaded.getWeapons().get(0).getTags());
        assertEquals(first.getInstanceId(), loaded.getWeapons().get(0).getInstanceId());
        assertNotEquals(first.getInstanceId(), second.getInstanceId());
        assertTrue(loaded.getWeapons().get(0).isEquipped());
        assertEquals("sword-specialty", loaded.getWeapons().get(0).getSpecialtyId());
        assertEquals("Plate", loaded.getArmors().get(0).getName());
        assertTrue(loaded.getArmors().get(0).getTags().contains("Silent"));
        assertEquals("Original power", loaded.getHearthstones().get(0).getDescription());
        assertEquals(3, loaded.getOtherEquipment().get(0).getAttunement());

        loaded.getWeapons().get(0).getTags().clear();
        assertTrue(saved.weapons.get(0).item.tags.contains("Balanced"));
        assertTrue(loaded.getWeapons().get(1).getTags().contains("Balanced"));
        assertNotEquals(saved, loaded.exportState());
        loaded.restoreState(saved, null);
        assertTrue(loaded.getWeapons().get(0).getTags().contains("Balanced"));
    }
}
