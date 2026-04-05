package com.vibethema.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.util.UUID;

public class OtherEquipment {
    private final StringProperty id = new SimpleStringProperty(UUID.randomUUID().toString());
    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");
    private final javafx.beans.property.BooleanProperty artifact = new javafx.beans.property.SimpleBooleanProperty(false);

    public OtherEquipment(String name, String description) {
        this.name.set(name);
        this.description.set(description);
    }

    public OtherEquipment(String id, String name, String description) {
        this.id.set(id);
        this.name.set(name);
        this.description.set(description);
    }

    public StringProperty idProperty() { return id; }
    public String getId() { return id.get(); }

    public StringProperty nameProperty() { return name; }
    public String getName() { return name.get(); }
    public void setName(String v) { name.set(v); }

    public StringProperty descriptionProperty() { return description; }
    public String getDescription() { return description.get(); }
    public void setDescription(String v) { description.set(v); }

    public javafx.beans.property.BooleanProperty artifactProperty() { return artifact; }
    public boolean isArtifact() { return artifact.get(); }
    public void setArtifact(boolean v) { artifact.set(v); }

    // --- Persistence Support (DTO) ---
    public static class OtherEquipmentData {
        public String id;
        public String name;
        public String description;
        public boolean artifact;
    }

    public OtherEquipmentData toData() {
        OtherEquipmentData data = new OtherEquipmentData();
        data.id = getId();
        data.name = getName();
        data.description = getDescription();
        data.artifact = isArtifact();
        return data;
    }

    public static OtherEquipment fromData(OtherEquipmentData data) {
        if (data == null) return null;
        OtherEquipment oe = new OtherEquipment(data.id, data.name, data.description);
        oe.setArtifact(data.artifact);
        return oe;
    }

    @Override
    public String toString() {
        return getName();
    }
}
