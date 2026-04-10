package com.vibethema.viewmodel.charms;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.vibethema.model.CharacterData;
import com.vibethema.service.CharmDataService;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CharmsViewModelTest {

    private CharmsViewModel viewModel;
    private CharacterData data;
    private CharmDataService charmDataService;

    @BeforeEach
    void setUp() {
        data = new CharacterData();
        charmDataService = mock(CharmDataService.class);
        viewModel = new CharmsViewModel(data, charmDataService, new HashMap<>(), "Ability");
    }

    @Test
    void testFilterOptionsIncludeCraft() {
        // Since we are testing the Ability filter, it should include "Craft"
        assertTrue(
                viewModel.getFilterOptions().contains("Craft"),
                "Filter options should include 'Craft' matching Ability.CRAFT display name");
    }

    @Test
    void testFilterOptionsExcludeMartialArts() {
        // Martial Arts should be excluded because it has its own filter type
        assertFalse(
                viewModel.getFilterOptions().contains("Martial Arts"),
                "Filter options should exclude 'Martial Arts' as it is handled by another filter"
                        + " type");
    }

    @Test
    void testSelectedFilterValueProperty() {
        viewModel.selectedFilterValueProperty().set("Melee");
        assertEquals("Melee", viewModel.selectedFilterValueProperty().get());
    }
}
