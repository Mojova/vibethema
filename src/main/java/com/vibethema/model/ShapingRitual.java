package com.vibethema.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.util.UUID;

public class ShapingRitual {
    private final StringProperty id = new SimpleStringProperty(UUID.randomUUID().toString());
    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");

    public ShapingRitual(String name, String description) {
        this.name.set(name);
        this.description.set(description);
    }

    public ShapingRitual(String id, String name, String description) {
        this.id.set(id);
        this.name.set(name);
        this.description.set(description);
    }

    public StringProperty idProperty() { return id; }
    public String getId() { return id.get(); }

    public StringProperty nameProperty() { return name; }
    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }

    public StringProperty descriptionProperty() { return description; }
    public String getDescription() { return description.get(); }
    public void setDescription(String description) { this.description.set(description); }
}
