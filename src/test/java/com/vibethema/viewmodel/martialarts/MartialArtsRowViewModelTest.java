package com.vibethema.viewmodel.martialarts;

import static org.junit.jupiter.api.Assertions.*;

import com.vibethema.model.CharacterData;
import com.vibethema.model.traits.Ability;
import com.vibethema.model.traits.MartialArtsStyle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MartialArtsRowViewModelTest {

    private CharacterData data;
    private MartialArtsStyle model;
    private MartialArtsRowViewModel viewModel;

    @BeforeEach
    void setUp() {
        data = new CharacterData();
        model = new MartialArtsStyle("1", "Tiger Style", 2);
        viewModel = new MartialArtsRowViewModel(data, model, null);
    }

    @Test
    void testInitialization() {
        assertEquals("Tiger Style", viewModel.nameProperty().get());
        assertEquals(2, viewModel.ratingProperty().get());
        assertFalse(viewModel.editingProperty().get());
    }

    @Test
    void testEditFlow() {
        viewModel.beginEdit();
        assertTrue(viewModel.editingProperty().get());
        assertEquals("Tiger Style", viewModel.draftNameProperty().get());

        viewModel.draftNameProperty().set("Mantis Style");
        viewModel.commitEdit();

        assertFalse(viewModel.editingProperty().get());
        assertEquals("Mantis Style", viewModel.nameProperty().get());
        assertEquals("Mantis Style", model.getStyleName());
    }

    @Test
    void testCancelEdit() {
        viewModel.beginEdit();
        viewModel.draftNameProperty().set("Mantis Style");
        viewModel.cancelEdit();

        assertFalse(viewModel.editingProperty().get());
        assertEquals("Tiger Style", viewModel.nameProperty().get());
        assertEquals("Tiger Style", model.getStyleName());
    }

    @Test
    void testCasteFavoredStatus() {
        // MA status is synced with Brawl
        data.getCasteAbility(Ability.BRAWL).set(true);
        assertTrue(viewModel.isCaste().get());
        assertFalse(viewModel.isFavored().get());

        data.getCasteAbility(Ability.BRAWL).set(false);
        data.getFavoredAbility(Ability.BRAWL).set(true);
        assertFalse(viewModel.isCaste().get());
        assertTrue(viewModel.isFavored().get());
    }
}
