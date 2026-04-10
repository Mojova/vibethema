package com.vibethema.viewmodel.martialarts;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.vibethema.model.CharacterData;
import com.vibethema.model.traits.MartialArtsStyle;
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
    void testFilterOptionsInitiallyEmpty() {
        assertTrue(viewModel.getFilterOptions().isEmpty());
    }

    @Test
    void testAddStyleUpdatesFilter() {
        viewModel.addStyle("Single Point");
        assertEquals(1, viewModel.getFilterOptions().size());
        assertTrue(viewModel.getFilterOptions().contains("Single Point"));
        assertEquals("Single Point", viewModel.selectedFilterValueProperty().get());
    }

    @Test
    void testRemoveStyleUpdatesFilter() {
        viewModel.addStyle("Single Point");
        viewModel.addStyle("Steel Devil");
        assertEquals(2, viewModel.getFilterOptions().size());

        MartialArtsStyle styleToRemove = data.getMartialArtsStyles().get(0);
        viewModel.removeStyle(styleToRemove);

        assertEquals(1, viewModel.getFilterOptions().size());
        assertFalse(viewModel.getFilterOptions().contains(styleToRemove.getStyleName()));
    }

    @Test
    void testRefreshStylesUpdatesAvailableList() {
        java.util.List<String> newStyles =
                java.util.List.of("Single Point", "Steel Devil", "Dreaming Pearl");
        org.mockito.Mockito.when(charmDataService.getAvailableMartialArtsStyles())
                .thenReturn(newStyles);

        viewModel.refreshStyles();

        assertTrue(viewModel.getAvailableStyles().contains("Dreaming Pearl"));
        assertEquals(3, viewModel.getAvailableStyles().size());
    }

    @Test
    void testAddCustomStyle() {
        int initialRows = viewModel.getStyleRows().size();
        viewModel.addStyle();
        assertEquals(initialRows + 1, viewModel.getStyleRows().size());
        MartialArtsRowViewModel newRow = viewModel.getStyleRows().get(initialRows);
        assertTrue(newRow.editingProperty().get());
        assertEquals("", newRow.nameProperty().get());
    }
}
