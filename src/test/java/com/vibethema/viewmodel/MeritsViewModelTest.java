package com.vibethema.viewmodel;

import static org.junit.jupiter.api.Assertions.*;

import com.vibethema.model.CharacterData;
import com.vibethema.model.CharacterFactory;
import com.vibethema.model.traits.Merit;
import com.vibethema.viewmodel.stats.MeritRowViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MeritsViewModelTest {
    private CharacterData data;
    private MeritsViewModel viewModel;

    @BeforeEach
    public void setup() {
        data = CharacterFactory.createNewCharacter();
        data.getMerits().clear();
        viewModel = new MeritsViewModel(data);
    }

    @Test
    public void testSyncOnInit() {
        data.getMerits().add(new Merit("id1", "Artifact", 3));

        MeritsViewModel newVm = new MeritsViewModel(data);
        assertEquals(1, newVm.getMeritRows().size());
        assertEquals("Artifact", newVm.getMeritRows().get(0).nameProperty().get());
    }

    @Test
    public void testAddMeritSyncs() {
        assertEquals(0, viewModel.getMeritRows().size());
        viewModel.addMerit();
        assertEquals(1, viewModel.getMeritRows().size());
        assertNotNull(viewModel.getMeritRows().get(0).getModel().getId());
    }

    @Test
    public void testRemoveMeritSyncs() {
        viewModel.addMerit();
        MeritRowViewModel rowVm = viewModel.getMeritRows().get(0);
        viewModel.removeMerit(rowVm.getModel());

        assertEquals(0, viewModel.getMeritRows().size());
    }

    @Test
    public void testDirtyFlagOnAdd() {
        data.setDirty(false);
        viewModel.addMerit();
        assertTrue(data.isDirty());
    }
}
