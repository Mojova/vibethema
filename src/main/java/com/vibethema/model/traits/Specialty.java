package com.vibethema.model.traits;

import java.util.UUID;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Specialty {
    private final StringProperty id = new SimpleStringProperty(UUID.randomUUID().toString());
    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty ability = new SimpleStringProperty("");

    public Specialty(String name, String ability) {
        this(UUID.randomUUID().toString(), name, ability);
    }

    public Specialty(String id, String name, String ability) {
        this.id.set(id);
        this.name.set(name);
        this.ability.set(ability);
    }

    public StringProperty idProperty() {
        return id;
    }

    public String getId() {
        return id.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty abilityProperty() {
        return ability;
    }

    public String getAbility() {
        return ability.get();
    }

    public void setAbility(String ability) {
        this.ability.set(ability);
    }
}
