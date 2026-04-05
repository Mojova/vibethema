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
    void testSupernalAbilityOptions() {
        // Initially no caste abilities, so only "" should be in options
        assertEquals(1, data.getValidSupernalAbilities().size());
        assertTrue(data.getValidSupernalAbilities().contains(""));

        // Add a caste ability
        data.getCasteAbility(Ability.ARCHERY).set(true);
        assertEquals(2, data.getValidSupernalAbilities().size());
        assertTrue(data.getValidSupernalAbilities().contains(Ability.ARCHERY.getDisplayName()));

        // Remove it
        data.getCasteAbility(Ability.ARCHERY).set(false);
        assertEquals(1, data.getValidSupernalAbilities().size());
    }

    @Test
    void testSupernalAbilityReset() {
        data.getCasteAbility(Ability.MELEE).set(true);
        data.supernalAbilityProperty().set(Ability.MELEE.getDisplayName());
        assertEquals(Ability.MELEE.getDisplayName(), data.supernalAbilityProperty().get());

        // Unset caste ability should reset supernal
        data.getCasteAbility(Ability.MELEE).set(false);
        assertEquals("", data.supernalAbilityProperty().get());
    }

    @Test
    void testAttackPoolCalculation() {
        data.getAttribute(Attribute.DEXTERITY).set(3);
        data.getAttribute(Attribute.STRENGTH).set(2);
        data.getAbility(Ability.MELEE).set(4);
        
        Weapon sword = new Weapon("Sword");
        sword.setRange(Weapon.WeaponRange.CLOSE);
        sword.setAccuracy(2);
        sword.setDamage(3);
        sword.setDefense(1);
        data.getWeapons().add(sword);
        
        // Find the attack pool data for the sword
        AttackPoolData apd = data.getAttackPools().stream()
                .filter(p -> p.getWeaponName().equals("Sword"))
                .findFirst().orElseThrow();
        
        // Dex 3 + Melee 4 + Accuracy 2 = 9
        assertEquals("9", apd.getWitheringPool());
        // Dex 3 + Melee 4 = 7
        assertEquals(7, apd.getDecisivePool());
        // Str 2 + Damage 3 = 5
        assertEquals(5, apd.getDamage());
        // ceil((Dex 3 + Melee 4) / 2) + Def 1 = 4 + 1 = 5
        assertEquals(5, apd.getParry());
    }

    @Test
    void testAttackPoolReactiveUpdate() {
        data.getAttribute(Attribute.DEXTERITY).set(3);
        data.getAbility(Ability.MELEE).set(2);
        
        Weapon sword = new Weapon("Sword");
        sword.setRange(Weapon.WeaponRange.CLOSE);
        data.getWeapons().add(sword);
        
        assertEquals(5, data.getAttackPools().get(data.getAttackPools().size()-1).getDecisivePool());
        
        data.getAttribute(Attribute.DEXTERITY).set(4);
        assertEquals(6, data.getAttackPools().get(data.getAttackPools().size()-1).getDecisivePool());
        
        data.getAbility(Ability.MELEE).set(5);
        assertEquals(9, data.getAttackPools().get(data.getAttackPools().size()-1).getDecisivePool());
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
