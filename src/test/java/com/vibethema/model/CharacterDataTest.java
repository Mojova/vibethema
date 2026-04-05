package com.vibethema.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CharacterDataTest {
    private CharacterData data;

    @BeforeEach
    void setUp() {
        data = new CharacterData();
    }

    @Test
    void testEvasionCalculation() {
        // Base: (Dex 1 + Dodge 0) / 2 = 0.5 -> 1
        assertEquals(1, data.evasionProperty().get());

        data.getAttribute(Attribute.DEXTERITY).set(3);
        data.getAbility(Ability.DODGE).set(2);
        // (3 + 2) / 2 = 2.5 -> 3
        assertEquals(3, data.evasionProperty().get());
        assertFalse(data.hasEvasionSpecialtyProperty().get());
        assertEquals(0, data.evasionBonusProperty().get()); // (3+2+1)/2 - 3 = 0
    }

    @Test
    void testEvasionWithSpecialty() {
        data.getAttribute(Attribute.DEXTERITY).set(3);
        data.getAbility(Ability.DODGE).set(2);
        
        data.getSpecialties().add(new Specialty("1", "Shadow Dancing", Ability.DODGE.getDisplayName()));
        
        assertTrue(data.hasEvasionSpecialtyProperty().get());
        // Base was 3. Total with spec: (3+2+1)/2 = 3. Bonus is 0.
        assertEquals(3, data.evasionProperty().get());
        assertEquals(0, data.evasionBonusProperty().get());

        // Try with values that DO give a bonus
        data.getAttribute(Attribute.DEXTERITY).set(4);
        data.getAbility(Ability.DODGE).set(2);
        // Base: (4+2)/2 = 3
        // Total with spec: (4+2+1)/2 = 3.5 -> 4. Bonus is 1.
        assertEquals(3, data.evasionProperty().get());
        assertEquals(1, data.evasionBonusProperty().get());
    }

    @Test
    void testResolveCalculation() {
        // Base: (Wits 1 + Integrity 0) / 2 = 0.5 -> 1
        assertEquals(1, data.resolveProperty().get());

        data.getAttribute(Attribute.WITS).set(4);
        data.getAbility(Ability.INTEGRITY).set(3);
        // (4 + 3) / 2 = 3.5 -> 4
        assertEquals(4, data.resolveProperty().get());
    }

    @Test
    void testGuileCalculation() {
        // Base: (Manip 1 + Socialize 0) / 2 = 0.5 -> 1
        assertEquals(1, data.guileProperty().get());

        data.getAttribute(Attribute.MANIPULATION).set(2);
        data.getAbility(Ability.SOCIALIZE).set(4);
        // (2 + 4) / 2 = 3
        assertEquals(3, data.guileProperty().get());
    }

    @Test
    void testJoinBattleCalculation() {
        // Base: Wits 1 + Awareness 0 = 1
        assertEquals(1, data.joinBattleProperty().get());

        data.getAttribute(Attribute.WITS).set(4);
        data.getAbility(Ability.AWARENESS).set(3);
        assertEquals(7, data.joinBattleProperty().get());
    }

    @Test
    void testJoinBattleWithSpecialty() {
        data.getAttribute(Attribute.WITS).set(4);
        data.getAbility(Ability.AWARENESS).set(3);
        
        data.getSpecialties().add(new Specialty("2", "Ambush", Ability.AWARENESS.getDisplayName()));
        
        assertTrue(data.hasJoinBattleSpecialtyProperty().get());
        assertEquals(7, data.joinBattleProperty().get());
    }

    @Test
    void testReactiveUpdates() {
        data.getAttribute(Attribute.DEXTERITY).set(2);
        data.getAbility(Ability.DODGE).set(2);
        assertEquals(2, data.evasionProperty().get());

        data.getAttribute(Attribute.DEXTERITY).set(3);
        assertEquals(3, data.evasionProperty().get());

        data.getAbility(Ability.DODGE).set(3);
        assertEquals(3, data.evasionProperty().get());
        
        data.getAbility(Ability.DODGE).set(4);
        assertEquals(4, data.evasionProperty().get());
    }
}
