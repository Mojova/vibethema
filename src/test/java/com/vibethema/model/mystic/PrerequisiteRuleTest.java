package com.vibethema.model.mystic;

import static org.junit.jupiter.api.Assertions.*;

import com.vibethema.model.CharacterData;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PrerequisiteRuleTest {
    @Test
    void abilityCountExcludesAwakeningItsRecursivePrerequisitesAndExcellencies() {
        CharacterData data = new CharacterData();
        data.addCharm(new PurchasedCharm("awakening", "Awakening Eye", "Awareness"));
        data.addCharm(new PurchasedCharm("awareness-1", "Keen Sight Technique", "Awareness"));
        data.addCharm(new PurchasedCharm("awareness-2", "Studied Ear Espial", "Awareness"));
        data.addCharm(new PurchasedCharm("sensory", "Sensory Acuity Prana", "Awareness"));
        data.addCharm(new PurchasedCharm("excellency", "Awareness Excellency", "Awareness"));
        data.addCharm(new PurchasedCharm("other", "Excellent Strike", "Melee"));
        data.addCharm(new PurchasedCharm("awareness-extra-1", "Knowing Beyond Silence", "Awareness"));

        PrerequisiteRule rule =
                PrerequisiteRule.countFromAbilities(
                        List.of("Awareness"), 3, List.of("awakening"), true);
        rule.setExcludedPrerequisitePaths(
                List.of(
                        List.of("awareness-1", "awareness-2", "sensory"),
                        List.of("awareness-1", "awareness-3", "sensory"),
                        List.of("awareness-2", "awareness-3", "sensory")));

        assertEquals(1, rule.getSatisfiedCount(data));
        assertFalse(rule.isSatisfied(data));
        HashSet<String> graphEdges = new HashSet<>();
        PrerequisiteRule eyePrerequisite =
                PrerequisiteRule.allOf(List.of(PrerequisiteRule.charm("awakening"), rule));
        eyePrerequisite.collectCharmIds(graphEdges);
        assertEquals(Set.of("awakening"), graphEdges);
        graphEdges.clear();
        rule.collectCharmIds(graphEdges);
        assertTrue(graphEdges.isEmpty(), "Count pools must not create individual charm web edges");

        data.addCharm(new PurchasedCharm("awareness-3", "Foe-Scenting Method", "Awareness"));
        assertEquals(2, rule.getSatisfiedCount(data), "The third optional Keen Charm counts as extra");
        data.addCharm(new PurchasedCharm("awareness-extra-2", "Living Pulse Perception", "Awareness"));
        assertEquals(3, rule.getSatisfiedCount(data));
        assertTrue(rule.isSatisfied(data));
    }
}
