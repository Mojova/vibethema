package com.vibethema.viewmodel.martialarts;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.vibethema.model.CharacterData;
import com.vibethema.service.CharmDataService;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MartialArtsViewModelTest {

    private MartialArtsViewModel viewModel;
    private CharacterData data;
    private CharmDataService charmDataService;

    @BeforeEach
    void setUp() {
        data = new CharacterData();
        charmDataService = mock(CharmDataService.class);
        
        java.util.List<String> styles = java.util.List.of("Single Point", "Steel Devil");
        org.mockito.Mockito.when(charmDataService.getAvailableMartialArtsStyles())
                .thenReturn(styles);
                
        viewModel = new MartialArtsViewModel(data, charmDataService, new HashMap<>());
    }

    @Test
    void testFilterOptionsIncludeStyles() {
        assertTrue(viewModel.getFilterOptions().contains("Single Point"));
        assertTrue(viewModel.getFilterOptions().contains("Steel Devil"));
        assertFalse(viewModel.getFilterOptions().contains("Archery"));
    }

    @Test
    void testRefreshStyles() {
        java.util.List<String> newStyles = java.util.List.of("Single Point", "Steel Devil", "Dreaming Pearl");
        org.mockito.Mockito.when(charmDataService.getAvailableMartialArtsStyles())
                .thenReturn(newStyles);
        
        viewModel.refreshStyles();
        
        assertTrue(viewModel.getFilterOptions().contains("Dreaming Pearl"));
    }

    @Test
    void testSelectedFilterValueProperty() {
        viewModel.selectedFilterValueProperty().set("Steel Devil");
        assertEquals("Steel Devil", viewModel.selectedFilterValueProperty().get());
    }
}
