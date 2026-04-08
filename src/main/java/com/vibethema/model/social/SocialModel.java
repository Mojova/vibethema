package com.vibethema.model.social;

import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class SocialModel {
    private final ObservableList<Intimacy> intimacies =
            FXCollections.observableArrayList(
                    i ->
                            new javafx.beans.Observable[] {
                                i.nameProperty(), i.intensityProperty(), i.descriptionProperty()
                            });

    private final Consumer<Void> dirtyListener;

    public SocialModel(Consumer<Void> dirtyListener) {
        this.dirtyListener = dirtyListener;

        intimacies.addListener(
                (javafx.collections.ListChangeListener<? super Intimacy>) c -> markDirty());
    }

    private void markDirty() {
        dirtyListener.accept(null);
    }

    public ObservableList<Intimacy> getIntimacies() {
        return intimacies;
    }
}
