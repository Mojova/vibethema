package com.vibethema.viewmodel;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.social.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.logic.*;


import de.saxsys.mvvmfx.ViewModel;
import javafx.collections.ObservableList;

public class MeritsViewModel implements ViewModel {
    private final CharacterData data;

    public MeritsViewModel(CharacterData data) {
        this.data = data;
    }

    public ObservableList<Merit> getMerits() {
        return data.getMerits();
    }

    public void addMerit() {
        data.getMerits().add(new Merit("", 1));
    }

    public void removeMerit(Merit merit) {
        data.getMerits().remove(merit);
    }
    
    public CharacterData getData() {
        return data;
    }
}