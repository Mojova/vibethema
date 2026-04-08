package com.vibethema.viewmodel.equipment;

import com.vibethema.model.equipment.*;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

/** ViewModel for a single other equipment row. */
public class OtherEquipmentRowViewModel implements ViewModel {
    private final OtherEquipment equipment;

    public OtherEquipmentRowViewModel(OtherEquipment equipment) {
        this.equipment = equipment;
    }

    public OtherEquipment getOtherEquipment() {
        return equipment;
    }

    public StringProperty nameProperty() {
        return equipment.nameProperty();
    }

    public StringProperty idProperty() {
        return equipment.idProperty();
    }

    public StringProperty descriptionProperty() {
        return equipment.descriptionProperty();
    }

    public BooleanProperty artifactProperty() {
        return equipment.artifactProperty();
    }

    public BooleanProperty equippedProperty() {
        return equipment.equippedProperty();
    }

    public javafx.beans.property.IntegerProperty attunementProperty() {
        return equipment.attunementProperty();
    }
}
