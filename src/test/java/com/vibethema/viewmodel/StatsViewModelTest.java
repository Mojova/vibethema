package com.vibethema.viewmodel;

import com.vibethema.model.CharacterData;
import de.saxsys.mvvmfx.utils.notifications.NotificationObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.vibethema.viewmodel.util.Messenger;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatsViewModelTest {

    private StatsViewModel viewModel;
    private CharacterData data;

    @BeforeEach
    void setUp() {
        data = new CharacterData();
        viewModel = new StatsViewModel(data);
    }

    @Test
    void testJumpToCharmsPublishesMessage() {
        AtomicReference<String> receivedAbility = new AtomicReference<>();
        
        NotificationObserver observer = (name, payload) -> {
            if (payload != null && payload.length > 0) {
                receivedAbility.set((String) payload[0]);
            }
        };

        Messenger.subscribe("jump_to_charms", observer);
        
        try {
            viewModel.jumpToCharms("Investigation");
            assertEquals("Investigation", receivedAbility.get());
        } finally {
            Messenger.unsubscribe("jump_to_charms", observer);
        }
    }

    @Test
    void testMotePropertiesDelegateToModel() {
        assertEquals(data.personalMotesProperty().get(), viewModel.personalMotesProperty().get());
        assertEquals(data.peripheralMotesProperty().get(), viewModel.peripheralMotesProperty().get());

        data.essenceProperty().set(5);
        assertEquals(data.personalMotesProperty().get(), viewModel.personalMotesProperty().get());
        assertEquals(data.peripheralMotesProperty().get(), viewModel.peripheralMotesProperty().get());
    }

    @Test
    void testHealthLevelsPropertyDelegatesToModel() {
        assertEquals(data.healthLevelsProperty().size(), viewModel.healthLevelsProperty().size());
        
        data.getAttribute(com.vibethema.model.Attribute.STAMINA).set(5);
        assertEquals(data.healthLevelsProperty().size(), viewModel.healthLevelsProperty().size());
    }
}
