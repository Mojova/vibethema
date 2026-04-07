package com.vibethema.viewmodel.charms;

import com.vibethema.model.Ability;
import com.vibethema.model.CharacterData;
import com.vibethema.model.SystemData;
import com.vibethema.service.CharmDataService;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.Map;

public class CharmsViewModel implements ViewModel {
    private final CharacterData data;
    private final CharmDataService charmDataService;
    private final Map<String, String> keywordDefs;
    
    private final StringProperty filterType = new SimpleStringProperty("Ability");
    private final StringProperty selectedFilterValue = new SimpleStringProperty("");
    private final ObservableList<String> filterOptions = FXCollections.observableArrayList();

    public CharmsViewModel(CharacterData data, CharmDataService charmDataService, Map<String, String> keywordDefs, String initialFilterType) {
        this.data = data;
        this.charmDataService = charmDataService;
        this.keywordDefs = keywordDefs;
        this.filterType.set(initialFilterType);
        
        setupFilterOptions();
    }

    private void setupFilterOptions() {
        filterOptions.clear();
        if ("Ability".equals(filterType.get())) {
            for (Ability abil : SystemData.ABILITIES) {
                if (abil != Ability.MARTIAL_ARTS && abil != Ability.CRAFT) {
                    filterOptions.add(abil.getDisplayName());
                }
            }
            if (!filterOptions.isEmpty()) selectedFilterValue.set(filterOptions.get(0));
        } else if ("Martial Arts Style".equals(filterType.get())) {
            filterOptions.addAll(charmDataService.getAvailableMartialArtsStyles());
            if (!filterOptions.isEmpty()) selectedFilterValue.set(filterOptions.get(0));
        }
    }

    // Accessors
    public CharacterData getData() { return data; }
    public CharmDataService getCharmDataService() { return charmDataService; }
    public Map<String, String> getKeywordDefs() { return keywordDefs; }
    public StringProperty filterTypeProperty() { return filterType; }
    public StringProperty selectedFilterValueProperty() { return selectedFilterValue; }
    public ObservableList<String> getFilterOptions() { return filterOptions; }

    public void refreshStyles() {
        if ("Martial Arts Style".equals(filterType.get())) {
            String current = selectedFilterValue.get();
            setupFilterOptions();
            if (filterOptions.contains(current)) selectedFilterValue.set(current);
        }
    }
}
