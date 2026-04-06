package com.vibethema.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.UUID;

public class Armor {
    public enum ArmorType {
        MORTAL, ARTIFACT;

        @Override
        public String toString() {
            String s = super.toString();
            return s.substring(0, 1) + s.substring(1).toLowerCase();
        }
    }

    public enum ArmorWeight {
        LIGHT, MEDIUM, HEAVY;

        @Override
        public String toString() {
            String s = super.toString();
            return s.substring(0, 1) + s.substring(1).toLowerCase();
        }
    }

    private final StringProperty id = new SimpleStringProperty(UUID.randomUUID().toString());
    private final StringProperty instanceId = new SimpleStringProperty(UUID.randomUUID().toString());
    private final StringProperty name = new SimpleStringProperty("");
    private final ObjectProperty<ArmorType> type = new SimpleObjectProperty<>(ArmorType.MORTAL);
    private final ObjectProperty<ArmorWeight> weight = new SimpleObjectProperty<>(ArmorWeight.MEDIUM);
    private final ObservableList<String> tags = FXCollections.observableArrayList();
    private final BooleanProperty equipped = new SimpleBooleanProperty(false);

    // Stats
    private final IntegerProperty soak = new SimpleIntegerProperty(0);
    private final IntegerProperty mobilityPenalty = new SimpleIntegerProperty(0);
    private final IntegerProperty hardness = new SimpleIntegerProperty(0);
    private final IntegerProperty attunement = new SimpleIntegerProperty(0);

    public Armor(String name) {
        this.name.set(name);
        setupListeners();
        updateStats();
    }

    public Armor(String id, String name, ArmorType type, ArmorWeight weight) {
        this.id.set(id);
        this.instanceId.set(UUID.randomUUID().toString());
        this.name.set(name);
        this.type.set(type);
        this.weight.set(weight);
        setupListeners();
        updateStats();
    }

    public Armor(String id, String instanceId, String name, ArmorType type, ArmorWeight weight) {
        this.id.set(id);
        this.instanceId.set(instanceId);
        this.name.set(name);
        this.type.set(type);
        this.weight.set(weight);
        setupListeners();
        updateStats();
    }

    private void setupListeners() {
        type.addListener((obs, old, ov) -> updateStats());
        weight.addListener((obs, old, ov) -> updateStats());
    }

    private void updateStats() {
        ArmorType t = type.get();
        ArmorWeight w = weight.get();

        if (t == ArmorType.MORTAL) {
            attunement.set(0);
            hardness.set(0);
            switch (w) {
                case LIGHT: soak.set(3); mobilityPenalty.set(0); break;
                case MEDIUM: soak.set(5); mobilityPenalty.set(-1); break;
                case HEAVY: soak.set(7); mobilityPenalty.set(-2); break;
            }
        } else { // ARTIFACT
            switch (w) {
                case LIGHT: soak.set(5); hardness.set(4); mobilityPenalty.set(0); attunement.set(4); break;
                case MEDIUM: soak.set(8); hardness.set(7); mobilityPenalty.set(-1); attunement.set(5); break;
                case HEAVY: soak.set(11); hardness.set(10); mobilityPenalty.set(-2); attunement.set(6); break;
            }
        }
    }

    // Accessors
    public StringProperty idProperty() { return id; }
    public String getId() { return id.get(); }

    public StringProperty instanceIdProperty() { return instanceId; }
    public String getInstanceId() { return instanceId.get(); }
    public void setInstanceId(String instanceId) { this.instanceId.set(instanceId); }
    
    public StringProperty nameProperty() { return name; }
    public String getName() { return name.get(); }
    public void setName(String v) { name.set(v); }

    public ObjectProperty<ArmorType> typeProperty() { return type; }
    public ArmorType getType() { return type.get(); }
    public void setType(ArmorType v) { type.set(v); }

    public ObjectProperty<ArmorWeight> weightProperty() { return weight; }
    public ArmorWeight getWeight() { return weight.get(); }
    public void setWeight(ArmorWeight v) { weight.set(v); }

    public ObservableList<String> getTags() { return tags; }

    public BooleanProperty equippedProperty() { return equipped; }
    public boolean isEquipped() { return equipped.get(); }
    public void setEquipped(boolean v) { equipped.set(v); }

    public IntegerProperty soakProperty() { return soak; }
    public int getSoak() { return soak.get(); }

    public IntegerProperty mobilityPenaltyProperty() { return mobilityPenalty; }
    public int getMobilityPenalty() { return mobilityPenalty.get(); }

    public IntegerProperty hardnessProperty() { return hardness; }
    public int getHardness() { return hardness.get(); }

    public IntegerProperty attunementProperty() { return attunement; }
    public int getAttunement() { return attunement.get(); }

    // --- Persistence Support (DTO) ---
    public static class ArmorData {
        public String id;
        public String instanceId;
        public String name;
        public ArmorType type;
        public ArmorWeight weight;
        public java.util.List<String> tags;
    }

    public ArmorData toData() {
        ArmorData data = new ArmorData();
        data.id = getId();
        data.instanceId = getInstanceId();
        data.name = getName();
        data.type = getType();
        data.weight = getWeight();
        data.tags = new java.util.ArrayList<>(getTags());
        return data;
    }

    public static Armor fromData(ArmorData data) {
        if (data == null) return null;
        Armor a = new Armor(data.id, data.instanceId, data.name, data.type, data.weight);
        a.getTags().setAll(data.tags != null ? data.tags : java.util.Collections.emptyList());
        return a;
    }

    public Armor copy() {
        Armor a = fromData(toData());
        a.setInstanceId(UUID.randomUUID().toString());
        return a;
    }

    @Override
    public String toString() {
        return getName();
    }
}
