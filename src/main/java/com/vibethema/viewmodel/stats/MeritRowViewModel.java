package com.vibethema.viewmodel.stats;

import com.vibethema.model.traits.Merit;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;

/** ViewModel for a single Merit row, wrapping the Merit model. */
public class MeritRowViewModel {
    private final Merit model;

    public MeritRowViewModel(Merit model) {
        this.model = model;
    }

    public Merit getModel() {
        return model;
    }

    public StringProperty nameProperty() {
        return model.nameProperty();
    }

    public IntegerProperty ratingProperty() {
        return model.ratingProperty();
    }
}
