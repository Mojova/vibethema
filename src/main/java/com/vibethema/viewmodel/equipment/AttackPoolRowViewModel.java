package com.vibethema.viewmodel.equipment;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.social.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.logic.*;


import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class AttackPoolRowViewModel {
    private final AttackPoolData data;
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty withering = new SimpleStringProperty();
    private final StringProperty decisive = new SimpleStringProperty();
    private final StringProperty damage = new SimpleStringProperty();
    private final StringProperty parry = new SimpleStringProperty();

    public AttackPoolRowViewModel(AttackPoolData poolData) {
        this.data = poolData;
        
        name.set(data.getWeaponName());
        withering.set(data.getWitheringPool());
        decisive.set(String.valueOf(data.getDecisivePool()));
        damage.set(String.valueOf(data.getDamage()));
        parry.set(String.valueOf(data.getParry()));
        
        // Status indicator: "E" if equipped, otherwise empty
        status.bind(Bindings.createStringBinding(() -> 
            data.getWeapon().isEquipped() ? "E" : "", 
            data.getWeapon().equippedProperty()
        ));
    }

    public StringProperty nameProperty() { return name; }
    public StringProperty statusProperty() { return status; }
    public StringProperty witheringProperty() { return withering; }
    public StringProperty decisiveProperty() { return decisive; }
    public StringProperty damageProperty() { return damage; }
    public StringProperty parryProperty() { return parry; }
}