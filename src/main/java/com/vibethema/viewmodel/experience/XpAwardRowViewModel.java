package com.vibethema.viewmodel.experience;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.social.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.logic.*;


import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.*;

public class XpAwardRowViewModel implements ViewModel {

    private final XpAward award;

    public XpAwardRowViewModel(XpAward award) {
        this.award = award;
    }

    public XpAward getAward() { return award; }

    public StringProperty descriptionProperty() { return award.descriptionProperty(); }
    public IntegerProperty amountProperty() { return award.amountProperty(); }
    public BooleanProperty isSolarProperty() { return award.isSolarProperty(); }

    public String getDescription() { return award.getDescription(); }
    public int getAmount() { return award.getAmount(); }
    public boolean isSolar() { return award.isSolar(); }
}