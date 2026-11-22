package com.vibethema.viewmodel.charms;

import com.vibethema.model.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.traits.*;
import com.vibethema.service.CharmDataService;
import com.vibethema.viewmodel.MainViewModel.CheckpointRequest;
import com.vibethema.viewmodel.util.Messenger;
import de.saxsys.mvvmfx.ViewModel;
import java.io.IOException;
import java.util.stream.Collectors;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * ViewModel for editing or creating a Charm. Encapsulates the logic for data binding and
 * persistence.
 */
public class EditCharmViewModel implements ViewModel {

    private final Charm charm;
    private final CharmDataService dataService;
    private final CharacterData character;
    private final java.util.List<String> initialPrerequisiteIds;
    private final String filterType;
    private final Runnable onSave;

    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty ability = new SimpleStringProperty();
    private final IntegerProperty minAbility = new SimpleIntegerProperty();
    private final IntegerProperty minEssence = new SimpleIntegerProperty();
    private final StringProperty cost = new SimpleStringProperty();
    private final StringProperty type = new SimpleStringProperty();
    private final StringProperty duration = new SimpleStringProperty();
    private final StringProperty fullText = new SimpleStringProperty();

    private final ObservableList<String> availableAbilities = FXCollections.observableArrayList();
    private final ObservableList<Charm> availablePrerequisites =
            FXCollections.observableArrayList();
    private final ObservableList<Charm> selectedPrerequisites = FXCollections.observableArrayList();
    private final ObservableList<String> availableKeywords = FXCollections.observableArrayList();
    private final ObservableList<String> selectedKeywords = FXCollections.observableArrayList();

    public EditCharmViewModel(
            Charm charm,
            CharmDataService dataService,
            String contextName,
            String filterType,
            Runnable onSave) {
        this(charm, dataService, contextName, filterType, onSave, null);
    }

    public EditCharmViewModel(
            Charm charm,
            CharmDataService dataService,
            String contextName,
            String filterType,
            Runnable onSave,
            CharacterData character) {
        this.charm =
                charm instanceof Evocation evocation
                        ? evocation.copyForArtifact(evocation.getArtifactId())
                        : charm;
        this.dataService = dataService;
        this.character = character;
        this.filterType = filterType;
        this.onSave = onSave;

        // Initialize properties from model
        this.name.set(charm.getName());
        this.ability.set(charm.getAbility());
        this.minAbility.set(charm.getMinAbility());
        this.minEssence.set(charm.getMinEssence());
        this.cost.set(charm.getCost());
        this.type.set(charm.getType());
        this.duration.set(charm.getDuration());
        this.fullText.set(charm.getFullText());

        // Load available abilities
        availableAbilities.addAll(
                SystemData.ABILITIES.stream()
                        .map(Ability::getDisplayName)
                        .collect(Collectors.toList()));
        availableAbilities.addAll(dataService.getAvailableMartialArtsStyles());

        // Load available keywords and initialize selections
        availableKeywords.addAll(
                dataService.loadKeywords().stream()
                        .map(com.vibethema.model.mystic.Keyword::getName)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .collect(Collectors.toList()));

        if (charm.getKeywords() != null) {
            selectedKeywords.addAll(charm.getKeywords());
        }

        // Initial prerequisite load
        refreshAvailablePrerequisites();

        // Populate current selections from model
        if (charm.getPrerequisites() != null) {
            java.util.Set<String> prereqIds = new java.util.HashSet<>();
            charm.getPrerequisites().collectCharmIds(prereqIds);

            selectedPrerequisites.addAll(
                    availablePrerequisites.stream()
                            .filter(c -> prereqIds.contains(c.getId()))
                            .collect(Collectors.toList()));
        }

        initialPrerequisiteIds = selectedPrerequisites.stream().map(Charm::getId).toList();

        // Listener to refresh prerequisites when ability changes
        this.ability.addListener((obs, oldV, newV) -> {
            if (newV != null && !newV.equals(oldV)) {
                refreshAvailablePrerequisites();
                // Clear selections as they might not be valid for the new ability/group
                selectedPrerequisites.clear();
            }
        });
    }

    private void refreshAvailablePrerequisites() {
        String currentAbility = ability.get();
        if (currentAbility == null || currentAbility.isEmpty()) {
            availablePrerequisites.clear();
            return;
        }

        java.util.List<Charm> allCharms;
        if (charm instanceof Evocation && character != null) {
            allCharms = new java.util.ArrayList<>(character.getEvocations(currentAbility));
            if (charm.getPrerequisites() != null) {
                java.util.Set<String> referenced = new java.util.HashSet<>();
                charm.getPrerequisites().collectCharmIds(referenced);
                for (String id : referenced) {
                    Charm prerequisite = dataService.findCharmById(id);
                    if (prerequisite != null) allCharms.add(prerequisite);
                }
            }
        } else {
            allCharms = dataService.loadCharmsForAbility(currentAbility);
        }
        availablePrerequisites.setAll(
                allCharms.stream()
                        .filter(c -> !c.getId().equals(charm.getId()))
                        .sorted(
                                java.util.Comparator.comparing(
                                        Charm::getName, String.CASE_INSENSITIVE_ORDER))
                        .collect(Collectors.toList()));
    }

    public void save() throws IOException {
        // Push changes to model
        charm.setName(name.get());
        if (!(charm instanceof Evocation)) charm.setAbility(ability.get());
        charm.setMinAbility(minAbility.get());
        charm.setMinEssence(minEssence.get());
        charm.setCost(cost.get());
        charm.setType(type.get());
        charm.setDuration(duration.get());
        charm.setFullText(fullText.get());

        // Save Keywords
        charm.setKeywords(new java.util.ArrayList<>(selectedKeywords));

        // Save manually selected prerequisites as a clean all-of expression.
        var selectedIds = selectedPrerequisites.stream().map(Charm::getId).toList();
        if (!(charm instanceof Evocation) || !selectedIds.equals(initialPrerequisiteIds)) {
            charm.setPrerequisites(
                    PrerequisiteRule.allOf(
                            selectedIds.stream().map(PrerequisiteRule::charm).toList()));
        }

        // Persist
        if (charm instanceof Evocation evocation) {
            if (character == null) throw new IOException("Select an owned artifact first.");
            Messenger.publish(
                    "RECORD_UNDO_CHECKPOINT",
                    new CheckpointRequest(
                            "Charms",
                            "Save Evocation: " + charm.getName(),
                            "charm_" + charm.getId()));
            try {
                character.saveEvocation(evocation);
            } catch (IllegalArgumentException ex) {
                throw new IOException(ex.getMessage(), ex);
            }
            Messenger.publish("refresh_all_ui");
        } else {
            dataService.saveCharm(charm);
        }

        if (onSave != null) {
            onSave.run();
        }
    }

    // Property Accessors
    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty abilityProperty() {
        return ability;
    }

    public IntegerProperty minAbilityProperty() {
        return minAbility;
    }

    public IntegerProperty minEssenceProperty() {
        return minEssence;
    }

    public StringProperty costProperty() {
        return cost;
    }

    public StringProperty typeProperty() {
        return type;
    }

    public StringProperty durationProperty() {
        return duration;
    }

    public StringProperty fullTextProperty() {
        return fullText;
    }

    public ObservableList<String> getAvailableAbilities() {
        return availableAbilities;
    }

    public ObservableList<Charm> getAvailablePrerequisites() {
        return availablePrerequisites;
    }

    public ObservableList<Charm> getSelectedPrerequisites() {
        return selectedPrerequisites;
    }

    public ObservableList<String> getAvailableKeywords() {
        return availableKeywords;
    }

    public ObservableList<String> getSelectedKeywords() {
        return selectedKeywords;
    }

    public String getCharmName() {
        return charm.getName();
    }

    public String getFilterType() {
        return filterType;
    }
}
