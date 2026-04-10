package com.vibethema.model.combat;

import static org.junit.jupiter.api.Assertions.*;

import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.traits.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CombatStatsModelTest {
    private TraitModel traitModel;
    private EquipmentModel equipmentModel;
    private MysticModel mysticModel;
    private CombatStatsModel model;

    @BeforeEach
    void setUp() {
        traitModel = new TraitModel(v -> {}, () -> {});
        equipmentModel = new EquipmentModel(v -> {}, () -> {});
        mysticModel = new MysticModel(v -> {}, () -> {});
        model = new CombatStatsModel(traitModel, equipmentModel, mysticModel);
    }

    @Test
    void testEvasionCalculation() {
        traitModel.getAttribute(Attribute.DEXTERITY).set(3);
        traitModel.getAbility(Ability.DODGE).set(2);

        model.updateDerivedStats();
        // Base: (3+2)/2 = 2.5 -> 3
        assertEquals(3, model.evasionProperty().get());
    }

    @Test
    void testSoakCalculation() {
        traitModel.getAttribute(Attribute.STAMINA).set(4);

        Armor armor = new Armor("Armor");
        armor.setType(Armor.ArmorType.MORTAL);
        armor.setWeight(Armor.ArmorWeight.LIGHT); // Soak 3
        armor.setEquipped(true);
        equipmentModel.getArmors().add(armor);

        model.updateCombatStats();
        assertEquals(4, model.naturalSoakProperty().get());
        assertEquals(7, model.totalSoakProperty().get()); // 4 + 3
    }

    @Test
    void testMoteCalculation() {
        model.updateMotesAndHealth(3); // Essence 3
        assertEquals(19, model.personalMotesProperty().get());
        assertEquals(47, model.peripheralMotesProperty().get());
    }

    @Test
    void testHealthLevelCalculation() {
        traitModel.getAttribute(Attribute.STAMINA).set(3);

        // Add Ox-Body Technique
        mysticModel
                .getUnlockedCharms()
                .add(
                        new PurchasedCharm(
                                "1", "Shadow Dancing", Ability.DODGE.getDisplayName())); // Dummy
        mysticModel
                .getUnlockedCharms()
                .add(
                        new PurchasedCharm(
                                "2", "Ox-Body Technique", Ability.RESISTANCE.getDisplayName()));

        model.updateMotesAndHealth(1);
        // Base 7 + Ox-Body (Stamina 3: adds one -1 and two -2) = 10
        assertEquals(10, model.healthLevelsProperty().size());
    }
}
