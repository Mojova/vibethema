package com.vibethema.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.util.UUID;

public class Hearthstone {
    private final StringProperty id = new SimpleStringProperty(UUID.randomUUID().toString());
    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");
    private final javafx.beans.property.BooleanProperty equipped = new javafx.beans.property.SimpleBooleanProperty(false);

    public Hearthstone(String name, String description) {
        this.name.set(name);
        this.description.set(description);
    }

    public Hearthstone(String id, String name, String description) {
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

    public javafx.beans.property.BooleanProperty equippedProperty() { return equipped; }
    public boolean isEquipped() { return equipped.get(); }
    public void setEquipped(boolean v) { equipped.set(v); }

    // --- Persistence Support (DTO) ---
    public static class HearthstoneData {
        public String id;
        public String name;
        public String description;
    }

    public HearthstoneData toData() {
        HearthstoneData data = new HearthstoneData();
        data.id = getId();
        data.name = getName();
        data.description = getDescription();
        return data;
    }

    public static Hearthstone fromData(HearthstoneData data) {
        if (data == null) return null;
        return new Hearthstone(data.id, data.name, data.description);
    }

    @Override
    public String toString() {
        return getName();
    }
}
