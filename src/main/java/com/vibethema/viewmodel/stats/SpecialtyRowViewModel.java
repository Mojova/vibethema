package com.vibethema.viewmodel.stats;

import com.vibethema.model.traits.*;
import javafx.beans.property.StringProperty;

public class SpecialtyRowViewModel {
    private final Specialty model;

    public SpecialtyRowViewModel(Specialty model) {
        this.model = model;
    }

    public String getId() {
        return model.getId();
    }

    public StringProperty nameProperty() {
        return model.nameProperty();
    }

    public StringProperty abilityProperty() {
        return model.abilityProperty();
    }

    public Specialty getModel() {
        return model;
    }
}
