package com.vibethema.model;

import static org.junit.jupiter.api.Assertions.*;

import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;
import com.vibethema.model.traits.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AttackPoolCalculationTest {

    private CharacterData data;

    @BeforeEach
    void setUp() {
        data = new CharacterData();
        // Clear default "Unarmed" if it was added (to have a clean slate)
        data.getWeapons().clear();
    }

    @Test
    void testMeleeAttackCalculation() {
        data.getAttribute(Attribute.DEXTERITY).set(3);
        data.getAttribute(Attribute.STRENGTH).set(3);
        data.getAbility(Ability.MELEE).set(3);

        Weapon sword = new Weapon("Steel Sword");
        sword.setRange(Weapon.WeaponRange.CLOSE);
        sword.setAccuracy(2); // Withering accuracy
        sword.setDamage(4);
        sword.setDefense(1);
        data.getWeapons().add(sword);

        data.updateAttackPools();

        assertEquals(1, data.getAttackPools().size());
        AttackPoolData pool = data.getAttackPools().get(0);

        // Withering = Dex(3) + Melee(3) + Spec(0) + Acc(2) = 8
        assertEquals("8", pool.getWitheringPool());
        // Decisive = Dex(3) + Melee(3) + Spec(0) = 6
        assertEquals(6, pool.getDecisivePool());
        // Damage = Str(3) + WepDamage(4) = 7
        assertEquals(7, pool.getDamage());
        // Parry = ceil((Dex(3) + Melee(3)) / 2) + Def(1) = 3 + 1 = 4
        assertEquals(4, pool.getParry());
    }

    @Test
    void testArcheryAttackCalculation() {
        data.getAttribute(Attribute.DEXTERITY).set(4);
        data.getAbility(Ability.ARCHERY).set(2);

        Weapon bow = new Weapon("Longbow");
        bow.setRange(Weapon.WeaponRange.ARCHERY);
        bow.setType(Weapon.WeaponType.MORTAL);
        bow.setCategory(Weapon.WeaponCategory.MEDIUM);

        // These are automatically set by updateStats() in Weapon:
        // C:-2 | S:4 | M:2 | L:0 | E:-2

        data.getWeapons().add(bow);

        data.updateAttackPools();

        AttackPoolData pool = data.getAttackPools().get(0);
        // Base = Dex(4) + Archery(2) = 6
        // C: 6-2=4 | S: 6+4=10 | M: 6+2=8 | L: 6+0=6 | E: 6-2=4
        assertEquals("C:4 | S:10 | M:8 | L:6 | E:4", pool.getWitheringPool());
    }

    @Test
    void testEquippedStatusIgnoredByCalculation() {
        Weapon sword = new Weapon("Steel Sword");
        sword.setEquipped(false);
        data.getWeapons().add(sword);

        data.updateAttackPools();
        assertEquals(
                1,
                data.getAttackPools().size(),
                "Pools should be generated even for unequipped weapons");
    }
}
