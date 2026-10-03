package com.vibethema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vibethema.model.mystic.Spell;
import com.vibethema.model.traits.Merit;
import java.util.List;
import org.junit.jupiter.api.Test;

class SourceMetadataPersistenceTest {
    @Test
    void preservesSourceIdsForMeritsAndSpells() {
        CharacterData data = new CharacterData();
        Merit merit = new Merit("instance", "definition", "Artifact", 3);
        merit.setSourceId("exalted-3e-core");
        data.getMerits().clear();
        data.getMerits().add(merit);

        Spell spell =
                new Spell(
                        "spell-id", "Fire", "TERRESTRIAL", "5m", List.of(), "Instant", "text");
        spell.setSourceId("exalted-3e-core");
        data.getSpells().add(spell);

        CharacterSaveState state = data.exportState();

        assertEquals("exalted-3e-core", state.merits.get(0).sourceId);
        assertEquals("exalted-3e-core", state.spells.get(0).sourceId);

        CharacterData restored = new CharacterData();
        restored.importState(state, new com.vibethema.service.EquipmentDataService());
        assertEquals("exalted-3e-core", restored.getMerits().get(0).getSourceId());
        assertEquals("exalted-3e-core", restored.getSpells().get(0).getSourceId());
    }
}
