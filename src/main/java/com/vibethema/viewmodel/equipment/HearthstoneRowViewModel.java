package com.vibethema.viewmodel.equipment;

import com.vibethema.model.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;
import com.vibethema.model.traits.*;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.StringProperty;

/** ViewModel for a single hearthstone row. */
public class HearthstoneRowViewModel implements ViewModel {
    private final Hearthstone hearthstone;

    public HearthstoneRowViewModel(Hearthstone hearthstone) {
        this.hearthstone = hearthstone;
    }

    public Hearthstone getHearthstone() {
        return hearthstone;
    }

    public StringProperty nameProperty() {
        return hearthstone.nameProperty();
    }

    public StringProperty idProperty() {
        return hearthstone.idProperty();
    }

    public StringProperty descriptionProperty() {
        return hearthstone.descriptionProperty();
    }

    public javafx.beans.property.BooleanProperty equippedProperty() {
        return hearthstone.equippedProperty();
    }
}
