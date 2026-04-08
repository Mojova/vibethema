package com.vibethema.service;

import com.vibethema.model.CharacterSaveState;
import java.util.ArrayDeque;
import java.util.Deque;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Manages the undo and redo history for the application. Uses snapshots of CharacterSaveState to
 * perform Memento-based undo/redo.
 */
public class UndoManager {
    private static final int MAX_HISTORY_SIZE = 50;

    public record UndoEntry(
            CharacterSaveState state, String contextId, String description, String targetId) {}

    private final Deque<UndoEntry> undoStack = new ArrayDeque<>();
    private final Deque<UndoEntry> redoStack = new ArrayDeque<>();

    private final SimpleBooleanProperty canUndo = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty canRedo = new SimpleBooleanProperty(false);

    /**
     * Pushes a new checkpoint to the undo stack. Clears the redo stack whenever a new action is
     * recorded.
     */
    public void pushCheckpoint(
            CharacterSaveState state, String contextId, String description, String targetId) {
        // If the state hasn't changed from the last one, ignore it
        if (!undoStack.isEmpty() && undoStack.peek().state().equals(state)) {
            return;
        }

        undoStack.push(new UndoEntry(state, contextId, description, targetId));
        if (undoStack.size() > MAX_HISTORY_SIZE) {
            undoStack.removeLast();
        }

        redoStack.clear();
        updateProperties();
    }

    /**
     * Pops the last state for the specific context and returns it. If contextId is null or empty,
     * it treats it as a global undo.
     */
    public UndoEntry undo(String currentContextId, CharacterSaveState currentState) {
        if (undoStack.isEmpty()) return null;

        UndoEntry entry = undoStack.pop();

        // Push current state to redo stack before moving back
        redoStack.push(
                new UndoEntry(
                        currentState, entry.contextId(), entry.description(), entry.targetId()));
        if (redoStack.size() > MAX_HISTORY_SIZE) {
            redoStack.removeLast();
        }

        updateProperties();
        return entry;
    }

    public UndoEntry redo(String currentContextId, CharacterSaveState currentState) {
        if (redoStack.isEmpty()) return null;

        UndoEntry entry = redoStack.pop();

        // Push current state to undo stack before moving forward
        undoStack.push(
                new UndoEntry(
                        currentState, entry.contextId(), entry.description(), entry.targetId()));
        if (undoStack.size() > MAX_HISTORY_SIZE) {
            undoStack.removeLast();
        }

        updateProperties();
        return entry;
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
        updateProperties();
    }

    public ReadOnlyBooleanProperty canUndoProperty() {
        return canUndo;
    }

    public ReadOnlyBooleanProperty canRedoProperty() {
        return canRedo;
    }

    /**
     * Returns true if the top of the undo stack matches the current context. Use this for
     * tab-scoped undo filtering.
     */
    public boolean isLastActionInContext(String contextId) {
        if (undoStack.isEmpty()) return false;
        if (contextId == null) return true;
        return contextId.equals(undoStack.peek().contextId());
    }

    private void updateProperties() {
        canUndo.set(!undoStack.isEmpty());
        canRedo.set(!redoStack.isEmpty());
    }
}
