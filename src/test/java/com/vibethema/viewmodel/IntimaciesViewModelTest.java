package com.vibethema.viewmodel;

import static org.junit.jupiter.api.Assertions.*;

import com.vibethema.model.CharacterData;
import com.vibethema.model.CharacterFactory;
import com.vibethema.model.social.Intimacy;
import com.vibethema.viewmodel.social.IntimacyRowViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IntimaciesViewModelTest {
    private CharacterData data;
    private IntimaciesViewModel viewModel;

    @BeforeEach
    public void setup() {
        data = CharacterFactory.createNewCharacter();
        data.getIntimacies().clear();
        viewModel = new IntimaciesViewModel(data);
    }

    @Test
    public void testSyncOnInit() {
        data.getIntimacies()
                .add(
                        new Intimacy(
                                "id1", "Intimacy 1", Intimacy.Type.TIE, Intimacy.Intensity.MINOR));

        // Re-inject/Re-init viewport
        IntimaciesViewModel newVm = new IntimaciesViewModel(data);
        assertEquals(1, newVm.getIntimacyRows().size());
        assertEquals("Intimacy 1", newVm.getIntimacyRows().get(0).nameProperty().get());
    }

    @Test
    public void testAddIntimacySyncs() {
        assertEquals(0, viewModel.getIntimacyRows().size());
        viewModel.addIntimacy();
        assertEquals(1, viewModel.getIntimacyRows().size());
        assertTrue(data.getIntimacies().size() == 1);
    }

    @Test
    public void testRemoveIntimacySyncs() {
        viewModel.addIntimacy();
        IntimacyRowViewModel rowVm = viewModel.getIntimacyRows().get(0);
        viewModel.removeIntimacy(rowVm.getModel());

        assertEquals(0, viewModel.getIntimacyRows().size());
        assertTrue(data.getIntimacies().isEmpty());
    }

    @Test
    public void testDirtyFlagOnAdd() {
        data.setDirty(false);
        viewModel.addIntimacy();
        assertTrue(data.isDirty());
    }
}
