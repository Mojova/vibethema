package com.vibethema.viewmodel;

import com.vibethema.model.Caste;
import com.vibethema.model.CharacterData;
import com.vibethema.model.mystic.AnimaAbility;
import com.vibethema.service.AnimaDataService;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AnimaEffectsViewModel implements ViewModel {
    private final CharacterData data;
    private final AnimaDataService service;
    private final ObservableList<AnimaAbility> abilities = FXCollections.observableArrayList();
    private final ObservableList<AnimaAbility> readOnlyAbilities =
            FXCollections.unmodifiableObservableList(abilities);
    private final ReadOnlyStringWrapper message = new ReadOnlyStringWrapper();

    public AnimaEffectsViewModel(CharacterData data) {
        this(data, new AnimaDataService());
    }

    public AnimaEffectsViewModel(CharacterData data, AnimaDataService service) {
        this.data = data;
        this.service = service;
        data.casteProperty().addListener((obs, oldValue, newValue) -> refresh());
        data.exaltTypeProperty().addListener((obs, oldValue, newValue) -> refresh());
        refresh();
    }

    public void refresh() {
        if (data.casteProperty().get() == null || data.casteProperty().get() == Caste.NONE) {
            message.set("Select caste to see anima effects");
            abilities.clear();
            return;
        }
        var loaded =
                service.loadAbilities(data.exaltTypeProperty().get(), data.casteProperty().get());
        message.set(
                loaded.isEmpty()
                        ? "No anima effects available. Import the Core PDF to load anima effects."
                        : "");
        abilities.setAll(loaded);
    }

    public ObservableList<AnimaAbility> getAbilities() {
        return readOnlyAbilities;
    }

    public ReadOnlyStringProperty messageProperty() {
        return message.getReadOnlyProperty();
    }
}
