package com.vibethema.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.UUID;

public class Weapon {
    public enum WeaponRange {
        CLOSE, THROWN, ARCHERY;

        @Override
        public String toString() {
            String s = super.toString();
            return s.substring(0, 1) + s.substring(1).toLowerCase();
        }
    }

    public enum WeaponType {
        MORTAL, ARTIFACT;

        @Override
        public String toString() {
            String s = super.toString();
            return s.substring(0, 1) + s.substring(1).toLowerCase();
        }
    }

    public enum WeaponCategory {
        LIGHT, MEDIUM, HEAVY;

        @Override
        public String toString() {
            String s = super.toString();
            return s.substring(0, 1) + s.substring(1).toLowerCase();
        }
    }

    private final StringProperty id = new SimpleStringProperty(UUID.randomUUID().toString());
    private final StringProperty name = new SimpleStringProperty("");
    private final ObjectProperty<WeaponRange> range = new SimpleObjectProperty<>(WeaponRange.CLOSE);
    private final ObjectProperty<WeaponType> type = new SimpleObjectProperty<>(WeaponType.MORTAL);
    private final ObjectProperty<WeaponCategory> category = new SimpleObjectProperty<>(WeaponCategory.MEDIUM);
    private final ObservableList<String> tags = FXCollections.observableArrayList();

    public Weapon(String name) {
        this.name.set(name);
    }

    public Weapon(String id, String name, WeaponRange range, WeaponType type, WeaponCategory category) {
        this.id.set(id);
        this.name.set(name);
        this.range.set(range);
        this.type.set(type);
        this.category.set(category);
    }

    public StringProperty idProperty() { return id; }
    public String getId() { return id.get(); }
    public void setId(String id) { this.id.set(id); }

    public StringProperty nameProperty() { return name; }
    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }

    public ObjectProperty<WeaponRange> rangeProperty() { return range; }
    public WeaponRange getRange() { return range.get(); }
    public void setRange(WeaponRange range) { this.range.set(range); }

    public ObjectProperty<WeaponType> typeProperty() { return type; }
    public WeaponType getType() { return type.get(); }
    public void setType(WeaponType type) { this.type.set(type); }

    public ObjectProperty<WeaponCategory> categoryProperty() { return category; }
    public WeaponCategory getCategory() { return category.get(); }
    public void setCategory(WeaponCategory category) { this.category.set(category); }

    public ObservableList<String> getTags() { return tags; }
}
