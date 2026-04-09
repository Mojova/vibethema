package com.vibethema.model.traits;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class SpecialtyTest {

    @Test
    void testSpecialtyConstruction() {
        Specialty specialty = new Specialty("Sword", "Melee");
        assertEquals("Sword", specialty.getName());
        assertEquals("Melee", specialty.getAbility());
        assertNotNull(specialty.getId());
        assertFalse(specialty.getId().isEmpty());
    }

    @Test
    void testSpecialtyWithExplicitId() {
        String id = "test-uuid";
        Specialty specialty = new Specialty(id, "Bow", "Archery");
        assertEquals(id, specialty.getId());
        assertEquals("Bow", specialty.getName());
        assertEquals("Archery", specialty.getAbility());
    }

    @Test
    void testProperties() {
        Specialty specialty = new Specialty("Sword", "Melee");
        specialty.nameProperty().set("Blade");
        assertEquals("Blade", specialty.getName());
        
        specialty.abilityProperty().set("Brawl");
        assertEquals("Brawl", specialty.getAbility());
    }
}
