package com.vibethema.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.UUID;
import java.util.List;

public class Spell {
    public enum Circle {
        TERRESTRIAL, CELESTIAL, SOLAR;

        @Override
        public String toString() {
            String s = super.toString();
            return s.substring(0, 1) + s.substring(1).toLowerCase();
        }
    }

    private final StringProperty id = new SimpleStringProperty(UUID.randomUUID().toString());
    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty circle = new SimpleStringProperty(Circle.TERRESTRIAL.name());
    private final StringProperty cost = new SimpleStringProperty("");
    private final ObservableList<String> keywords = FXCollections.observableArrayList();
    private final StringProperty duration = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");

    public Spell() {}

    public Spell(String name, Circle circle) {
        this.name.set(name);
        this.circle.set(circle.name());
    }

    public Spell(String id, String name, String circle, String cost, List<String> keywords, String duration, String description) {
        this.id.set(id);
        this.name.set(name);
        this.circle.set(circle);
        this.cost.set(cost);
        if (keywords != null) this.keywords.setAll(keywords);
        this.duration.set(duration);
        this.description.set(description);
    }

    public StringProperty idProperty() { return id; }
    public String getId() { return id.get(); }

    public StringProperty nameProperty() { return name; }
    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }

    public StringProperty circleProperty() { return circle; }
    public String getCircle() { return circle.get(); }
    public void setCircle(String circle) { this.circle.set(circle); }

    public StringProperty costProperty() { return cost; }
    public String getCost() { return cost.get(); }
    public void setCost(String cost) { this.cost.set(cost); }

    public ObservableList<String> getKeywords() { return keywords; }

    public StringProperty durationProperty() { return duration; }
    public String getDuration() { return duration.get(); }
    public void setDuration(String duration) { this.duration.set(duration); }

    public StringProperty descriptionProperty() { return description; }
    public String getDescription() { return description.get(); }
    public void setDescription(String description) { this.description.set(description); }
}
