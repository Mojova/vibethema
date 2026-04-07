package com.vibethema.model.equipment;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.social.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.logic.*;


import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.util.UUID;

public class OtherEquipment {
    private final StringProperty id = new SimpleStringProperty(UUID.randomUUID().toString());
    private final StringProperty instanceId = new SimpleStringProperty(UUID.randomUUID().toString());
    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");
    private final javafx.beans.property.BooleanProperty artifact = new javafx.beans.property.SimpleBooleanProperty(false);
    private final javafx.beans.property.BooleanProperty equipped = new javafx.beans.property.SimpleBooleanProperty(false);
    private final javafx.beans.property.IntegerProperty attunement = new javafx.beans.property.SimpleIntegerProperty(0);

    public OtherEquipment(String name, String description) {
        this.name.set(name);
        this.description.set(description);
    }

    public OtherEquipment(String id, String name, String description) {
        this.id.set(id);
        this.instanceId.set(UUID.randomUUID().toString());
        this.name.set(name);
        this.description.set(description);
    }

    public OtherEquipment(String id, String instanceId, String name, String description) {
        this.id.set(id);
        this.instanceId.set(instanceId);
        this.name.set(name);
        this.description.set(description);
    }

    public StringProperty idProperty() { return id; }
    public String getId() { return id.get(); }

    public StringProperty instanceIdProperty() { return instanceId; }
    public String getInstanceId() { return instanceId.get(); }
    public void setInstanceId(String instanceId) { this.instanceId.set(instanceId); }

    public StringProperty nameProperty() { return name; }
    public String getName() { return name.get(); }
    public void setName(String v) { name.set(v); }

    public StringProperty descriptionProperty() { return description; }
    public String getDescription() { return description.get(); }
    public void setDescription(String v) { description.set(v); }

    public javafx.beans.property.BooleanProperty artifactProperty() { return artifact; }
    public boolean isArtifact() { return artifact.get(); }
    public void setArtifact(boolean v) { artifact.set(v); }

    public javafx.beans.property.BooleanProperty equippedProperty() { return equipped; }
    public boolean isEquipped() { return equipped.get(); }
    public void setEquipped(boolean v) { equipped.set(v); }

    public javafx.beans.property.IntegerProperty attunementProperty() { return attunement; }
    public int getAttunement() { return attunement.get(); }
    public void setAttunement(int v) { attunement.set(v); }

    // --- Persistence Support (DTO) ---
    public static class OtherEquipmentData {
        public String id;
        public String instanceId;
        public String name;
        public String description;
        public boolean artifact;
        public int attunement;
    }

    public OtherEquipmentData toData() {
        OtherEquipmentData data = new OtherEquipmentData();
        data.id = getId();
        data.instanceId = getInstanceId();
        data.name = getName();
        data.description = getDescription();
        data.artifact = isArtifact();
        data.attunement = getAttunement();
        return data;
    }

    public static OtherEquipment fromData(OtherEquipmentData data) {
        if (data == null) return null;
        OtherEquipment oe = new OtherEquipment(data.id, data.instanceId, data.name, data.description);
        oe.setArtifact(data.artifact);
        oe.setAttunement(data.attunement);
        return oe;
    }

    public OtherEquipment copy() {
        OtherEquipment oe = fromData(toData());
        oe.setInstanceId(UUID.randomUUID().toString());
        return oe;
    }

    @Override
    public String toString() {
        return getName();
    }
}