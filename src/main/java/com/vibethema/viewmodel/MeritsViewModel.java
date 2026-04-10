package com.vibethema.viewmodel;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.viewmodel.stats.MeritRowViewModel;
import de.saxsys.mvvmfx.ViewModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class MeritsViewModel implements ViewModel {
    private final CharacterData data;
    private final ObservableList<MeritRowViewModel> meritRows = FXCollections.observableArrayList();

    public MeritsViewModel(CharacterData data) {
        this.data = data;

        // Sync rows surgically to preserve stability and focus
        updateRows();
        data.getMerits()
                .addListener(
                        (javafx.collections.ListChangeListener<? super Merit>)
                                c -> {
                                    while (c.next()) {
                                        if (c.wasAdded() || c.wasRemoved()) {
                                            updateRows();
                                            break;
                                        }
                                    }
                                });
    }

    private void updateRows() {
        meritRows.setAll(
                data.getMerits().stream()
                        .map(MeritRowViewModel::new)
                        .collect(java.util.stream.Collectors.toList()));
    }

    public ObservableList<MeritRowViewModel> getMeritRows() {
        return meritRows;
    }

    public void addMerit() {
        data.getMerits().add(new Merit(java.util.UUID.randomUUID().toString(), "", 1));
        data.setDirty(true);
    }

    public void removeMerit(Merit model) {
        data.getMerits().remove(model);
        data.setDirty(true);
    }

    public CharacterData getData() {
        return data;
    }
}
