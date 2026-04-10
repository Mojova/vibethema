package com.vibethema.viewmodel.social;

import com.vibethema.model.social.Intimacy;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;

/** ViewModel for a single Intimacy row, wrapping the Intimacy model. */
public class IntimacyRowViewModel {
    private final Intimacy model;

    public IntimacyRowViewModel(Intimacy model) {
        this.model = model;
    }

    public Intimacy getModel() {
        return model;
    }

    public StringProperty nameProperty() {
        return model.nameProperty();
    }

    public ObjectProperty<Intimacy.Type> typeProperty() {
        return model.typeProperty();
    }

    public ObjectProperty<Intimacy.Intensity> intensityProperty() {
        return model.intensityProperty();
    }

    public StringProperty descriptionProperty() {
        return model.descriptionProperty();
    }
}
