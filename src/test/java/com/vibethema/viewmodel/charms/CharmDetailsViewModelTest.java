package com.vibethema.viewmodel.charms;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.vibethema.model.*;
import com.vibethema.model.mystic.PrerequisiteRule;
import com.vibethema.model.mystic.PurchasedCharm;
import com.vibethema.model.mystic.SolarCharm;
import com.vibethema.service.CharmDataService;
import com.vibethema.viewmodel.util.Messenger;
import de.saxsys.mvvmfx.utils.notifications.NotificationObserver;
import java.util.HashMap;
import java.util.List;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
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
        when(data.getAbility(com.vibethema.model.traits.Ability.RESISTANCE))
                .thenReturn(new SimpleIntegerProperty(1));
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
    void sidebarShowsProgressForNonExcellencyAbilityCharmCount() {
        SolarCharm eye = createBasicCharm("eye", "Eye of the Unconquered Sun");
        eye.setAbility("Awareness");
        eye.setPrerequisites(
                PrerequisiteRule.allOf(
                        List.of(
                                PrerequisiteRule.charm("awakening-eye"),
                                PrerequisiteRule.countFromAbilities(
                                        List.of("Awareness"), 3, List.of("awakening-eye"), true))));
        when(dataService.getCharmName("awakening-eye")).thenReturn("Awakening Eye");
        when(data.getUnlockedCharms())
                .thenReturn(
                        FXCollections.observableArrayList(
                                new PurchasedCharm("awakening-eye", "Awakening Eye", "Awareness"),
                                new PurchasedCharm(
                                        "keen-sight", "Keen Sight Technique", "Awareness"),
                                new PurchasedCharm(
                                        "awareness-exc", "Awareness Excellency", "Awareness")));

        viewModel.updateSidebar(eye);

        assertTrue(viewModel.detailReqsProperty().get().contains("Awakening Eye"));
        assertTrue(
                viewModel
                        .detailReqsProperty()
                        .get()
                        .contains("Other non-Excellency Awareness Charms: 1/3"),
                viewModel.detailReqsProperty().get());

        when(data.getUnlockedCharms())
                .thenReturn(
                        FXCollections.observableArrayList(
                                new PurchasedCharm("awakening-eye", "Awakening Eye", "Awareness"),
                                new PurchasedCharm(
                                        "keen-sight", "Keen Sight Technique", "Awareness"),
                                new PurchasedCharm(
                                        "studied-ear", "Studied Ear Espial", "Awareness"),
                                new PurchasedCharm(
                                        "awareness-exc", "Awareness Excellency", "Awareness")));
        viewModel.updateSidebar(eye);
        assertTrue(
                viewModel
                        .detailReqsProperty()
                        .get()
                        .contains("Other non-Excellency Awareness Charms: 2/3"));
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

    @Test
    void directActionsEnforceEligibilityAndRepeatLimitsInSidebarAndTree() {
        CharacterData character = new CharacterData();
        character.getAbility(com.vibethema.model.traits.Ability.RESISTANCE).set(1);
        character.getAbility(com.vibethema.model.traits.Ability.ARCHERY).set(3);
        SolarCharm charm = createBasicCharm("stack", "Ox-Body Technique");
        charm.setKeywords(List.of("Stackable"));
        var sidebar = new CharmDetailsViewModel(character, dataService, new HashMap<>());
        var tree = new CharmTreeViewModel(character, dataService, new HashMap<>());
        sidebar.selectedCharmProperty().set(charm);
        tree.selectedCharmProperty().set(charm);
        sidebar.selectedCharmProperty().set(charm);
        sidebar.togglePurchase();
        tree.selectedCharmProperty().set(charm);
        tree.togglePurchase();
        sidebar.selectedCharmProperty().set(charm);
        sidebar.togglePurchase();
        assertEquals(1, character.getCharmCount(charm.getId()));
        sidebar.selectedCharmProperty().set(charm);
        sidebar.refundOne();
        assertEquals(0, character.getCharmCount(charm.getId()));
        // The selected Charm has become ineligible since its sidebar was rendered.
        character.getAbility(com.vibethema.model.traits.Ability.ARCHERY).set(0);
        sidebar.selectedCharmProperty().set(charm);
        sidebar.togglePurchase();
        tree.selectedCharmProperty().set(charm);
        tree.togglePurchase();
        assertEquals(0, character.getCharmCount(charm.getId()));
        character.getAbility(com.vibethema.model.traits.Ability.ARCHERY).set(3);
        tree.selectedCharmProperty().set(charm);
        tree.togglePurchase();
        assertEquals(1, character.getCharmCount(charm.getId()));
        character.getAbility(com.vibethema.model.traits.Ability.ARCHERY).set(0);
        sidebar.selectedCharmProperty().set(charm);
        sidebar.refundOne();
        assertEquals(0, character.getCharmCount(charm.getId()));
    }

    @Test
    void genericStackLimitAndSinglePurchaseRefundArePreserved() {
        CharacterData character = new CharacterData();
        character.getAbility(com.vibethema.model.traits.Ability.ARCHERY).set(3);
        SolarCharm charm = createBasicCharm("stack-ten", "Repeatable Test");
        charm.setKeywords(List.of("Stackable"));
        var sidebar = new CharmDetailsViewModel(character, dataService, new HashMap<>());
        sidebar.selectedCharmProperty().set(charm);
        for (int i = 0; i < 12; i++) {
            sidebar.selectedCharmProperty().set(charm);
            sidebar.togglePurchase();
        }
        assertEquals(10, character.getCharmCount(charm.getId()));
        SolarCharm single = createBasicCharm("single", "Single Test");
        sidebar.selectedCharmProperty().set(single);
        sidebar.togglePurchase();
        character.getAbility(com.vibethema.model.traits.Ability.ARCHERY).set(0);
        sidebar.selectedCharmProperty().set(single);
        sidebar.togglePurchase();
        assertFalse(character.hasCharm(single.getId()));
    }
}
