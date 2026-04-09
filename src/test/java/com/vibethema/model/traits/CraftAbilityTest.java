package com.vibethema.model.traits;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

public class CraftAbilityTest {

    @Test
    void testUuidGeneration() {
        CraftAbility ca1 = new CraftAbility("Smithing", 3);
        CraftAbility ca2 = new CraftAbility("Cooking", 2);

        assertNotNull(ca1.getId());
        assertNotNull(ca2.getId());
        assertNotEquals(ca1.getId(), ca2.getId());
    }

    @Test
    void testManualUuidAssignment() {
        UUID manualId = UUID.randomUUID();
        CraftAbility ca = new CraftAbility(manualId, "Enchanting", 5);

        assertEquals(manualId, ca.getId());
        assertEquals("Enchanting", ca.getExpertise());
        assertEquals(5, ca.getRating());
    }

    @Test
    void testExpertiseAndRatingProperties() {
        CraftAbility ca = new CraftAbility("Alchemy", 1);
        ca.setExpertise("Thaumaturgy");
        ca.setRating(4);

        assertEquals("Thaumaturgy", ca.getExpertise());
        assertEquals(4, ca.getRating());
    }
}
