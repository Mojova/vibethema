package com.vibethema.viewmodel.equipment;

import com.vibethema.model.Weapon;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;

/**
 * ViewModel for a single weapon row in the equipment list.
 */
public class WeaponRowViewModel implements ViewModel {
    private final Weapon weapon;

    public WeaponRowViewModel(Weapon weapon) {
        this.weapon = weapon;
    }

    public Weapon getWeapon() {
        return weapon;
    }

    public StringProperty nameProperty() { return weapon.nameProperty(); }
    public StringProperty idProperty() { return weapon.idProperty(); }
    public IntegerProperty accuracyProperty() { return weapon.accuracyProperty(); }
    public IntegerProperty damageProperty() { return weapon.damageProperty(); }
    public IntegerProperty defenseProperty() { return weapon.defenseProperty(); }
    public IntegerProperty overwhelmingProperty() { return weapon.overwhelmingProperty(); }
    public IntegerProperty attunementProperty() { return weapon.attunementProperty(); }
    public StringProperty specialtyIdProperty() { return weapon.specialtyIdProperty(); }
    
    public boolean isArtifact() {
        return weapon.getType() == Weapon.WeaponType.ARTIFACT;
    }
}
