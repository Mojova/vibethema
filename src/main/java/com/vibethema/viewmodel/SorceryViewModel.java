package com.vibethema.viewmodel;

import com.vibethema.model.*;
import com.vibethema.model.mystic.*;
import com.vibethema.service.CharmDataService;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;

public class SorceryViewModel implements ViewModel {
    private final CharacterData data;
    private final CharmDataService charmDataService;
    private final BooleanProperty sorceryEnabled = new SimpleBooleanProperty();

    public SorceryViewModel(CharacterData data, CharmDataService charmDataService) {
        this.data = data;
        this.charmDataService = charmDataService;

        // Sorcery is enabled if the character has the "Terrestrial Circle Sorcery" charm
        sorceryEnabled.bind(
                Bindings.createBooleanBinding(
                        () -> data.hasCharmByName("Terrestrial Circle Sorcery"),
                        data.getUnlockedCharms()));
    }

    public ObservableList<ShapingRitual> getShapingRituals() {
        return data.getShapingRituals();
    }

    public ObservableList<Spell> getSpells() {
        return data.getSpells();
    }

    public BooleanProperty sorceryEnabledProperty() {
        return sorceryEnabled;
    }

    public CharacterData getData() {
        return data;
    }

    public CharmDataService getCharmDataService() {
        return charmDataService;
    }

    public void addShapingRitual() {
        data.getShapingRituals().add(new ShapingRitual("New Ritual", "Description..."));
        data.setDirty(true);
    }

    public void removeShapingRitual(ShapingRitual ritual) {
        data.getShapingRituals().remove(ritual);
        data.setDirty(true);
    }

    public void removeSpell(Spell spell) {
        data.getSpells().remove(spell);
        data.setDirty(true);
    }
}
