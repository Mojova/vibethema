package com.vibethema.model;

import static org.junit.jupiter.api.Assertions.*;

import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.social.*;
import com.vibethema.model.traits.*;
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

        data.getSpecialties()
                .add(new Specialty("1", "Shadow Dancing", Ability.DODGE.getDisplayName()));

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
    void testSupernalAbilityNotClearedOnAddition() {
        data.getCasteAbility(Ability.MELEE).set(true);
        data.supernalAbilityProperty().set(Ability.MELEE.getDisplayName());
        assertEquals(Ability.MELEE.getDisplayName(), data.supernalAbilityProperty().get());

        // Adding ANOTHER caste ability should NOT clear the supernal one
        data.getCasteAbility(Ability.ARCHERY).set(true);
        assertEquals(
                Ability.MELEE.getDisplayName(),
                data.supernalAbilityProperty().get(),
                "Supernal ability should not be cleared when adding a new caste ability");
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
    void testImportStateRestoresSupernalAndCaste() {
        // Setup initial state (e.g. Zenit-style)
        data.getCasteAbility(Ability.ATHLETICS).set(true);
        data.supernalAbilityProperty().set("Athletics");

        CharacterSaveState state = new CharacterSaveState();
        state.caste = "DAWN";
        state.supernalAbility = "WAR";
        state.casteAbilities = java.util.List.of("WAR");
        state.abilities = new java.util.HashMap<>();
        state.abilities.put("WAR", 5);

        data.importState(state, null);

        assertEquals(Caste.DAWN, data.casteProperty().get());
        assertTrue(data.getCasteAbility(Ability.WAR).get());
        assertEquals(
                Ability.WAR.getDisplayName(),
                data.supernalAbilityProperty().get(),
                "Supernal ability should be correctly restored when importing over existing data");
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
        AttackPoolData apd =
                data.getAttackPools().stream()
                        .filter(p -> p.getWeaponName().equals("Sword"))
                        .findFirst()
                        .orElseThrow();

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

        assertEquals(
                5, data.getAttackPools().get(data.getAttackPools().size() - 1).getDecisivePool());

        data.getAttribute(Attribute.DEXTERITY).set(4);
        assertEquals(
                6, data.getAttackPools().get(data.getAttackPools().size() - 1).getDecisivePool());

        data.getAbility(Ability.MELEE).set(5);
        assertEquals(
                9, data.getAttackPools().get(data.getAttackPools().size() - 1).getDecisivePool());
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

    @Test
    void testMoteCalculation() {
        // Base Essence 1: Personal 13, Peripheral 33
        assertEquals(13, data.personalMotesProperty().get());
        assertEquals(33, data.peripheralMotesProperty().get());

        data.essenceProperty().set(3);
        // Essence 3: Personal (3*3)+10=19, Peripheral (3*7)+26=47
        assertEquals(19, data.personalMotesProperty().get());
        assertEquals(47, data.peripheralMotesProperty().get());
    }

    @Test
    void testHealthLevelCalculation() {
        // Base health levels: -0, -1, -1, -2, -2, -4, Incap
        assertTrue(data.healthLevelsProperty().contains("-0"));
        assertEquals(7, data.healthLevelsProperty().size());

        // Add Ox-Body Technique (simulated)
        // Ox-Body ID calculation
        String oxBodyId =
                java.util
                        .UUID
                        .nameUUIDFromBytes(
                                ("Ox-Body Technique" + "|" + Ability.RESISTANCE.getDisplayName())
                                        .getBytes(java.nio.charset.StandardCharsets.UTF_8))
                        .toString();

        data.addCharm(
                new PurchasedCharm(
                        oxBodyId, "Ox-Body Technique", Ability.RESISTANCE.getDisplayName()));

        // Stamina 1: Ox-Body adds one -1 and one -2
        // Total should be 9 levels
        assertEquals(9, data.healthLevelsProperty().size());

        long zeroCount = data.healthLevelsProperty().stream().filter(l -> l.equals("-0")).count();
        long oneCount = data.healthLevelsProperty().stream().filter(l -> l.equals("-1")).count();
        long twoCount = data.healthLevelsProperty().stream().filter(l -> l.equals("-2")).count();

        assertEquals(1, zeroCount);
        assertEquals(3, oneCount);
        assertEquals(3, twoCount);

        // Increase Stamina to 3
        data.getAttribute(Attribute.STAMINA).set(3);
        // Stamina 3: Ox-Body adds one -1 and two -2
        assertEquals(10, data.healthLevelsProperty().size());
        assertEquals(1, data.healthLevelsProperty().stream().filter(l -> l.equals("-0")).count());
        assertEquals(3, data.healthLevelsProperty().stream().filter(l -> l.equals("-1")).count());
        assertEquals(4, data.healthLevelsProperty().stream().filter(l -> l.equals("-2")).count());
    }

    @Test
    void testCasteFavoredMutualExclusivity() {
        data.getFavoredAbility(Ability.ARCHERY).set(true);
        assertTrue(data.getFavoredAbility(Ability.ARCHERY).get());
        assertFalse(data.getCasteAbility(Ability.ARCHERY).get());

        // Setting caste should untoggle favored
        data.getCasteAbility(Ability.ARCHERY).set(true);
        assertTrue(data.getCasteAbility(Ability.ARCHERY).get());
        assertFalse(data.getFavoredAbility(Ability.ARCHERY).get());

        // Setting favored should untoggle caste
        data.getFavoredAbility(Ability.ARCHERY).set(true);
        assertTrue(data.getFavoredAbility(Ability.ARCHERY).get());
        assertFalse(data.getCasteAbility(Ability.ARCHERY).get());
    }

    @Test
    void testReactiveExcellencyProperty() {
        // Initially no excellency
        assertFalse(data.excellencyProperty(Ability.ARCHERY).get());

        // Favored + Dot > 0 should grant excellency
        data.getFavoredAbility(Ability.ARCHERY).set(true);
        data.getAbility(Ability.ARCHERY).set(1);
        assertTrue(data.excellencyProperty(Ability.ARCHERY).get());

        // Setting back to 0 dots should remove it
        data.getAbility(Ability.ARCHERY).set(0);
        assertFalse(data.excellencyProperty(Ability.ARCHERY).get());

        // Adding a charm should also grant it
        data.getUnlockedCharms()
                .add(new PurchasedCharm("1", "Any Charm", Ability.ARCHERY.getDisplayName()));
        assertTrue(data.excellencyProperty(Ability.ARCHERY).get());
    }
}
