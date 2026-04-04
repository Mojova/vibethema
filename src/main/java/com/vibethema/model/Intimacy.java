package com.vibethema.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Intimacy {
    public enum Type { PRINCIPLE, TIE }
    public enum Intensity { MINOR, MAJOR, DEFINING }

    private final String id;
    private final StringProperty name = new SimpleStringProperty("");
    private final ObjectProperty<Type> type = new SimpleObjectProperty<>(Type.TIE);
    private final ObjectProperty<Intensity> intensity = new SimpleObjectProperty<>(Intensity.MINOR);
    private final StringProperty description = new SimpleStringProperty("");

    public Intimacy(String id, String name, Type type, Intensity intensity) {
        this.id = id;
        this.name.set(name);
        this.type.set(type);
        this.intensity.set(intensity);
    }

    public String getId() { return id; }
    public StringProperty nameProperty() { return name; }
    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }

    public ObjectProperty<Type> typeProperty() { return type; }
    public Type getType() { return type.get(); }
    public void setType(Type type) { this.type.set(type); }

    public ObjectProperty<Intensity> intensityProperty() { return intensity; }
    public Intensity getIntensity() { return intensity.get(); }
    public void setIntensity(Intensity intensity) { this.intensity.set(intensity); }

    public StringProperty descriptionProperty() { return description; }
    public String getDescription() { return description.get(); }
    public void setDescription(String description) { this.description.set(description); }
}
