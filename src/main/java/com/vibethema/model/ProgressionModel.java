package com.vibethema.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.function.Consumer;

public class ProgressionModel {
    private final ObservableList<XpAward> xpAwards = FXCollections.observableArrayList(
            x -> new javafx.beans.Observable[] { x.descriptionProperty(), x.amountProperty(), x.isSolarProperty() });

    private ObjectProperty<CharacterSaveState> creationSnapshot = new SimpleObjectProperty<>(null);
    private final Consumer<Void> dirtyListener;

    public ProgressionModel(Consumer<Void> dirtyListener) {
        this.dirtyListener = dirtyListener;

        xpAwards.addListener((javafx.collections.ListChangeListener<? super XpAward>) c -> markDirty());
    }

    private void markDirty() {
        dirtyListener.accept(null);
    }

    public ObservableList<XpAward> getXpAwards() { return xpAwards; }
    public ObjectProperty<CharacterSaveState> creationSnapshotProperty() { return creationSnapshot; }
    public CharacterSaveState getCreationSnapshot() { return creationSnapshot.get(); }
    public void setCreationSnapshot(CharacterSaveState snapshot) { creationSnapshot.set(snapshot); }
}
