package com.vibethema.model.mystic;

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

public class MysticModelTest {
    private MysticModel model;
    private AtomicBoolean dirtyFlag;
    private AtomicInteger updateCount;

    @BeforeEach
    void setUp() {
        dirtyFlag = new AtomicBoolean(false);
        updateCount = new AtomicInteger(0);
        model = new MysticModel(v -> dirtyFlag.set(true), () -> updateCount.incrementAndGet());
    }

    @Test
    void testSorceryAvailability() {
        assertFalse(model.terrestrialSorceryAvailableProperty().get());

        // Add Terrestrial Circle Sorcery charm
        model.getUnlockedCharms().add(new PurchasedCharm("1", SystemData.TERRESTRIAL_CIRCLE_SORCERY, Ability.OCCULT.getDisplayName()));
        
        assertTrue(model.terrestrialSorceryAvailableProperty().get());
        assertFalse(model.celestialSorceryAvailableProperty().get());

        // Add Celestial Circle Sorcery charm
        model.getUnlockedCharms().add(new PurchasedCharm("2", SystemData.CELESTIAL_CIRCLE_SORCERY, Ability.OCCULT.getDisplayName()));
        
        assertTrue(model.celestialSorceryAvailableProperty().get());
    }

    @Test
    void testCharmTriggersUpdate() {
        int initial = updateCount.get();
        model.getUnlockedCharms().add(new PurchasedCharm("1", "Shadow Dancing", Ability.DODGE.getDisplayName()));
        assertTrue(updateCount.get() > initial);
    }
}