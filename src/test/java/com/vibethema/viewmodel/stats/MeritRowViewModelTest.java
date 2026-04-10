package com.vibethema.viewmodel.stats;

import static org.junit.jupiter.api.Assertions.*;

import com.vibethema.model.traits.Merit;
import org.junit.jupiter.api.Test;

public class MeritRowViewModelTest {
    @Test
    public void testPropertyBinding() {
        Merit model = new Merit("id", "Original", 2);
        MeritRowViewModel viewModel = new MeritRowViewModel(model);

        assertEquals("Original", viewModel.nameProperty().get());

        viewModel.nameProperty().set("Updated");
        assertEquals("Updated", model.getName());

        viewModel.ratingProperty().set(4);
        assertEquals(4, model.getRating());
    }

    @Test
    public void testEditingLogic() {
        Merit model = new Merit("id", "Original", 2);
        MeritRowViewModel viewModel = new MeritRowViewModel(model);

        assertFalse(viewModel.editingProperty().get());

        // Begin edit
        viewModel.beginEdit();
        assertTrue(viewModel.editingProperty().get());
        assertEquals("Original", viewModel.draftNameProperty().get());

        // Cancel edit
        viewModel.draftNameProperty().set("Changed But Canceled");
        viewModel.cancelEdit();
        assertFalse(viewModel.editingProperty().get());
        assertEquals("Original", model.getName());

        // Commit edit
        viewModel.beginEdit();
        viewModel.draftNameProperty().set("New Name");
        viewModel.commitEdit();
        assertFalse(viewModel.editingProperty().get());
        assertEquals("New Name", model.getName());
        assertEquals("New Name", viewModel.nameProperty().get());
    }
}
