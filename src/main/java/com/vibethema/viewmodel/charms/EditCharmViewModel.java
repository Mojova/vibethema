package com.vibethema.viewmodel.charms;

import com.vibethema.model.Ability;
import com.vibethema.model.Charm;
import com.vibethema.model.SystemData;
import com.vibethema.service.CharmDataService;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * ViewModel for editing or creating a Charm.
 * Encapsulates the logic for data binding and persistence.
 */
public class EditCharmViewModel implements ViewModel {

    private final Charm charm;
    private final CharmDataService dataService;
    private final String contextName;
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

    public EditCharmViewModel(Charm charm, CharmDataService dataService, String contextName, String filterType, Runnable onSave) {
        this.charm = charm;
        this.dataService = dataService;
        this.contextName = contextName;
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
        availableAbilities.addAll(SystemData.ABILITIES.stream()
                .map(Ability::getDisplayName)
                .collect(Collectors.toList()));
        availableAbilities.addAll(dataService.getAvailableMartialArtsStyles());
    }

    public void save() throws IOException {
        // Push changes to model
        charm.setName(name.get());
        charm.setAbility(ability.get());
        charm.setMinAbility(minAbility.get());
        charm.setMinEssence(minEssence.get());
        charm.setCost(cost.get());
        charm.setType(type.get());
        charm.setDuration(duration.get());
        charm.setFullText(fullText.get());

        // Persist
        if ("evocation".equals(charm.getCategory())) {
            dataService.saveEvocation(charm.getAbility(), contextName, charm);
        } else {
            dataService.saveCharm(charm);
        }

        if (onSave != null) {
            onSave.run();
        }
    }

    // Property Accessors
    public StringProperty nameProperty() { return name; }
    public StringProperty abilityProperty() { return ability; }
    public IntegerProperty minAbilityProperty() { return minAbility; }
    public IntegerProperty minEssenceProperty() { return minEssence; }
    public StringProperty costProperty() { return cost; }
    public StringProperty typeProperty() { return type; }
    public StringProperty durationProperty() { return duration; }
    public StringProperty fullTextProperty() { return fullText; }

    public ObservableList<String> getAvailableAbilities() { return availableAbilities; }

    public String getCharmName() { return charm.getName(); }
    public String getCharmCategory() { return charm.getCategory(); }
    public String getFilterType() { return filterType; }
}
