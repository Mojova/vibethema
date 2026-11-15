package com.vibethema.viewmodel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vibethema.model.Caste;
import com.vibethema.model.CharacterData;
import com.vibethema.model.ExaltType;
import com.vibethema.model.mystic.AnimaAbility;
import com.vibethema.service.AnimaDataService;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnimaEffectsViewModelTest {
    @Test
    void reactsToCasteChangesResetAndExaltTypeWithoutEditingCharacter() {
        var data = new CharacterData();
        var service = mock(AnimaDataService.class);
        var general = ability("General");
        var dawn = ability("Dawn");
        var night = ability("Night");
        when(service.loadAbilities(ExaltType.SOLAR, Caste.DAWN)).thenReturn(List.of(general, dawn));
        when(service.loadAbilities(ExaltType.SOLAR, Caste.NIGHT))
                .thenReturn(List.of(general, night));
        var vm = new AnimaEffectsViewModel(data, service);
        assertEquals("Select caste to see anima effects", vm.messageProperty().get());
        assertTrue(vm.getAbilities().isEmpty());
        verifyNoInteractions(service);
        data.casteProperty().set(Caste.DAWN);
        assertEquals(List.of(general, dawn), vm.getAbilities());
        assertEquals("", vm.messageProperty().get());
        data.casteProperty().set(Caste.NIGHT);
        assertEquals(List.of(general, night), vm.getAbilities());
        data.exaltTypeProperty().set(ExaltType.LUNAR);
        assertTrue(vm.getAbilities().isEmpty());
        assertFalse(vm.messageProperty().get().isBlank());
        data.casteProperty().set(Caste.NONE);
        assertEquals("Select caste to see anima effects", vm.messageProperty().get());
        data.setDirty(false);
        vm.refresh();
        assertFalse(data.isDirty());
    }

    @Test
    void refreshLoadsNewlyImportedDataForAnAlreadySelectedCaste() {
        var data = new CharacterData();
        data.casteProperty().set(Caste.DAWN);
        var service = mock(AnimaDataService.class);
        when(service.loadAbilities(ExaltType.SOLAR, Caste.DAWN))
                .thenReturn(List.of(), List.of(ability("Imported")));
        var vm = new AnimaEffectsViewModel(data, service);
        assertTrue(vm.getAbilities().isEmpty());
        vm.refresh();
        assertEquals("Imported", vm.getAbilities().getFirst().name());
        assertEquals("", vm.messageProperty().get());
    }

    private AnimaAbility ability(String name) {
        return new AnimaAbility(
                name,
                "source",
                List.of("SOLAR"),
                List.of(),
                name,
                "Description",
                List.of("Effect"));
    }
}
