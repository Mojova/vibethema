package com.vibethema.viewmodel.charms;

import com.vibethema.model.CharacterData;
import com.vibethema.model.Charm;
import com.vibethema.model.SolarCharm;
import com.vibethema.service.CharmDataService;
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
    void testSearchFilterBasicMatch() {
        // Arrange
        Charm c1 = createCharm("C1", "Archery Charm");
        when(dataService.loadCharmsForAbility("Archery")).thenReturn(Arrays.asList(c1));
        viewModel.initialize("Ability", "Archery", "Archery", null, null);

        // Act & Assert
        viewModel.searchTextProperty().set("Archery");
        assertTrue(viewModel.getCurrentCharms().contains(c1), "Should match 'Archery'");
        
        viewModel.searchTextProperty().set("Charm");
        assertTrue(viewModel.getCurrentCharms().contains(c1), "Should match 'Charm' (suffix)");
        
        viewModel.searchTextProperty().set("None");
        assertFalse(viewModel.getCurrentCharms().contains(c1), "Should NOT match 'None'");
    }

    @Test
    void testSearchIncludesPrerequisites() {
        // Tree: A -> B -> C
        Charm a = createCharm("A", "Alpha");
        Charm b = createCharm("B", "Beta", "A");
        Charm c = createCharm("C", "Gamma", "B");
        
        when(dataService.loadCharmsForAbility("Melee")).thenReturn(Arrays.asList(a, b, c));
        viewModel.initialize("Ability", "Melee", "Melee", null, null);

        // Act: Search for "Gamma"
        viewModel.searchTextProperty().set("Gamma");

        // Assert
        assertTrue(viewModel.getCurrentCharms().contains(c), "Direct match must be visible");
        assertTrue(viewModel.getCurrentCharms().contains(b), "Direct prerequisite must be included");
        assertTrue(viewModel.getCurrentCharms().contains(a), "Recursive prerequisite must be included");
        assertEquals(3, viewModel.getCurrentCharms().size(), "Only the chain and match should be visible");
    }

    @Test
    void testSearchCaseInsensitive() {
        Charm c = createCharm("C1", "Solar Flare");
        when(dataService.loadCharmsForAbility("Archery")).thenReturn(Arrays.asList(c));
        viewModel.initialize("Ability", "Archery", "Archery", null, null);

        viewModel.searchTextProperty().set("solar");
        assertTrue(viewModel.getCurrentCharms().contains(c), "Should be case-insensitive");
    }

    @Test
    void testAdditiveFilters() {
        // A (bought) -> B (eligible) -> C (ineligible)
        Charm a = createCharm("A", "Alpha");
        Charm b = createCharm("B", "Beta", "A");
        Charm c = createCharm("C", "Gamma", "B");
        
        // Setup character state
        data.addCharm(new com.vibethema.model.PurchasedCharm("A", "Alpha", "Melee"));
        // C is ineligible because B is not purchased
        
        when(dataService.loadCharmsForAbility("Melee")).thenReturn(Arrays.asList(a, b, c));
        viewModel.initialize("Ability", "Melee", "Melee", null, null);

        // Toggle "Eligible Only"
        viewModel.showEligibleOnlyProperty().set(true);
        // At this point A and B are visible, C is hidden.
        assertTrue(viewModel.getCurrentCharms().contains(a));
        assertTrue(viewModel.getCurrentCharms().contains(b));
        assertFalse(viewModel.getCurrentCharms().contains(c));

        // Search for "Gamma"
        viewModel.searchTextProperty().set("Gamma");
        // Gamma is ineligible. With additive filters, it remains hidden because it doesn't pass the "Eligible" check.
        // Wait, if search overrides eligibility? No, additive.
        assertFalse(viewModel.getCurrentCharms().contains(c), "Should be hidden by eligibility even if matching search");
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
