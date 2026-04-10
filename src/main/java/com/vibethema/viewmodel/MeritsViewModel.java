package com.vibethema.viewmodel;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.service.MeritService;
import com.vibethema.viewmodel.stats.MeritRowViewModel;
import de.saxsys.mvvmfx.ViewModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class MeritsViewModel implements ViewModel {
    private final CharacterData data;
    private final ObservableList<MeritRowViewModel> meritRows = FXCollections.observableArrayList();
    private final ObservableList<MeritReference> availableMerits =
            FXCollections.observableArrayList();
    private final MeritService meritService = new MeritService();

    public MeritsViewModel(CharacterData data) {
        this.data = data;
        this.availableMerits.addAll(meritService.getAvailableMerits());

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
        // Surgical sync: remove missing, add new
        java.util.Set<Merit> currentModels = new java.util.HashSet<>(data.getMerits());
        meritRows.removeIf(row -> !currentModels.contains(row.getModel()));

        for (int i = 0; i < data.getMerits().size(); i++) {
            Merit model = data.getMerits().get(i);
            if (i >= meritRows.size() || meritRows.get(i).getModel() != model) {
                // Check if it exists elsewhere and was moved, or should be inserted
                int existingIdx = -1;
                for (int j = i + 1; j < meritRows.size(); j++) {
                    if (meritRows.get(j).getModel() == model) {
                        existingIdx = j;
                        break;
                    }
                }
                if (existingIdx != -1) {
                    MeritRowViewModel moved = meritRows.remove(existingIdx);
                    meritRows.add(i, moved);
                } else {
                    meritRows.add(i, new MeritRowViewModel(model));
                }
            }
        }
    }

    public ObservableList<MeritRowViewModel> getMeritRows() {
        return meritRows;
    }

    public ObservableList<MeritReference> getAvailableMerits() {
        return availableMerits;
    }

    public void addMerit() {
        data.getMerits().add(new Merit(java.util.UUID.randomUUID().toString(), "", 1));
        data.setDirty(true);
    }

    public void addMerit(MeritReference ref) {
        int initialRating =
                (ref.getRatings() != null && !ref.getRatings().isEmpty())
                        ? ref.getRatings().get(0)
                        : 1;
        data.getMerits()
                .add(
                        new Merit(
                                java.util.UUID.randomUUID().toString(),
                                ref.getId(),
                                ref.getName(),
                                initialRating,
                                ref.getDescription()));
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
