package com.vibethema.model.traits;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.social.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.logic.*;


import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TraitModelTest {
    private TraitModel model;
    private AtomicBoolean dirtyFlag;
    private AtomicInteger updateCount;

    @BeforeEach
    void setUp() {
        dirtyFlag = new AtomicBoolean(false);
        updateCount = new AtomicInteger(0); model = new TraitModel(v -> dirtyFlag.set(true), () -> updateCount.incrementAndGet());
    }

    @Test
    void testAttributeInitialization() {
        assertEquals(1, model.getAttribute(Attribute.STRENGTH).get());
        assertEquals(1, model.getAttribute(Attribute.DEXTERITY).get());
    }

    @Test
    void testAttributeChangeTriggersDirty() {
        assertFalse(dirtyFlag.get());
        model.getAttribute(Attribute.STRENGTH).set(3);
        assertTrue(dirtyFlag.get());
    }

    @Test
    void testAbilityChangeTriggersUpdate() {
        assertEquals(0, updateCount.get());
        model.getAbility(Ability.ARCHERY).set(3);
        assertTrue(updateCount.get() > 0);
    }

    @Test
    void testCasteFavoredExclusivity() {
        model.getFavoredAbility(Ability.ARCHERY).set(true);
        assertTrue(model.getFavoredAbility(Ability.ARCHERY).get());
        assertFalse(model.getCasteAbility(Ability.ARCHERY).get());

        // Setting caste should untoggle favored
        model.getCasteAbility(Ability.ARCHERY).set(true);
        assertTrue(model.getCasteAbility(Ability.ARCHERY).get());
        assertFalse(model.getFavoredAbility(Ability.ARCHERY).get());

        // Setting favored should untoggle caste
        model.getFavoredAbility(Ability.ARCHERY).set(true);
        assertTrue(model.getFavoredAbility(Ability.ARCHERY).get());
        assertFalse(model.getCasteAbility(Ability.ARCHERY).get());
    }

    @Test
    void testCasteFavoredCounts() {
        assertEquals(0, model.casteAbilityCountProperty().get());
        assertEquals(0, model.favoredAbilityCountProperty().get());

        model.getCasteAbility(Ability.ARCHERY).set(true);
        model.getCasteAbility(Ability.MELEE).set(true);
        assertEquals(2, model.casteAbilityCountProperty().get());

        model.getFavoredAbility(Ability.DODGE).set(true);
        assertEquals(1, model.favoredAbilityCountProperty().get());
    }

    @Test
    void testBrawlMartialArtsPooling() {
        // Brawl and Martial Arts should count as one for Caste/Favored limits
        // And they are now "tied", so setting one sets the other.
        model.getCasteAbility(Ability.BRAWL).set(true);
        assertTrue(model.getCasteAbility(Ability.MARTIAL_ARTS).get(), "Martial Arts should be tied to Brawl");
        assertEquals(1, model.casteAbilityCountProperty().get());

        model.getCasteAbility(Ability.BRAWL).set(false);
        assertFalse(model.getCasteAbility(Ability.MARTIAL_ARTS).get());
        assertEquals(0, model.casteAbilityCountProperty().get());
        
        model.getFavoredAbility(Ability.BRAWL).set(true);
        assertTrue(model.getFavoredAbility(Ability.MARTIAL_ARTS).get(), "Martial Arts should be tied to Brawl");
        assertEquals(1, model.favoredAbilityCountProperty().get());

        // Unsetting Brawl should also unset Martial Arts and reduce count
        model.getFavoredAbility(Ability.BRAWL).set(false);
        assertEquals(0, model.favoredAbilityCountProperty().get());
        assertFalse(model.getFavoredAbility(Ability.MARTIAL_ARTS).get(), "Martial Arts should be tied to Brawl");
    }

    @Test
    void testSpecialtyTriggersUpdate() {
        int initial = updateCount.get();
        model.getSpecialties().add(new Specialty("1", "Fast Talk", Ability.SOCIALIZE.getDisplayName()));
        assertTrue(updateCount.get() > initial);
    }
}