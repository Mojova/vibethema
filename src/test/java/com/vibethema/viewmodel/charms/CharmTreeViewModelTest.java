package com.vibethema.viewmodel.charms;

import com.vibethema.model.Ability;
import com.vibethema.model.CharacterData;
import com.vibethema.model.Charm;
import com.vibethema.model.SolarCharm;
import com.vibethema.service.CharmDataService;
import com.vibethema.viewmodel.util.Messenger;
import de.saxsys.mvvmfx.utils.notifications.NotificationObserver;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class CharmTreeViewModelTest {

    private CharmTreeViewModel viewModel;
    private CharacterData data;
    private CharmDataService dataService;

    @BeforeEach
    void setUp() {
        data = new CharacterData();
        dataService = Mockito.mock(CharmDataService.class);
        viewModel = new CharmTreeViewModel(data, dataService, new HashMap<>());
    }

    @Test
    void testOnEditRequest() {
        Charm c1 = createCharm("C1", "Test Charm");
        viewModel.selectedCharmProperty().set(c1);
        viewModel.artifactNameProperty().set("Artifact X");
        viewModel.filterTypeProperty().set("Evocation");

        final boolean[] received = {false};
        NotificationObserver observer = (name, payload) -> {
            received[0] = true;
            assertEquals(c1, payload[0]);
            assertEquals("Artifact X", payload[1]);
            assertEquals("Evocation", payload[2]);
        };

        try {
            Messenger.subscribe("open_edit_charm_dialog", observer);
            viewModel.onEditRequest();
            assertTrue(received[0], "Message should be published");
        } finally {
            Messenger.unsubscribe("open_edit_charm_dialog", observer);
        }
    }

    @Test
    void testSearchFilterBasicMatch() {
        Charm c1 = createCharm("C1", "Archery Charm");
        when(dataService.loadCharmsForAbility("Archery")).thenReturn(Arrays.asList(c1));
        viewModel.initialize("Ability", "Archery", "Archery", null, null);

        viewModel.searchTextProperty().set("Archery");
        assertTrue(viewModel.getCurrentCharms().contains(c1));
    }

    @Test
    void testSearchIncludesPrerequisites() {
        Charm a = createCharm("A", "Alpha");
        Charm b = createCharm("B", "Beta", "A");
        Charm c = createCharm("C", "Gamma", "B");
        when(dataService.loadCharmsForAbility("Melee")).thenReturn(Arrays.asList(a, b, c));
        viewModel.initialize("Ability", "Melee", "Melee", null, null);

        viewModel.searchTextProperty().set("Gamma");
        assertTrue(viewModel.getCurrentCharms().contains(c));
        assertTrue(viewModel.getCurrentCharms().contains(b));
        assertTrue(viewModel.getCurrentCharms().contains(a));
    }

    @Test
    void testSearchCaseInsensitive() {
        Charm c = createCharm("C1", "Solar Flare");
        when(dataService.loadCharmsForAbility("Archery")).thenReturn(Arrays.asList(c));
        viewModel.initialize("Ability", "Archery", "Archery", null, null);

        viewModel.searchTextProperty().set("solar");
        assertTrue(viewModel.getCurrentCharms().contains(c));
    }

    @Test
    void testAdditiveFilters() {
        Charm a = createCharm("A", "Alpha");
        Charm b = createCharm("B", "Beta", "A");
        Charm c = createCharm("C", "Gamma", "B");
        data.addCharm(new com.vibethema.model.PurchasedCharm("A", "Alpha", "Melee"));
        when(dataService.loadCharmsForAbility("Melee")).thenReturn(Arrays.asList(a, b, c));
        viewModel.initialize("Ability", "Melee", "Melee", null, null);

        viewModel.showEligibleOnlyProperty().set(true);
        assertTrue(viewModel.getCurrentCharms().contains(a));
        assertTrue(viewModel.getCurrentCharms().contains(b));
        assertFalse(viewModel.getCurrentCharms().contains(c));
    }

    @Test
    void testNavigation() {
        Charm a1 = createCharm("A1", "Alpha 1");
        Charm a2 = createCharm("A2", "Alpha 2");
        Charm b1 = createCharm("B1", "Beta 1", "A1");
        when(dataService.loadCharmsForAbility("Melee")).thenReturn(Arrays.asList(a1, a2, b1));
        viewModel.initialize("Ability", "Melee", "Melee", null, null);

        viewModel.navigate(KeyCode.RIGHT); 
        assertEquals(a1, viewModel.selectedCharmProperty().get());
        viewModel.navigate(KeyCode.RIGHT);
        assertEquals(a2, viewModel.selectedCharmProperty().get());
        viewModel.navigate(KeyCode.DOWN);
        assertEquals(b1, viewModel.selectedCharmProperty().get());
        viewModel.navigate(KeyCode.UP);
        assertEquals(a1, viewModel.selectedCharmProperty().get());
    }

    @Test
    void testSelectionPreservedOnRefresh() {
        String charmId = "TEST_ID";
        Charm c1 = createCharm(charmId, "Test Charm");
        when(dataService.loadCharmsForAbility("Archery")).thenReturn(new ArrayList<>(Arrays.asList(c1)));
        viewModel.initialize("Ability", "Archery", "Archery", null, null);
        viewModel.selectedCharmProperty().set(c1);

        Charm c1Prime = createCharm(charmId, "Test Charm");
        when(dataService.loadCharmsForAbility("Archery")).thenReturn(new ArrayList<>(Arrays.asList(c1Prime)));
        viewModel.refresh();

        assertEquals(c1Prime, viewModel.selectedCharmProperty().get());
    }

    private Charm createCharm(String id, String name, String... prereqIds) {
        SolarCharm c = new SolarCharm();
        c.setId(id);
        c.setName(name);
        if (prereqIds.length > 0) {
            c.setPrerequisiteGroups(Arrays.asList(new Charm.PrerequisiteGroup(null, Arrays.asList(prereqIds), 0)));
        }
        return c;
    }
}
