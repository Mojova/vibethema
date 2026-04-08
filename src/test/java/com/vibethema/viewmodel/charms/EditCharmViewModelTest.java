package com.vibethema.viewmodel.charms;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vibethema.model.*;
import com.vibethema.model.mystic.*;
import com.vibethema.service.CharmDataService;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EditCharmViewModelTest {

    private CharmDataService dataService;
    private SolarCharm charm;
    private EditCharmViewModel viewModel;

    @BeforeEach
    void setUp() {
        dataService = mock(CharmDataService.class);
        charm = new SolarCharm();
        charm.setId("test-id");
        charm.setName("Test Charm");
        charm.setAbility("Archery");

        // Mock loadCharmsForAbility to return some potential prereqs
        SolarCharm p1 = new SolarCharm();
        p1.setId("p1");
        p1.setName("Prereq 1");
        SolarCharm p2 = new SolarCharm();
        p2.setId("p2");
        p2.setName("Prereq 2");

        // When Archery is requested, return the charm itself (to test filtering) and two others
        when(dataService.loadCharmsForAbility("Archery")).thenReturn(Arrays.asList(charm, p1, p2));
        when(dataService.getAvailableMartialArtsStyles()).thenReturn(Collections.emptyList());

        com.vibethema.model.mystic.Keyword k1 = new com.vibethema.model.mystic.Keyword();
        k1.setName("Mute");
        com.vibethema.model.mystic.Keyword k2 = new com.vibethema.model.mystic.Keyword();
        k2.setName("Stackable");
        when(dataService.loadKeywords()).thenReturn(Arrays.asList(k1, k2));

        viewModel = new EditCharmViewModel(charm, dataService, "test-context", "solar", null);
    }

    @Test
    void testKeywordsLoading() {
        assertEquals(2, viewModel.getAvailableKeywords().size());
        assertTrue(viewModel.getAvailableKeywords().contains("Mute"));
        assertTrue(viewModel.getAvailableKeywords().contains("Stackable"));
    }

    @Test
    void testSaveKeywords() throws IOException {
        viewModel.getSelectedKeywords().add("Mute");
        viewModel.save();

        assertNotNull(charm.getKeywords());
        assertEquals(1, charm.getKeywords().size());
        assertEquals("Mute", charm.getKeywords().get(0));
    }

    @Test
    void testPrerequisitesLoading() {
        // availablePrerequisites should NOT include the charm itself
        assertEquals(2, viewModel.getAvailablePrerequisites().size());
        assertEquals("p1", viewModel.getAvailablePrerequisites().get(0).getId());
        assertEquals("p2", viewModel.getAvailablePrerequisites().get(1).getId());
    }

    @Test
    void testSavePrerequisites() throws IOException {
        Charm p1 = viewModel.getAvailablePrerequisites().get(0);
        viewModel.getSelectedPrerequisites().add(p1);

        viewModel.save();

        // Verify charm has the prereq
        assertNotNull(charm.getPrerequisiteGroups());
        assertEquals(1, charm.getPrerequisiteGroups().size());
        Charm.PrerequisiteGroup group = charm.getPrerequisiteGroups().get(0);
        assertEquals(1, group.getCharmIds().size());
        assertEquals("p1", group.getCharmIds().get(0));

        // Verify minCount is set to total count (AND logic)
        assertEquals(1, group.getMinCount());
    }

    @Test
    void testAbilityChangeRefreshesPrerequisites() {
        SolarCharm meleePrereq = new SolarCharm();
        meleePrereq.setId("m1");
        meleePrereq.setName("Melee Prereq");
        when(dataService.loadCharmsForAbility("Melee"))
                .thenReturn(Collections.singletonList(meleePrereq));

        viewModel.abilityProperty().set("Melee");

        assertEquals(1, viewModel.getAvailablePrerequisites().size());
        assertEquals("m1", viewModel.getAvailablePrerequisites().get(0).getId());
        assertTrue(viewModel.getSelectedPrerequisites().isEmpty());
    }
}
