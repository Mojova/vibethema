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
}
