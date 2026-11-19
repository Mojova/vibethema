package com.vibethema.model.logic;

import static org.junit.jupiter.api.Assertions.*;

import com.vibethema.model.CharacterData;
import org.junit.jupiter.api.Test;

public class FinalizationTest {
    @Test
    void newCharacterIsNotReady() {
        assertFalse(CreationRuleEngine.calculateStatus(new CharacterData()).isReadyToFinalize);
    }

    @Test
    void completeCharacterIsReady() {
        assertTrue(
                CreationRuleEngine.calculateStatus(CreationTestCharacters.ready())
                        .isReadyToFinalize);
    }

    @Test
    void unspentBonusPointsPreventFinalization() {
        CharacterData data = CreationTestCharacters.ready();
        data.getMerits().removeLast();
        assertFalse(CreationRuleEngine.calculateStatus(data).isReadyToFinalize);
    }

    @Test
    void missingSupernalPreventsFinalizationEvenWithCompletePools() {
        CharacterData data = CreationTestCharacters.ready();
        data.supernalAbilityProperty().set("");
        assertFalse(CreationRuleEngine.calculateStatus(data).isReadyToFinalize);
    }
}
