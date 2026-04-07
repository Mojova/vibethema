package com.vibethema.model;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.social.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.logic.*;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HearthstoneTest {
    @Test
    public void testEquippedProperty() {
        Hearthstone h = new Hearthstone("Stone of Luck", "Gives +1 to all rolls");
        assertFalse(h.isEquipped());
        
        h.setEquipped(true);
        assertTrue(h.isEquipped());
    }

    @Test
    public void testDtoConversion() {
        Hearthstone h = new Hearthstone("id-123", "Stone of Luck", "Gives +1 to all rolls");
        h.setEquipped(true);
        
        Hearthstone.HearthstoneData data = h.toData();
        assertEquals("id-123", data.id);
        assertEquals("Stone of Luck", data.name);
        // Note: data.equipped should NOT exist as it's character-specific
        
        Hearthstone loaded = Hearthstone.fromData(data);
        assertEquals("id-123", loaded.getId());
        assertEquals("Stone of Luck", loaded.getName());
        assertFalse(loaded.isEquipped()); // Should be false by default when loaded from global DB
    }
}