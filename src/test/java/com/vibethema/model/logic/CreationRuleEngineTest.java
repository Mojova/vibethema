package com.vibethema.model.logic;

import com.vibethema.model.*;
import com.vibethema.model.logic.CreationRuleEngine.CreationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CreationRuleEngineTest {

    private CharacterData data;

    @BeforeEach
    void setUp() {
        data = new CharacterData();
    }

    @Test
    void testSystemDataInitialization() {
        assertNotNull(SystemData.ATTRIBUTES, "SystemData.ATTRIBUTES should not be null");
        assertFalse(SystemData.ATTRIBUTES.isEmpty(), "SystemData.ATTRIBUTES should not be empty");
        assertTrue(SystemData.ATTRIBUTES.contains(Attribute.STRENGTH), "SystemData.ATTRIBUTES should contain Strength");
    }

    @Test
    void testInitialMotePools() {
        data.essenceProperty().set(1);
        CreationStatus status = CreationRuleEngine.calculateStatus(data);
        assertEquals(13, status.personalMotes);
        assertEquals(33, status.peripheralMotes);
    }

    @Test
    void testAttributeDistributionNoBP() {
        // Base is 1. Standard is 8/6/4 above base.
        // Physical: 4+4=8 dots above base (9 total Strength, Dexterity, Stamina combined sum of dots above 1)
        // Actually getAttributeTotal returns sum of (dot - 1).
        // Strengh=4 (3 above 1), Dex=4 (3 above 1), Stamina=3 (2 above 1). Total = 3+3+2 = 8.
        data.getAttribute(Attribute.STRENGTH).set(4);
        data.getAttribute(Attribute.DEXTERITY).set(4);
        data.getAttribute(Attribute.STAMINA).set(3);
        
        // Social: 1/3/3 dots above base (Total 6)
        data.getAttribute(Attribute.CHARISMA).set(2);
        data.getAttribute(Attribute.MANIPULATION).set(4);
        data.getAttribute(Attribute.APPEARANCE).set(3);
        
        // Mental: 2/2/1 dots above base (Total 4)
        data.getAttribute(Attribute.PERCEPTION).set(3);
        data.getAttribute(Attribute.INTELLIGENCE).set(2);
        data.getAttribute(Attribute.WITS).set(2);

        CreationStatus status = CreationRuleEngine.calculateStatus(data);
        assertEquals(0, status.bonusPointsSpent, "Standard 8/6/4 distribution should cost 0 BP");
    }

    @Test
    void testAbilityDotsNoBP() {
        // 28 free dots, none above 3.
        for (int i = 0; i < 7; i++) {
            Ability abil = SystemData.ABILITIES.get(i);
            data.getAbility(abil).set(3); // 7 * 3 = 21 dots
        }
        data.getAbility(SystemData.ABILITIES.get(7)).set(3); // 24 dots
        data.getAbility(SystemData.ABILITIES.get(8)).set(3); // 27 dots
        data.getAbility(SystemData.ABILITIES.get(9)).set(1); // 28 dots total

        CreationStatus status = CreationRuleEngine.calculateStatus(data);
        assertEquals(0, status.bonusPointsSpent, "28 dots at 3 or less should be 0 BP");
    }

    @Test
    void testAbilityDotsWithBP() {
        // 28 dots free. 1 dot at 4 costs 1 BP (favored) or 2 BP (non-favored).
        Ability abil = SystemData.ABILITIES.get(0);
        data.getAbility(abil).set(4); // 4 dots (3 free, 1 billable)
        data.getFavoredAbility(abil).set(true);
        
        // Fill 28 dots
        for (int i = 1; i < 10; i++) {
            data.getAbility(SystemData.ABILITIES.get(i)).set(3);
        }
        // Total dots = 4 + 9*3 = 4 + 27 = 31.
        // 28 are free. 3 dots are extra.
        // Wait, dots > 3 always cost BP.
        // The loop in engine says: if (i > 3) mustBp += costPerDot.
        // So 1 dot (the 4th) costs 1 BP immediately.
        // Plus 31 dots total - 28 free = 3 extra dots.
        
        CreationStatus status = CreationRuleEngine.calculateStatus(data);
        assertTrue(status.bonusPointsSpent > 0);
    }

    @Test
    void testOxBodyHealthLevels() {
        // Default health: -0, -1, -1, -2, -2, -4, Incap (7 levels)
        data.getAttribute(Attribute.STAMINA).set(3);
        CreationStatus status = CreationRuleEngine.calculateStatus(data);
        assertEquals(7, status.healthLevels.size());

        // Add 1 Ox-Body
        String oxBodyId = java.util.UUID.nameUUIDFromBytes(("Ox-Body Technique" + "|" + Ability.RESISTANCE.getDisplayName()).getBytes()).toString();
        data.getUnlockedCharms().add(new PurchasedCharm(oxBodyId, "Ox-Body Technique", Ability.RESISTANCE.getDisplayName()));
        
        status = CreationRuleEngine.calculateStatus(data);
        // At Stamina 3, Ox-Body adds -1, -2, -2 (3 levels)
        assertEquals(10, status.healthLevels.size());
        assertTrue(status.healthLevels.stream().anyMatch(l -> l.equals("-0")));
        assertTrue(status.healthLevels.stream().anyMatch(l -> l.equals("Incap")));
    }

    @Test
    void testWillpowerBP() {
        data.willpowerProperty().set(5);
        CreationStatus status = CreationRuleEngine.calculateStatus(data);
        assertEquals(0, status.bonusPointsSpent);

        data.willpowerProperty().set(7); // +2 willpower costs 4 BP
        status = CreationRuleEngine.calculateStatus(data);
        assertEquals(4, status.bonusPointsSpent);
    }

    @Test
    void testAttributePriorityManualSelection() {
        // Physical: 4+4+4=12 dots (Total 9 dots above 1)
        // Wait, 4+4+3=11 total. Dots above 1 are 3+3+2 = 8.
        data.getAttribute(Attribute.STRENGTH).set(4);
        data.getAttribute(Attribute.DEXTERITY).set(4);
        data.getAttribute(Attribute.STAMINA).set(3);
        
        // Social: 3+3+3=9 dots (Total 6 dots above 1)
        data.getAttribute(Attribute.CHARISMA).set(3);
        data.getAttribute(Attribute.MANIPULATION).set(3);
        data.getAttribute(Attribute.APPEARANCE).set(3);
        
        // Mental: 2+2+3=7 dots (Total 4 dots above 1)
        data.getAttribute(Attribute.PERCEPTION).set(2);
        data.getAttribute(Attribute.INTELLIGENCE).set(2);
        data.getAttribute(Attribute.WITS).set(3);

        // 1. No priority set -> BP should be 0 (auto-fit fallback)
        CreationStatus status = CreationRuleEngine.calculateStatus(data);
        assertEquals(0, status.bonusPointsSpent);
        assertFalse(status.allAttributePrioritiesSet);
        assertFalse(status.isReadyToFinalize);

        // 2. Optimal priority set: Phys=Primary(8), Soc=Secondary(6), Ment=Tertiary(4)
        data.getAttributePriority(Attribute.Category.PHYSICAL).set(AttributePriority.PRIMARY);
        data.getAttributePriority(Attribute.Category.SOCIAL).set(AttributePriority.SECONDARY);
        data.getAttributePriority(Attribute.Category.MENTAL).set(AttributePriority.TERTIARY);
        
        status = CreationRuleEngine.calculateStatus(data);
        assertEquals(0, status.bonusPointsSpent);
        assertTrue(status.allAttributePrioritiesSet);

        // 3. Sub-optimal priority set: Phys=Tertiary(4), Soc=Secondary(6), Ment=Primary(8)
        // Phys: 8 dots - 4 free = 4 extra. 4 * 3 BP = 12 BP. (Tertiary is 3 BP/dot above pool)
        // Soc: 6 dots - 6 free = 0 extra.
        // Ment: 4 dots - 8 free = 0 extra.
        // Total = 12 BP.
        data.getAttributePriority(Attribute.Category.PHYSICAL).set(AttributePriority.TERTIARY);
        data.getAttributePriority(Attribute.Category.SOCIAL).set(AttributePriority.SECONDARY);
        data.getAttributePriority(Attribute.Category.MENTAL).set(AttributePriority.PRIMARY);
        
        status = CreationRuleEngine.calculateStatus(data);
        assertEquals(12, status.bonusPointsSpent);
    }
}

