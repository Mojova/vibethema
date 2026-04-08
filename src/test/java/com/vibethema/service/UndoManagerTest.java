package com.vibethema.service;

import static org.junit.jupiter.api.Assertions.*;

import com.vibethema.model.CharacterSaveState;
import com.vibethema.service.UndoManager.UndoEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UndoManagerTest {
    private UndoManager undoManager;

    @BeforeEach
    void setUp() {
        undoManager = new UndoManager();
    }

    @Test
    void testBasicUndoRedo() {
        CharacterSaveState state1 = new CharacterSaveState();
        state1.name = "State 1";
        CharacterSaveState state2 = new CharacterSaveState();
        state2.name = "State 2";
        CharacterSaveState state3 = new CharacterSaveState();
        state3.name = "State 3";

        // Initial state
        assertFalse(undoManager.canUndoProperty().get());
        assertFalse(undoManager.canRedoProperty().get());

        // Push state 1
        undoManager.pushCheckpoint(state1, "TAB1", "Action 1", "target1");
        assertTrue(undoManager.canUndoProperty().get());
        assertFalse(undoManager.canRedoProperty().get());

        // Push state 2
        undoManager.pushCheckpoint(state2, "TAB1", "Action 2", "target2");

        // Push state 3
        undoManager.pushCheckpoint(state3, "TAB1", "Action 3", "target3");

        // Undo 3 -> 2
        UndoEntry entry = undoManager.undo("TAB1", state3);
        assertEquals(state3, entry.state());
        assertTrue(undoManager.canRedoProperty().get());

        // Undo 2 -> 1
        entry = undoManager.undo("TAB1", state2);
        assertEquals(state2, entry.state());

        // Redo 1 -> 2
        entry = undoManager.redo("TAB1", state1);
        assertEquals(state2, entry.state());

        // Redo 2 -> 3
        entry = undoManager.redo("TAB1", state2);
        assertEquals(state3, entry.state());
    }

    @Test
    void testContextFiltering() {
        CharacterSaveState state1 = new CharacterSaveState();
        undoManager.pushCheckpoint(state1, "TAB_STATS", "Change Strength", "stats_strength");

        assertTrue(undoManager.isLastActionInContext("TAB_STATS"));
        assertFalse(undoManager.isLastActionInContext("TAB_CHARMS"));
    }

    @Test
    void testIdenticalStateIgnored() {
        CharacterSaveState state1 = new CharacterSaveState();
        state1.name = "Name";

        undoManager.pushCheckpoint(state1, "CONTEXT", "Action 1", "t1");

        CharacterSaveState state2 = new CharacterSaveState();
        state2.name = "Name"; // Identical

        undoManager.pushCheckpoint(state2, "CONTEXT", "Action 2", "t1");

        // Should only have 1 entry
        undoManager.undo("CONTEXT", state2);
        assertFalse(undoManager.canUndoProperty().get());
    }

    @Test
    void testRedoStackClearedOnNewAction() {
        CharacterSaveState state1 = new CharacterSaveState();
        undoManager.pushCheckpoint(state1, "C1", "A1", "T");
        undoManager.undo("C1", state1);
        assertTrue(undoManager.canRedoProperty().get());

        undoManager.pushCheckpoint(new CharacterSaveState(), "C1", "A2", "T2");
        assertFalse(undoManager.canRedoProperty().get());
    }
}
