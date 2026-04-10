package com.vibethema.viewmodel.social;

import static org.junit.jupiter.api.Assertions.*;

import com.vibethema.model.social.Intimacy;
import org.junit.jupiter.api.Test;

public class IntimacyRowViewModelTest {
    @Test
    public void testPropertyBinding() {
        Intimacy model =
                new Intimacy("id", "Original", Intimacy.Type.TIE, Intimacy.Intensity.MINOR);
        IntimacyRowViewModel viewModel = new IntimacyRowViewModel(model);

        assertEquals("Original", viewModel.nameProperty().get());

        viewModel.nameProperty().set("Updated");
        assertEquals("Updated", model.getName());

        viewModel.typeProperty().set(Intimacy.Type.PRINCIPLE);
        assertEquals(Intimacy.Type.PRINCIPLE, model.getType());
    }
}
