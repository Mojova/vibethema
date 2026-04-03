package com.vibethema.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Specialty {
    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty ability = new SimpleStringProperty("");

    public Specialty(String name, String ability) {
        this.name.set(name);
        this.ability.set(ability);
    }

    public StringProperty nameProperty() { return name; }
    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }

    public StringProperty abilityProperty() { return ability; }
    public String getAbility() { return ability.get(); }
    public void setAbility(String ability) { this.ability.set(ability); }
}
