package com.vibethema.viewmodel;

import com.vibethema.model.*;
import com.vibethema.model.social.*;
import com.vibethema.viewmodel.social.IntimacyRowViewModel;
import de.saxsys.mvvmfx.ViewModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class IntimaciesViewModel implements ViewModel {
    private final CharacterData data;
    private final ObservableList<IntimacyRowViewModel> intimacyRows =
            FXCollections.observableArrayList();

    public IntimaciesViewModel(CharacterData data) {
        this.data = data;

        // Sync rows surgically to preserve stability and focus
        updateRows();
        data.getIntimacies()
                .addListener(
                        (javafx.collections.ListChangeListener<? super Intimacy>)
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
        intimacyRows.setAll(
                data.getIntimacies().stream()
                        .map(IntimacyRowViewModel::new)
                        .collect(java.util.stream.Collectors.toList()));
    }

    public ObservableList<IntimacyRowViewModel> getIntimacyRows() {
        return intimacyRows;
    }

    public void addIntimacy() {
        data.getIntimacies()
                .add(
                        new Intimacy(
                                java.util.UUID.randomUUID().toString(),
                                "",
                                Intimacy.Type.TIE,
                                Intimacy.Intensity.MINOR));
        data.setDirty(true);
    }

    public void removeIntimacy(Intimacy model) {
        data.getIntimacies().remove(model);
        data.setDirty(true);
    }
}
