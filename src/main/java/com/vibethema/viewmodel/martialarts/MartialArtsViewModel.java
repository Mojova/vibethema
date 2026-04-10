package com.vibethema.viewmodel.martialarts;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.service.CharmDataService;
import com.vibethema.viewmodel.util.Messenger;
import de.saxsys.mvvmfx.ViewModel;
import java.io.IOException;
import java.util.Map;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MartialArtsViewModel implements ViewModel {
    private static final Logger logger = LoggerFactory.getLogger(MartialArtsViewModel.class);

    private final CharacterData data;
    private final CharmDataService charmDataService;
    private final Map<String, String> keywordDefs;

    private final StringProperty filterType = new SimpleStringProperty("Martial Arts Style");
    private final StringProperty selectedFilterValue = new SimpleStringProperty("");
    private final ObservableList<String> filterOptions = FXCollections.observableArrayList();

    private final ObservableList<MartialArtsRowViewModel> styleRows =
            FXCollections.observableArrayList();
    private final ObservableList<String> availableStyles = FXCollections.observableArrayList();
    private final BooleanProperty martialArtsEnabled = new SimpleBooleanProperty();

    public MartialArtsViewModel(
            CharacterData data,
            CharmDataService charmDataService,
            Map<String, String> keywordDefs) {
        this.data = data;
        this.charmDataService = charmDataService;
        this.keywordDefs = keywordDefs;

        this.availableStyles.addAll(charmDataService.getAvailableMartialArtsStyles());

        this.martialArtsEnabled.bind(
                Bindings.createBooleanBinding(
                        () -> data.hasMeritByDefinitionId("3dd46a27-92bf-3295-9a1b-dba09c281581"),
                        data.getMerits()));

        updateRows();

        // Listen for rating changes to trigger UI refreshes (e.g. charm eligibility)
        for (MartialArtsStyle style : data.getMartialArtsStyles()) {
            style.ratingProperty()
                    .addListener((obs, oldV, newV) -> Messenger.publish("refresh_all_ui"));
        }

        data.getMartialArtsStyles()
                .addListener(
                        (ListChangeListener<? super MartialArtsStyle>)
                                c -> {
                                    while (c.next()) {
                                        if (c.wasAdded()) {
                                            for (MartialArtsStyle style : c.getAddedSubList()) {
                                                style.ratingProperty()
                                                        .addListener(
                                                                (obs, oldV, newV) ->
                                                                        Messenger.publish(
                                                                                "refresh_all_ui"));
                                            }
                                        }
                                        if (c.wasAdded() || c.wasRemoved()) {
                                            updateRows();
                                            setupFilterOptions();
                                        }
                                    }
                                });

        setupFilterOptions();
    }

    private void updateRows() {
        java.util.Set<MartialArtsStyle> currentModels =
                new java.util.HashSet<>(data.getMartialArtsStyles());
        styleRows.removeIf(row -> !currentModels.contains(row.getModel()));

        for (int i = 0; i < data.getMartialArtsStyles().size(); i++) {
            MartialArtsStyle model = data.getMartialArtsStyles().get(i);
            if (i >= styleRows.size() || styleRows.get(i).getModel() != model) {
                int existingIdx = -1;
                for (int j = i + 1; j < styleRows.size(); j++) {
                    if (styleRows.get(j).getModel() == model) {
                        existingIdx = j;
                        break;
                    }
                }
                if (existingIdx != -1) {
                    MartialArtsRowViewModel moved = styleRows.remove(existingIdx);
                    styleRows.add(i, moved);
                } else {
                    styleRows.add(
                            i,
                            new MartialArtsRowViewModel(data, model, this::createNewDatabaseStyle));
                }
            }
        }
    }

    private void setupFilterOptions() {
        String current = selectedFilterValue.get();
        filterOptions.clear();
        for (MartialArtsStyle style : data.getMartialArtsStyles()) {
            filterOptions.add(style.getStyleName());
        }

        if (!filterOptions.isEmpty()) {
            if (current != null && filterOptions.contains(current)) {
                selectedFilterValue.set(current);
            } else {
                selectedFilterValue.set(filterOptions.get(0));
            }
        } else {
            selectedFilterValue.set("");
        }
    }

    public void addStyle() {
        MartialArtsStyle newStyle =
                new MartialArtsStyle(java.util.UUID.randomUUID().toString(), "", 0);
        data.getMartialArtsStyles().add(newStyle);
        // Find the row and trigger editing
        styleRows.stream()
                .filter(r -> r.getModel() == newStyle)
                .findFirst()
                .ifPresent(MartialArtsRowViewModel::beginEdit);
        data.setDirty(true);
    }

    public void addStyle(String name) {
        if (data.getMartialArtsStyles().stream().anyMatch(s -> s.getStyleName().equals(name))) {
            return; // Already added
        }
        data.getMartialArtsStyles()
                .add(new MartialArtsStyle(java.util.UUID.randomUUID().toString(), name, 0));
        data.setDirty(true);
    }

    public void removeStyle(MartialArtsStyle style) {
        data.getMartialArtsStyles().remove(style);
        data.setDirty(true);
    }

    public void createNewDatabaseStyle(String id, String name) {
        try {
            charmDataService.createNewMartialArtsStyle(id, name);
            // Refresh available styles
            availableStyles.clear();
            availableStyles.addAll(charmDataService.getAvailableMartialArtsStyles());
        } catch (IOException e) {
            logger.error("Failed to create new martial arts style in database", e);
        }
    }

    // Accessors
    public CharacterData getData() {
        return data;
    }

    public CharmDataService getCharmDataService() {
        return charmDataService;
    }

    public Map<String, String> getKeywordDefs() {
        return keywordDefs;
    }

    public StringProperty filterTypeProperty() {
        return filterType;
    }

    public StringProperty selectedFilterValueProperty() {
        return selectedFilterValue;
    }

    public ObservableList<String> getFilterOptions() {
        return filterOptions;
    }

    public ObservableList<MartialArtsRowViewModel> getStyleRows() {
        return styleRows;
    }

    public ObservableList<String> getAvailableStyles() {
        return availableStyles;
    }

    public BooleanProperty martialArtsEnabledProperty() {
        return martialArtsEnabled;
    }

    public void refreshStyles() {
        availableStyles.clear();
        availableStyles.addAll(charmDataService.getAvailableMartialArtsStyles());
        setupFilterOptions();
    }
}
