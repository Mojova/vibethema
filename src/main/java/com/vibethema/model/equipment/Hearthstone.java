package com.vibethema.model.equipment;

import java.util.UUID;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Hearthstone {
    private final StringProperty id = new SimpleStringProperty(UUID.randomUUID().toString());
    private final StringProperty instanceId =
            new SimpleStringProperty(UUID.randomUUID().toString());
    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");
    private final javafx.beans.property.BooleanProperty equipped =
            new javafx.beans.property.SimpleBooleanProperty(false);

    public Hearthstone(String name, String description) {
        this.name.set(name);
        this.description.set(description);
    }

    public Hearthstone(String id, String name, String description) {
        this.id.set(id);
        this.instanceId.set(UUID.randomUUID().toString());
        this.name.set(name);
        this.description.set(description);
    }

    public Hearthstone(String id, String instanceId, String name, String description) {
        this.id.set(id);
        this.instanceId.set(instanceId);
        this.name.set(name);
        this.description.set(description);
    }

    public StringProperty idProperty() {
        return id;
    }

    public String getId() {
        return id.get();
    }

    public StringProperty instanceIdProperty() {
        return instanceId;
    }

    public String getInstanceId() {
        return instanceId.get();
    }

    public void setInstanceId(String instanceId) {
        this.instanceId.set(instanceId);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String v) {
        name.set(v);
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public String getDescription() {
        return description.get();
    }

    public void setDescription(String v) {
        description.set(v);
    }

    public javafx.beans.property.BooleanProperty equippedProperty() {
        return equipped;
    }

    public boolean isEquipped() {
        return equipped.get();
    }

    public void setEquipped(boolean v) {
        equipped.set(v);
    }

    // --- Persistence Support (DTO) ---
    public static class HearthstoneData {
        public String id;
        public String instanceId;
        public String name;
        public String description;
    }

    public HearthstoneData toData() {
        HearthstoneData data = new HearthstoneData();
        data.id = getId();
        data.instanceId = getInstanceId();
        data.name = getName();
        data.description = getDescription();
        return data;
    }

    public static Hearthstone fromData(HearthstoneData data) {
        if (data == null) return null;
        return new Hearthstone(data.id, data.instanceId, data.name, data.description);
    }

    public Hearthstone copy() {
        Hearthstone h = fromData(toData());
        h.setInstanceId(UUID.randomUUID().toString());
        return h;
    }

    @Override
    public String toString() {
        return getName();
    }
}
