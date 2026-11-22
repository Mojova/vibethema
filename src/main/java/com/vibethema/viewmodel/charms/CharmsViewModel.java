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

    private record ArtifactSelection(String id, String name) {}

    private final Map<String, ArtifactSelection> artifactSelections =
            new java.util.LinkedHashMap<>();
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
        ArtifactSelection artifact = artifactSelections.get(selected);
        return artifact == null
                ? new TreeSelection(filterType.get(), selected, selected)
                : new TreeSelection("Evocation", artifact.id(), artifact.name());
    }

    /** Adds a named artifact destination without exposing instance IDs in the filter. */
    public String selectArtifact(String instanceId, String name) {
        String label = addArtifactOption(new ArtifactSelection(instanceId, name));
        selectedFilterValue.set(label);
        return label;
    }

    private String addArtifactOption(ArtifactSelection artifact) {
        String label = "Evocations: " + artifact.name();
        int suffix = 2;
        while (artifactSelections.containsKey(label)) {
            if (artifactSelections.get(label).equals(artifact)) return label;
            label = "Evocations: " + artifact.name() + " (" + suffix++ + ")";
        }
        artifactSelections.put(label, artifact);
        filterOptions.add(label);
        return label;
    }

    public void refreshArtifactOptions() {
        if (artifactSelections.values().stream()
                .allMatch(artifact ->
                        hasArtifactDestination(artifact.id())
                                && artifact.name().equals(data.getArtifactName(artifact.id()))))
            return;

        String selection = selectedFilterValue.get();
        ArtifactSelection selectedArtifact = artifactSelections.get(selection);
        var existingArtifacts = java.util.List.copyOf(artifactSelections.values());
        filterOptions.removeAll(artifactSelections.keySet());
        artifactSelections.clear();
        for (ArtifactSelection artifact : existingArtifacts) {
            if (!hasArtifactDestination(artifact.id())) continue;
            String label =
                    addArtifactOption(
                            new ArtifactSelection(
                                    artifact.id(), data.getArtifactName(artifact.id())));
            if (selectedArtifact != null && selectedArtifact.id().equals(artifact.id()))
                selection = label;
        }
        selectedFilterValue.set(
                filterOptions.contains(selection) ? selection : filterOptions.getFirst());
    }

    private boolean hasArtifactDestination(String id) {
        return data.isArtifactPossessed(id) || !data.getEvocations(id).isEmpty();
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
