package com.vibethema.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class EquipmentInstanceTest {

    @Test
    public void testWeaponCloning() {
        Weapon w1 = new Weapon("template-123", "Short Sword", Weapon.WeaponRange.CLOSE, Weapon.WeaponType.MORTAL, Weapon.WeaponCategory.LIGHT);
        Weapon w2 = w1.copy();

        assertEquals(w1.getId(), w2.getId(), "Template IDs should match");
        assertNotEquals(w1.getInstanceId(), w2.getInstanceId(), "Instance IDs should be different");
        assertEquals(w1.getName(), w2.getName(), "Names should match");
        assertEquals(w1.getRange(), w2.getRange(), "Ranges should match");
    }

    @Test
    public void testArmorCloning() {
        Armor a1 = new Armor("armor-123", "Buff Jacket", Armor.ArmorType.MORTAL, Armor.ArmorWeight.LIGHT);
        Armor a2 = a1.copy();

        assertEquals(a1.getId(), a2.getId(), "Template IDs should match");
        assertNotEquals(a1.getInstanceId(), a2.getInstanceId(), "Instance IDs should be different");
    }

    @Test
    public void testHearthstoneCloning() {
        Hearthstone h1 = new Hearthstone("stone-123", "Seven Leaping Dragons Stone", "Does cool things");
        Hearthstone h2 = h1.copy();

        assertEquals(h1.getId(), h2.getId(), "Template IDs should match");
        assertNotEquals(h1.getInstanceId(), h2.getInstanceId(), "Instance IDs should be different");
    }

    @Test
    public void testOtherEquipmentCloning() {
        OtherEquipment o1 = new OtherEquipment("other-123", "Silk Shirt", "Fancy");
        OtherEquipment o2 = o1.copy();

        assertEquals(o1.getId(), o2.getId(), "Template IDs should match");
        assertNotEquals(o1.getInstanceId(), o2.getInstanceId(), "Instance IDs should be different");
    }
}
