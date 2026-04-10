package com.vibethema.viewmodel.martialarts;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.service.CharmDataService;
import de.saxsys.mvvmfx.ViewModel;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class MartialArtsViewModel implements ViewModel {
    private final CharacterData data;
    private final CharmDataService charmDataService;
    private final Map<String, String> keywordDefs;

    private final StringProperty filterType = new SimpleStringProperty("Martial Arts Style");
    private final StringProperty selectedFilterValue = new SimpleStringProperty("");
    private final ObservableList<String> filterOptions = FXCollections.observableArrayList();

    public MartialArtsViewModel(
            CharacterData data,
            CharmDataService charmDataService,
            Map<String, String> keywordDefs) {
        this.data = data;
        this.charmDataService = charmDataService;
        this.keywordDefs = keywordDefs;

        setupFilterOptions();
    }

    private void setupFilterOptions() {
        filterOptions.clear();
        filterOptions.addAll(charmDataService.getAvailableMartialArtsStyles());
        if (!filterOptions.isEmpty()) {
            selectedFilterValue.set(filterOptions.get(0));
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

    public void refreshStyles() {
        String current = selectedFilterValue.get();
        setupFilterOptions();
        if (filterOptions.contains(current)) {
            selectedFilterValue.set(current);
        }
    }
}
