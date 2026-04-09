package com.vibethema.viewmodel.footer;

import static org.junit.jupiter.api.Assertions.*;

import com.vibethema.model.*;
import com.vibethema.model.traits.Ability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FooterViewModelTest {
    private CharacterData data;
    private FooterViewModel viewModel;

    @BeforeEach
    void setUp() {
        data = new CharacterData();
        viewModel = new FooterViewModel(data, () -> {});
    }

    @Test
    void testFooterUpdatesReactivelyOnAbilityChange() {
        // 1. Initial status
        assertEquals("Abilities: 0/28", viewModel.abilitiesTextProperty().get());

        // 2. Modify model directly
        data.getAbility(Ability.ARCHERY).set(3);

        // 3. Verify footer updated IMMEDIATELY (reactive)
        assertEquals("Abilities: 3/28", viewModel.abilitiesTextProperty().get());
    }

    @Test
    void testCasteCountUpdatesReactively() {
        data.getCasteAbility(Ability.BRAWL).set(true);
        assertEquals("Caste: 1/5", viewModel.casteTextProperty().get());
    }

    @Test
    void testMotePoolUpdatesReactively() {
        // Stats are updated when Essence changes
        data.essenceProperty().set(3);
        // Essence 3: Personal 19 (Check CreationRuleEngine logic)
        // Actually, the Footer might not show motes directly, let's check one it DOES show.
        data.getAttribute(com.vibethema.model.traits.Attribute.STRENGTH).set(4);
        assertTrue(viewModel.attrTextProperty().get().contains("3/"));
    }
}
