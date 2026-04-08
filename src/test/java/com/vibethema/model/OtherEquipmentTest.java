package com.vibethema.model;

import static org.junit.jupiter.api.Assertions.*;

import com.vibethema.model.equipment.*;
import org.junit.jupiter.api.Test;

public class OtherEquipmentTest {
    @Test
    public void testEquippedProperty() {
        OtherEquipment oe = new OtherEquipment("Grappling Hook", "Use to climb walls");
        assertFalse(oe.isEquipped());

        oe.setEquipped(true);
        assertTrue(oe.isEquipped());
    }

    @Test
    public void testDtoConversion() {
        OtherEquipment oe = new OtherEquipment("id-456", "Grappling Hook", "Use to climb walls");
        oe.setArtifact(true);
        oe.setEquipped(true);
        oe.setAttunement(4);

        OtherEquipment.OtherEquipmentData data = oe.toData();
        assertEquals("id-456", data.id);
        assertTrue(data.artifact);
        assertEquals(4, data.attunement);

        OtherEquipment loaded = OtherEquipment.fromData(data);
        assertEquals("id-456", loaded.getId());
        assertTrue(loaded.isArtifact());
        assertEquals(4, loaded.getAttunement());
        assertFalse(loaded.isEquipped()); // Should be false by default when loaded from global DB
    }
}
