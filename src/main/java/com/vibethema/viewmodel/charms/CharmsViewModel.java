package com.vibethema.viewmodel.charms;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.service.CharmDataService;
import de.saxsys.mvvmfx.ViewModel;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class CharmsViewModel implements ViewModel {
    private final CharacterData data;
    private final CharmDataService charmDataService;
    private final Map<String, String> keywordDefs;

    private final Map<String, String> artifactSelections = new java.util.HashMap<>();
    private final StringProperty filterType = new SimpleStringProperty("Ability");
    private final StringProperty selectedFilterValue = new SimpleStringProperty("");
    private final ObservableList<String> filterOptions = FXCollections.observableArrayList();

    public CharmsViewModel(
            CharacterData data,
            CharmDataService charmDataService,
            Map<String, String> keywordDefs,
            String initialFilterType) {
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
                if (abil != Ability.MARTIAL_ARTS) {
                    filterOptions.add(abil.getDisplayName());
                }
            }
            if (!filterOptions.isEmpty()) selectedFilterValue.set(filterOptions.get(0));
        }
    }

    public record TreeSelection(String type, String id, String name) {}

    public TreeSelection getTreeSelection() {
        String selected = selectedFilterValue.get();
        String artifactId = artifactSelections.get(selected);
        return artifactId == null
                ? new TreeSelection(filterType.get(), selected, selected)
                : new TreeSelection("Evocation", artifactId, selected);
    }

    /** Adds a named artifact destination without exposing definition IDs in the filter. */
    public String selectArtifact(String artifactId, String name) {
        String label = "Evocations: " + name;
        int suffix = 2;
        while (artifactSelections.containsKey(label)
                && !java.util.Objects.equals(artifactSelections.get(label), artifactId)) {
            label = "Evocations: " + name + " (" + suffix++ + ")";
        }
        artifactSelections.put(label, artifactId);
        if (!filterOptions.contains(label)) filterOptions.add(label);
        selectedFilterValue.set(label);
        return label;
    }

    public void selectAbility(String ability) {
        if (!filterOptions.contains(ability)) filterOptions.add(ability);
        selectedFilterValue.set(ability);
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
        // No longer handles Martial Arts Style directly
    }
}
