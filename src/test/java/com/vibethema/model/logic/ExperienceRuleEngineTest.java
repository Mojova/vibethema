package com.vibethema.model.logic;

import com.vibethema.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

public class ExperienceRuleEngineTest {
    private static final Logger logger = LoggerFactory.getLogger(ExperienceRuleEngineTest.class);

    private CharacterData data;
    private CharacterSaveState snapshot;

    @BeforeEach
    void setUp() {
        data = new CharacterData();
        snapshot = new CharacterSaveState();
        
        // Initialize basic snapshot structure to prevent NPEs
        snapshot.attributes = new java.util.HashMap<>();
        snapshot.abilities = new java.util.HashMap<>();
        snapshot.merits = new java.util.HashMap<>();
        snapshot.crafts = new java.util.ArrayList<>();
        snapshot.martialArts = new java.util.ArrayList<>();
        snapshot.specialties = new java.util.ArrayList<>();
        snapshot.unlockedCharms = new java.util.ArrayList<>();
        snapshot.spells = new java.util.ArrayList<>();
        snapshot.willpower = 5;
    }

    @Test
    void testNoSnapshotReturnsEmptyStatus() {
        data.getXpAwards().add(new XpAward("Init", 10, false));
        ExperienceRuleEngine.ExperienceStatus status = ExperienceRuleEngine.calculateStatus(data, null);
        
        assertEquals(10, status.totalRegularXpAwarded);
        assertEquals(0, status.regularXpSpent);
        assertTrue(status.log.isEmpty());
    }

    @Test
    void testAttributeCost() {
        snapshot.attributes.put(Attribute.STRENGTH.name(), 2);
        data.getAttribute(Attribute.STRENGTH).set(4);

        ExperienceRuleEngine.ExperienceStatus status = ExperienceRuleEngine.calculateStatus(data, snapshot);
        
        // 2 -> 3 (cost 8), 3 -> 4 (cost 12) = Total 20
        for (ExperiencePurchase p : status.log) {
            logger.info("PURCHASE: {} : {}", p.getDescription(), p.getCost());
        }
        assertEquals(20, status.regularXpSpent);
        assertEquals(2, status.log.size());
    }

    @Test
    void testFavoredAbilityCost() {
        snapshot.abilities.put(Ability.AWARENESS.name(), 1);
        data.getAbility(Ability.AWARENESS).set(3);
        data.getFavoredAbility(Ability.AWARENESS).set(true);

        ExperienceRuleEngine.ExperienceStatus status = ExperienceRuleEngine.calculateStatus(data, snapshot);
        
        // 1 -> 2 (cost 1*2 - 1 = 1), 2 -> 3 (cost 2*2 - 1 = 3) = Total 4
        assertEquals(4, status.regularXpSpent);
    }

    @Test
    void testNonFavoredAbilityCost() {
        snapshot.abilities.put(Ability.STEALTH.name(), 2);
        data.getAbility(Ability.STEALTH).set(4);
        data.getFavoredAbility(Ability.STEALTH).set(false);
        data.getCasteAbility(Ability.STEALTH).set(false);

        ExperienceRuleEngine.ExperienceStatus status = ExperienceRuleEngine.calculateStatus(data, snapshot);
        
        // 2 -> 3 (cost 4), 3 -> 4 (cost 6) = Total 10
        assertEquals(10, status.regularXpSpent);
    }

    @Test
    void testNewAbilityCost() {
        // Not in snapshot (so 0), data is 1
        data.getAbility(Ability.LARCENY).set(1);
        
        ExperienceRuleEngine.ExperienceStatus status = ExperienceRuleEngine.calculateStatus(data, snapshot);
        
        // New ability costs 3
        assertEquals(3, status.regularXpSpent);
    }

    @Test
    void testMeritCost() {
        snapshot.merits.put("Resources", 2);
        data.getMerits().add(new Merit("Resources", 4));

        ExperienceRuleEngine.ExperienceStatus status = ExperienceRuleEngine.calculateStatus(data, snapshot);
        
        // 2 -> 3 (cost 9), 3 -> 4 (cost 12) = Total 21
        assertEquals(21, status.regularXpSpent);
    }

    @Test
    void testSolarCharmCostAndTypeRouting() {
        // Solar charm (ability matches core enum)
        data.getUnlockedCharms().add(new PurchasedCharm("charm1", "Dipping Swallow Defense", "Melee"));
        data.getFavoredAbility(Ability.MELEE).set(true); // Favored -> 8
        
        data.getUnlockedCharms().add(new PurchasedCharm("charm2", "Peony Blade", "Melee")); // Non-favored would be 10, but since Melee is favored it's 8.
        
        data.getUnlockedCharms().add(new PurchasedCharm("charm3", "Some Archery Charm", "Archery"));
        data.getFavoredAbility(Ability.ARCHERY).set(false); // Non-favored -> 10

        // Give them 100 regular and 100 solar XP
        data.getXpAwards().add(new XpAward("Reg", 100, false));
        data.getXpAwards().add(new XpAward("Sol", 100, true));

        ExperienceRuleEngine.ExperienceStatus status = ExperienceRuleEngine.calculateStatus(data, snapshot);

        // Charms belong to core abilities, so they are solar charms and CANNOT use Solar XP!
        // Total cost: 8 + 8 + 10 = 26
        assertEquals(26, status.regularXpSpent);
        assertEquals(0, status.solarXpSpent); // None could use solar xp
        assertEquals(3, status.log.size());
    }

    @Test
    void testMartialArtsAndEvocationSolarXpRouting() {
        // Evocation
        data.getUnlockedCharms().add(new PurchasedCharm("charm1", "Volcano Cutter", "Evocation")); // Cost 10
        
        // MA Charm
        data.getMartialArtsStyles().add(new MartialArtsStyle("id", "Snake Style", 1));
        data.getMartialArtsStyles().get(0).setCaste(true); 
        data.getUnlockedCharms().add(new PurchasedCharm("charm2", "Striking Cobra Technique", "Snake Style")); // Favored -> Cost 8

        // Give 15 Solar XP. Costs are 21.
        data.getXpAwards().add(new XpAward("Sol", 15, true));

        ExperienceRuleEngine.ExperienceStatus status = ExperienceRuleEngine.calculateStatus(data, snapshot);
        
        // Total cost 21. They can all use Solar XP. 
        // 15 is eaten by Solar pool, remaining 6 spills over to Regular pool.
        assertEquals(15, status.solarXpSpent);
        assertEquals(6, status.regularXpSpent);
    }
}
