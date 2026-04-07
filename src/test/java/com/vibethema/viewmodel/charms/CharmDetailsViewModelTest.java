package com.vibethema.viewmodel.charms;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.vibethema.model.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;
import com.vibethema.model.traits.*;
import com.vibethema.service.CharmDataService;
import com.vibethema.viewmodel.util.Messenger;
import de.saxsys.mvvmfx.utils.notifications.NotificationObserver;
import java.util.HashMap;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class CharmDetailsViewModelTest {

    private CharmDetailsViewModel viewModel;
    private CharacterData data;
    private CharmDataService dataService;

    @BeforeEach
    void setUp() {
        data = Mockito.mock(CharacterData.class);
        dataService = Mockito.mock(CharmDataService.class);

        when(data.essenceProperty()).thenReturn(new SimpleIntegerProperty(1));
        when(data.supernalAbilityProperty()).thenReturn(new SimpleStringProperty(""));
        when(dataService.getCharmName(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        viewModel = new CharmDetailsViewModel(data, dataService, new HashMap<>());
    }

    private SolarCharm createBasicCharm(String id, String name) {
        SolarCharm c = new SolarCharm();
        c.setId(id);
        c.setName(name);
        c.setAbility("Archery");
        c.setMinEssence(1);
        c.setMinAbility(1);
        return c;
    }

    @Test
    void testOnEditRequest() {
        SolarCharm c = createBasicCharm("C1", "Edit Test");
        viewModel.selectedCharmProperty().set(c);

        final boolean[] received = {false};
        NotificationObserver observer =
                (name, payload) -> {
                    received[0] = true;
                    assertEquals(c, payload[0]);
                };

        try {
            Messenger.subscribe("open_edit_charm_dialog", observer);
            viewModel.onEditRequest();
            assertTrue(received[0], "Edit dialog message should be published");
        } finally {
            Messenger.unsubscribe("open_edit_charm_dialog", observer);
        }
    }

    @Test
    void testUpdateSidebarWithCharm() {
        SolarCharm c = createBasicCharm("C1", "Test Charm");
        when(data.getAbilityRatingByName("Archery")).thenReturn(3);

        viewModel.selectedCharmProperty().set(c);
        assertEquals("Test Charm", viewModel.detailTitleProperty().get());
    }

    @Test
    void testPurchaseToggleCallsAdd() {
        SolarCharm c = createBasicCharm("C1", "Test");
        when(data.hasCharm("C1")).thenReturn(false);
        when(data.getAbilityRatingByName("Archery")).thenReturn(3);

        viewModel.selectedCharmProperty().set(c);
        viewModel.togglePurchase();

        ArgumentCaptor<PurchasedCharm> captor = ArgumentCaptor.forClass(PurchasedCharm.class);
        verify(data).addCharm(captor.capture());
        assertEquals("C1", captor.getValue().id());
    }

    @Test
    void testPurchaseToggleCallsRemove() {
        SolarCharm c = createBasicCharm("C1", "Test");
        when(data.hasCharm("C1")).thenReturn(true);
        when(data.getAbilityRatingByName("Archery")).thenReturn(3);

        viewModel.selectedCharmProperty().set(c);
        viewModel.togglePurchase();

        verify(data).removeCharm("C1");
    }

    @Test
    void testMessengerSelection() {
        SolarCharm c = createBasicCharm("C1", "Message Test");
        Messenger.publish("charm_selected", new Object[] {c});
        assertEquals(c, viewModel.selectedCharmProperty().get());
    }
}
