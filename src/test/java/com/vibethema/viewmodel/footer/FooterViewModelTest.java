package com.vibethema.viewmodel.footer;

import com.vibethema.model.Ability;
import com.vibethema.model.CharacterData;
import com.vibethema.viewmodel.util.Messenger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FooterViewModelTest {
    private CharacterData data;
    private FooterViewModel viewModel;

    @BeforeEach
    void setUp() {
        data = new CharacterData();
        viewModel = new FooterViewModel(data, () -> {});
    }

    @Test
    void testFooterUpdatesOnRefreshMessage() {
        // 1. Initial status
        assertEquals("Abilities: 0/28", viewModel.abilitiesTextProperty().get());

        // 2. Modify model directly
        data.getAbility(Ability.ARCHERY).set(3);
        
        // At this point, the footer hasn't updated because it's not listening to Archery
        assertEquals("Abilities: 0/28", viewModel.abilitiesTextProperty().get());

        // 3. Publish refresh message (simulating what DotSelector does)
        Messenger.publish("refresh_all_ui");

        // 4. Verify footer updated
        assertEquals("Abilities: 3/28", viewModel.abilitiesTextProperty().get());
    }

    @Test
    void testCasteCountUpdatesOnRefresh() {
        data.getCasteAbility(Ability.BRAWL).set(true);
        Messenger.publish("refresh_all_ui");
        assertEquals("Caste: 1/5", viewModel.casteTextProperty().get());
    }
}
