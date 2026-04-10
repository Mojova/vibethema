package com.vibethema.model.equipment;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EquipmentModelTest {
    private EquipmentModel model;
    private AtomicBoolean dirtyFlag;
    private AtomicInteger updateCount;

    @BeforeEach
    void setUp() {
        dirtyFlag = new AtomicBoolean(false);
        updateCount = new AtomicInteger(0);
        model = new EquipmentModel(v -> dirtyFlag.set(true), () -> updateCount.incrementAndGet());
    }

    @Test
    void testEquipArmorSoak() {
        Armor plate = new Armor("Plate");
        plate.setType(Armor.ArmorType.MORTAL);
        plate.setWeight(Armor.ArmorWeight.MEDIUM); // Soak 5, Hardness 0
        model.getArmors().add(plate);

        assertFalse(plate.isEquipped());
        assertEquals(0, model.armorSoakProperty().get());

        plate.setEquipped(true);
        assertEquals(5, model.armorSoakProperty().get());
        assertEquals(0, model.totalHardnessProperty().get());
    }

    @Test
    void testArmorMutualExclusivity() {
        Armor plate = new Armor("Plate");
        plate.setEquipped(true);
        model.getArmors().add(plate);

        Armor mail = new Armor("Mail");
        model.getArmors().add(mail);

        assertTrue(plate.isEquipped());
        assertFalse(mail.isEquipped());

        mail.setEquipped(true);
        assertTrue(mail.isEquipped());
        assertFalse(plate.isEquipped());
    }

    @Test
    void testWeaponAdditionTriggersUpdate() {
        int initial = updateCount.get();
        model.getWeapons().add(new Weapon("Sword"));
        assertTrue(updateCount.get() > initial);
    }
}
