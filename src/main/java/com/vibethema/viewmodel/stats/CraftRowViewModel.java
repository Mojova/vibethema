package com.vibethema.viewmodel.stats;

import com.vibethema.model.CraftAbility;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;

public class CraftRowViewModel {
    private final CraftAbility model;

    public CraftRowViewModel(CraftAbility model) {
        this.model = model;
    }

    public StringProperty expertiseProperty() {
        return model.expertiseProperty();
    }

    public IntegerProperty ratingProperty() {
        return model.ratingProperty();
    }

    public BooleanProperty favoredProperty() {
        return model.favoredProperty();
    }

    public BooleanProperty casteProperty() {
        return model.casteProperty();
    }
}
