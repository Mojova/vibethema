package com.vibethema.viewmodel.stats;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.social.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.logic.*;


import javafx.beans.property.StringProperty;

public class SpecialtyRowViewModel {
    private final Specialty model;

    public SpecialtyRowViewModel(Specialty model) {
        this.model = model;
    }

    public StringProperty nameProperty() {
        return model.nameProperty();
    }

    public StringProperty abilityProperty() {
        return model.abilityProperty();
    }
}