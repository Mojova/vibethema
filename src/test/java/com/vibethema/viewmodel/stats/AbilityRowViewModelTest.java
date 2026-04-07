package com.vibethema.viewmodel.stats;

import com.vibethema.model.Ability;
import com.vibethema.model.Caste;
import com.vibethema.model.CharacterData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AbilityRowViewModel, verifying business rules for
 * caste/favored selection and UI state management.
 */
public class AbilityRowViewModelTest {
    private CharacterData data;
    private AbilityRowViewModel viewModel;

    @BeforeEach
    void setUp() {
        data = new CharacterData();
        viewModel = new AbilityRowViewModel(data, Ability.ARCHERY);
    }

    @Test
    void testCasteBoxDisabling() {
        // Initially Caste is NONE. Archery checkbox should be disabled.
        data.casteProperty().set(Caste.NONE);
        assertTrue(viewModel.casteBoxDisabledProperty().get());

        // Set to Dawn (Dawn has Archery). Checkbox should be enabled.
        data.casteProperty().set(Caste.DAWN);
        assertFalse(viewModel.casteBoxDisabledProperty().get());

        // Set to Zenith (Zenith does NOT have Archery). Checkbox should be disabled.
        data.casteProperty().set(Caste.ZENITH);
        assertTrue(viewModel.casteBoxDisabledProperty().get());
    }

    @Test
    void testFavoredBoxDisablingByCaste() {
        // If it's a caste ability, favored should be disabled
        data.getCasteAbility(Ability.ARCHERY).set(true);
        assertTrue(viewModel.favoredBoxDisabledProperty().get());

        // If it's NOT a caste ability, favored should be enabled
        data.getCasteAbility(Ability.ARCHERY).set(false);
        assertFalse(viewModel.favoredBoxDisabledProperty().get());
    }

    @Test
    void testFavoredLimitEnforcement() {
        // Pick 5 other favored abilities
        data.getFavoredAbility(Ability.ATHLETICS).set(true);
        data.getFavoredAbility(Ability.AWARENESS).set(true);
        data.getFavoredAbility(Ability.BRAWL).set(true);
        data.getFavoredAbility(Ability.DODGE).set(true);
        data.getFavoredAbility(Ability.MELEE).set(true);
        
        // Archery favored box should now be disabled (limit reached)
        assertTrue(viewModel.favoredBoxDisabledProperty().get());
        
        // Remove one, it should re-enable
        data.getFavoredAbility(Ability.MELEE).set(false);
        assertFalse(viewModel.favoredBoxDisabledProperty().get());
    }

    @Test
    void testMartialArtsSyncDisabling() {
        // MA Selection boxes should always be disabled (they're synced with Brawl)
        AbilityRowViewModel maVm = new AbilityRowViewModel(data, Ability.MARTIAL_ARTS);
        assertTrue(maVm.casteBoxDisabledProperty().get());
        assertTrue(maVm.favoredBoxDisabledProperty().get());
    }

    @Test
    void testReactiveHighlighting() {
        // Zenith has Athletics
        data.casteProperty().set(Caste.ZENITH);
        AbilityRowViewModel athleticsVm = new AbilityRowViewModel(data, Ability.ATHLETICS);
        assertTrue(athleticsVm.isOptionProperty().get());
        
        // Switch to Dawn, athletics is NOT a Dawn ability
        data.casteProperty().set(Caste.DAWN);
        assertFalse(athleticsVm.isOptionProperty().get());
    }
}
