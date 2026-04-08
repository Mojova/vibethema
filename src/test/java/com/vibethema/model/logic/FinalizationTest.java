package com.vibethema.model.logic;

import static org.junit.jupiter.api.Assertions.*;

import com.vibethema.model.*;
import com.vibethema.model.mystic.PurchasedCharm;
import com.vibethema.model.traits.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FinalizationTest {

    private CharacterData data;

    @BeforeEach
    void setUp() {
        data = new CharacterData();
        data.setDirty(false); // Clean start
    }

    @Test
    void testInitialStateNotReady() {
        CreationRuleEngine.CreationStatus status = CreationRuleEngine.calculateStatus(data);
        assertFalse(status.isReadyToFinalize, "New character should not be ready to finalize");
    }

    @Test
    void testReadyForFinalize() {
        // 1. Attributes: 8/6/4 distribution (18 dots above 1)
        data.getAttribute(Attribute.STRENGTH).set(4);
        data.getAttribute(Attribute.DEXTERITY).set(4);
        data.getAttribute(Attribute.STAMINA).set(3);
        data.getAttribute(Attribute.CHARISMA).set(3);
        data.getAttribute(Attribute.MANIPULATION).set(3);
        data.getAttribute(Attribute.APPEARANCE).set(3);
        data.getAttribute(Attribute.PERCEPTION).set(3);
        data.getAttribute(Attribute.INTELLIGENCE).set(2);
        data.getAttribute(Attribute.WITS)
                .set(1); // Sum: (3+3+2) + (2+2+2) + (2+1+0) = 8 + 6 + 3 = 17.
        // Let's adjust to exactly 8/6/4.
        data.getAttribute(Attribute.WITS).set(2); // Sum: 8 + 6 + 4 = 18 dots above 1.

        // 2. Abilities: 28 dots
        for (int i = 0; i < 9; i++) {
            data.getAbility(SystemData.ABILITIES.get(i)).set(3); // 27 dots
        }
        data.getAbility(SystemData.ABILITIES.get(9)).set(1); // 28 dots total

        // 3. Merits: 10 dots
        data.getMerits().clear();
        data.getMerits().add(new Merit("Merit 1", 5));
        data.getMerits().add(new Merit("Merit 2", 5));

        // 4. Specialties: 4
        data.getSpecialties().clear();
        data.getSpecialties().add(new Specialty("1", "S1", "Melee"));
        data.getSpecialties().add(new Specialty("2", "S2", "Melee"));
        data.getSpecialties().add(new Specialty("3", "S3", "Melee"));
        data.getSpecialties().add(new Specialty("4", "S4", "Melee"));

        // 5. Charms: 15
        data.getUnlockedCharms().clear();
        for (int i = 0; i < 15; i++) {
            data.getUnlockedCharms().add(new PurchasedCharm("C" + i, "Charm " + i, "Melee"));
        }

        // 6. Caste/Favored: 5 each
        data.casteProperty().set(Caste.DAWN);
        // Dawn caste abilities: Archery, Brawl, Melee, Thrown, War
        data.getCasteAbility(Ability.ARCHERY).set(true);
        data.getCasteAbility(Ability.BRAWL).set(true);
        data.getCasteAbility(Ability.MELEE).set(true);
        data.getCasteAbility(Ability.THROWN).set(true);
        data.getCasteAbility(Ability.WAR).set(true);

        data.getFavoredAbility(Ability.PRESENCE).set(true);
        data.getFavoredAbility(Ability.RESISTANCE).set(true);
        data.getFavoredAbility(Ability.SURVIVAL).set(true);
        data.getFavoredAbility(Ability.INVESTIGATION).set(true);
        data.getFavoredAbility(Ability.AWARENESS).set(true);

        // 7. Bonus Points: Exactly 15
        // Currently spent: 0.
        // Let's spend 15 BP.
        // Attributes: Willpower increase from 5 to 7 (costs 4 BP)
        data.willpowerProperty().set(7); // 4 BP
        // Abilities: Raising an 11th ability to 3 (non-favored) would cost dots then BP.
        // Wait, raising dots above the 28 pool costs BP.
        data.getAbility(SystemData.ABILITIES.get(10))
                .set(3); // +3 dots. Non-favored cost is 2 BP per dot. 3*2 = 6 BP.
        // Total so far: 4 + 6 = 10 BP.
        // Raising a 12th ability to 2 (favored) costs 1 BP per dot. +5.
        data.getAbility(SystemData.ABILITIES.get(11))
                .set(5); // +5 dots. Favored cost is 1 BP per dot. Total pool is now > 28.
        // This math is complex because of how the engine sorts pools.
        // Let's just use Merit dots which are 1 BP each.
        data.getMerits().add(new Merit("BP Merit", 5)); // +5 BP
        // Total: 4 (WP) + 6 (Abil) + 5 (Merit) = 15 BP?
        // Let's be simpler.
        data.willpowerProperty().set(5);
        data.getAbility(SystemData.ABILITIES.get(10)).set(0);
        data.getAbility(SystemData.ABILITIES.get(11)).set(0);
        data.getMerits().clear();
        data.getMerits().add(new Merit("M1", 10)); // Free
        data.getMerits().add(new Merit("M2", 15)); // 15 dots spent above 10 = 15 BP.

        // 8. Attribute Priorities: Required for finalization
        data.getAttributePriority(Attribute.Category.PHYSICAL).set(AttributePriority.PRIMARY);
        data.getAttributePriority(Attribute.Category.SOCIAL).set(AttributePriority.SECONDARY);
        data.getAttributePriority(Attribute.Category.MENTAL).set(AttributePriority.TERTIARY);

        CreationRuleEngine.CreationStatus status = CreationRuleEngine.calculateStatus(data);
        assertEquals(15, status.bonusPointsSpent, "Should have 15 BP spent");
        assertEquals(18, status.attributesSpent, "Should have 18 attribute dots above 1");
        assertEquals(28, status.abilitiesSpent, "Should have 28 ability dots");
        assertEquals(25, status.meritsSpent, "Should have 25 merit dots (10 free + 15 billable)");
        assertEquals(4, status.specialtiesSpent, "Should have 4 specialties");
        assertEquals(15, status.charmsSpent, "Should have 15 charms");
        assertEquals(5, status.casteAbilities, "Should have 5 caste abilities");
        assertEquals(5, status.favoredAbilities, "Should have 5 favored abilities");

        assertTrue(
                status.isReadyToFinalize,
                "Character should be ready to finalize when all points are spent");
    }

    @Test
    void testNotReadyIfUnderPoints() {
        // Set all but one requirement
        data.getAttribute(Attribute.STRENGTH).set(4);
        data.getAttribute(Attribute.DEXTERITY).set(4);
        data.getAttribute(Attribute.STAMINA).set(3);
        data.getAttribute(Attribute.CHARISMA).set(3);
        data.getAttribute(Attribute.MANIPULATION).set(3);
        data.getAttribute(Attribute.APPEARANCE).set(3);
        data.getAttribute(Attribute.PERCEPTION).set(3);
        data.getAttribute(Attribute.INTELLIGENCE).set(2);
        data.getAttribute(Attribute.WITS).set(2); // 18 dots

        for (int i = 0; i < 9; i++) data.getAbility(SystemData.ABILITIES.get(i)).set(3);
        data.getAbility(SystemData.ABILITIES.get(9)).set(1); // 28 dots

        data.getMerits().add(new Merit("M1", 10)); // 10 dots

        data.getSpecialties().add(new Specialty("1", "S1", "Melee"));
        data.getSpecialties().add(new Specialty("2", "S2", "Melee"));
        data.getSpecialties().add(new Specialty("3", "S3", "Melee"));
        data.getSpecialties().add(new Specialty("4", "S4", "Melee")); // 4 specialties

        for (int i = 0; i < 15; i++)
            data.getUnlockedCharms()
                    .add(new PurchasedCharm("C" + i, "Charm " + i, "Melee")); // 15 charms

        data.casteProperty().set(Caste.DAWN);
        data.getCasteAbility(Ability.ARCHERY).set(true);
        data.getCasteAbility(Ability.BRAWL).set(true);
        data.getCasteAbility(Ability.MELEE).set(true);
        data.getCasteAbility(Ability.THROWN).set(true);
        data.getCasteAbility(Ability.WAR).set(true);

        data.getFavoredAbility(Ability.PRESENCE).set(true);
        data.getFavoredAbility(Ability.RESISTANCE).set(true);
        data.getFavoredAbility(Ability.SURVIVAL).set(true);
        data.getFavoredAbility(Ability.INVESTIGATION).set(true);
        data.getFavoredAbility(Ability.AWARENESS).set(true); // 5/5

        // 8. Attribute Priorities: Required for finalization
        data.getAttributePriority(Attribute.Category.PHYSICAL).set(AttributePriority.PRIMARY);
        data.getAttributePriority(Attribute.Category.SOCIAL).set(AttributePriority.SECONDARY);
        data.getAttributePriority(Attribute.Category.MENTAL).set(AttributePriority.TERTIARY);

        // 0 BP spent. Not 15.
        CreationRuleEngine.CreationStatus status = CreationRuleEngine.calculateStatus(data);
        assertFalse(status.isReadyToFinalize, "Should not be ready with 0 BP spent");
    }
}
