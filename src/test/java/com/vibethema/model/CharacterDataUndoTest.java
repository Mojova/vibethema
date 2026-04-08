package com.vibethema.model;

import static org.junit.jupiter.api.Assertions.*;

import com.vibethema.model.traits.Attribute;
import com.vibethema.service.UndoManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CharacterDataUndoTest {
    private CharacterData data;

    @BeforeEach
    void setUp() {
        data = new CharacterData();
    }

    @Test
    void testRestoreState() {
        // Initial name
        data.nameProperty().set("Old Name");
        data.getAttribute(Attribute.STRENGTH).set(2);
        
        CharacterSaveState snapshot = data.exportState();
        
        // Modify
        data.nameProperty().set("New Name");
        data.getAttribute(Attribute.STRENGTH).set(5);
        
        assertEquals("New Name", data.nameProperty().get());
        assertEquals(5, data.getAttribute(Attribute.STRENGTH).get());

        // Restore
        data.restoreState(snapshot, null);
        
        assertEquals("Old Name", data.nameProperty().get());
        assertEquals(2, data.getAttribute(Attribute.STRENGTH).get());
        assertFalse(data.isUndoRedoInProgress());
    }

    @Test
    void testStateChangesDetectionInUndoManager() {
        UndoManager undoManager = new UndoManager();
        
        data.nameProperty().set("Start");
        // Checkpoint the state BEFORE the change
        undoManager.pushCheckpoint(data.exportState(), "MAIN", "Set to Changed", "name");
        
        data.nameProperty().set("Changed");
        
        assertTrue(undoManager.canUndoProperty().get());
        
        // Undo "Set to Changed" -> Should get "Start" state
        CharacterSaveState previous = undoManager.undo("MAIN", data.exportState()).state();
        data.restoreState(previous, null);
        assertEquals("Start", data.nameProperty().get());

        // Redo -> Should get "Changed" state
        CharacterSaveState next = undoManager.redo("MAIN", data.exportState()).state();
        data.restoreState(next, null);
        assertEquals("Changed", data.nameProperty().get());
    }
}
