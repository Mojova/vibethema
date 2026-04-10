package com.vibethema.viewmodel;

import static org.mockito.Mockito.*;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.viewmodel.util.Messenger;
import de.saxsys.mvvmfx.utils.notifications.NotificationObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TraitSyncTest {
    private CharacterData data;
    private NotificationObserver mockObserver;

    @BeforeEach
    void setUp() {
        data = CharacterFactory.createNewCharacter();
        mockObserver = mock(NotificationObserver.class);
        Messenger.subscribe("refresh_all_ui", mockObserver);
    }

    @Test
    void testAbilityChangeTriggersRefresh() {
        data.getAbility(Ability.MELEE).set(3);
        verify(mockObserver, atLeastOnce()).receivedNotification(eq("refresh_all_ui"));
    }

    @Test
    void testAttributeChangeTriggersRefresh() {
        data.getAttribute(Attribute.STRENGTH).set(3);
        verify(mockObserver, atLeastOnce()).receivedNotification(eq("refresh_all_ui"));
    }

    @Test
    void testEssenceChangeTriggersRefresh() {
        data.essenceProperty().set(2);
        verify(mockObserver, atLeastOnce()).receivedNotification(eq("refresh_all_ui"));
    }

    @Test
    void testCasteAbilityChangeTriggersRefresh() {
        data.getCasteAbility(Ability.MELEE).set(true);
        verify(mockObserver, atLeastOnce()).receivedNotification(eq("refresh_all_ui"));
    }
}
