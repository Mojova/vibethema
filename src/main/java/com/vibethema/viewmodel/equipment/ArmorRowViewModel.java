package com.vibethema.viewmodel.equipment;

import com.vibethema.model.equipment.*;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;

/** ViewModel for a single armor row. */
public class ArmorRowViewModel implements ViewModel {
    private final Armor armor;

    public ArmorRowViewModel(Armor armor) {
        this.armor = armor;
    }

    public Armor getArmor() {
        return armor;
    }

    public StringProperty nameProperty() {
        return armor.nameProperty();
    }

    public StringProperty idProperty() {
        return armor.idProperty();
    }

    public BooleanProperty equippedProperty() {
        return armor.equippedProperty();
    }

    public IntegerProperty soakProperty() {
        return armor.soakProperty();
    }

    public IntegerProperty hardnessProperty() {
        return armor.hardnessProperty();
    }

    public IntegerProperty mobilityPenaltyProperty() {
        return armor.mobilityPenaltyProperty();
    }

    public IntegerProperty attunementProperty() {
        return armor.attunementProperty();
    }

    public boolean isArtifact() {
        return armor.getType() == Armor.ArmorType.ARTIFACT;
    }
}
