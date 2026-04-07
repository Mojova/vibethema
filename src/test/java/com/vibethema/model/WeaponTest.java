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

public class WeaponTest {

    @Test
    void testEquippedProperty() {
        Weapon weapon = new Weapon("Test Weapon");
        assertFalse(weapon.isEquipped());

        weapon.setEquipped(true);
        assertTrue(weapon.isEquipped());

        weapon.equippedProperty().set(false);
        assertFalse(weapon.isEquipped());
    }

    @Test
    void testWeaponInitialization() {
        Weapon weapon =
                new Weapon(
                        "id-123",
                        "Super Sword",
                        Weapon.WeaponRange.CLOSE,
                        Weapon.WeaponType.ARTIFACT,
                        Weapon.WeaponCategory.HEAVY);
        assertEquals("id-123", weapon.getId());
        assertEquals("Super Sword", weapon.getName());
        assertEquals(Weapon.WeaponType.ARTIFACT, weapon.getType());
        assertFalse(weapon.isEquipped());
    }
}
