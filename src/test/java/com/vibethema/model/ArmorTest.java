package com.vibethema.model;

import static org.junit.jupiter.api.Assertions.*;

import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;
import com.vibethema.model.traits.*;
import org.junit.jupiter.api.Test;

public class ArmorTest {

    @Test
    void testMortalArmorStats() {
        Armor light =
                new Armor("Padded", "Padded", Armor.ArmorType.MORTAL, Armor.ArmorWeight.LIGHT);
        assertEquals(3, light.getSoak());
        assertEquals(0, light.getMobilityPenalty());
        assertEquals(0, light.getHardness());
        assertEquals(0, light.getAttunement());

        Armor medium =
                new Armor(
                        "Breastplate",
                        "Breastplate",
                        Armor.ArmorType.MORTAL,
                        Armor.ArmorWeight.MEDIUM);
        assertEquals(5, medium.getSoak());
        assertEquals(-1, medium.getMobilityPenalty());

        Armor heavy = new Armor("Plate", "Plate", Armor.ArmorType.MORTAL, Armor.ArmorWeight.HEAVY);
        assertEquals(7, heavy.getSoak());
        assertEquals(-2, heavy.getMobilityPenalty());
    }

    @Test
    void testArtifactArmorStats() {
        Armor light = new Armor("Silk", "Silk", Armor.ArmorType.ARTIFACT, Armor.ArmorWeight.LIGHT);
        assertEquals(5, light.getSoak());
        assertEquals(4, light.getHardness());
        assertEquals(0, light.getMobilityPenalty());
        assertEquals(4, light.getAttunement());

        Armor medium =
                new Armor(
                        "Lamellar", "Lamellar", Armor.ArmorType.ARTIFACT, Armor.ArmorWeight.MEDIUM);
        assertEquals(8, medium.getSoak());
        assertEquals(7, medium.getHardness());
        assertEquals(-1, medium.getMobilityPenalty());
        assertEquals(5, medium.getAttunement());

        Armor heavy =
                new Armor(
                        "ArtPlate", "ArtPlate", Armor.ArmorType.ARTIFACT, Armor.ArmorWeight.HEAVY);
        assertEquals(11, heavy.getSoak());
        assertEquals(10, heavy.getHardness());
        assertEquals(-2, heavy.getMobilityPenalty());
        assertEquals(6, heavy.getAttunement());
    }

    @Test
    void testStatUpdatesOnWeightChange() {
        Armor armor = new Armor("Test", "Test", Armor.ArmorType.MORTAL, Armor.ArmorWeight.LIGHT);
        assertEquals(3, armor.getSoak());

        armor.setWeight(Armor.ArmorWeight.HEAVY);
        assertEquals(7, armor.getSoak());
        assertEquals(-2, armor.getMobilityPenalty());

        armor.setType(Armor.ArmorType.ARTIFACT);
        assertEquals(11, armor.getSoak());
        assertEquals(10, armor.getHardness());
    }
}
