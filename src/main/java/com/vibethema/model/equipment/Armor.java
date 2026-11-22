package com.vibethema.model.equipment;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
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

public class Armor {
    public enum Stat {
        SOAK,
        MOBILITY_PENALTY,
        HARDNESS,
        ATTUNEMENT
    }

    private String description = "";
    private final Map<Stat, Integer> statOverrides = new EnumMap<>(Stat.class);

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    public Map<Stat, Integer> getStatOverrides() {
        return Map.copyOf(statOverrides);
    }

    public void setStatOverrides(Map<Stat, Integer> overrides) {
        statOverrides.clear();
        if (overrides != null) statOverrides.putAll(overrides);
        updateStats();
    }

    public enum ArmorType {
        MORTAL,
        ARTIFACT;

        @Override
        public String toString() {
            String s = super.toString();
            return s.substring(0, 1) + s.substring(1).toLowerCase();
        }
    }

    public enum ArmorWeight {
        LIGHT,
        MEDIUM,
        HEAVY;

        @Override
        public String toString() {
            String s = super.toString();
            return s.substring(0, 1) + s.substring(1).toLowerCase();
        }
    }

    private final StringProperty id = new SimpleStringProperty(UUID.randomUUID().toString());
    private String sourceId;
    private final StringProperty instanceId =
            new SimpleStringProperty(UUID.randomUUID().toString());
    private final StringProperty name = new SimpleStringProperty("");
    private final ObjectProperty<ArmorType> type = new SimpleObjectProperty<>(ArmorType.MORTAL);
    private final ObjectProperty<ArmorWeight> weight =
            new SimpleObjectProperty<>(ArmorWeight.MEDIUM);
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
                case LIGHT:
                    soak.set(3);
                    mobilityPenalty.set(0);
                    break;
                case MEDIUM:
                    soak.set(5);
                    mobilityPenalty.set(-1);
                    break;
                case HEAVY:
                    soak.set(7);
                    mobilityPenalty.set(-2);
                    break;
            }
        } else { // ARTIFACT
            switch (w) {
                case LIGHT:
                    soak.set(5);
                    hardness.set(4);
                    mobilityPenalty.set(0);
                    attunement.set(4);
                    break;
                case MEDIUM:
                    soak.set(8);
                    hardness.set(7);
                    mobilityPenalty.set(-1);
                    attunement.set(5);
                    break;
                case HEAVY:
                    soak.set(11);
                    hardness.set(10);
                    mobilityPenalty.set(-2);
                    attunement.set(6);
                    break;
            }
        }
        statOverrides.forEach((stat, value) -> {
            switch (stat) {
                case SOAK -> soak.set(value);
                case MOBILITY_PENALTY -> mobilityPenalty.set(value);
                case HARDNESS -> hardness.set(value);
                case ATTUNEMENT -> attunement.set(value);
            }
        });
    }

    // Accessors
    public StringProperty idProperty() {
        return id;
    }

    public String getId() {
        return id.get();
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
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

    public ObjectProperty<ArmorType> typeProperty() {
        return type;
    }

    public ArmorType getType() {
        return type.get();
    }

    public void setType(ArmorType v) {
        type.set(v);
    }

    public ObjectProperty<ArmorWeight> weightProperty() {
        return weight;
    }

    public ArmorWeight getWeight() {
        return weight.get();
    }

    public void setWeight(ArmorWeight v) {
        weight.set(v);
    }

    public ObservableList<String> getTags() {
        return tags;
    }

    public BooleanProperty equippedProperty() {
        return equipped;
    }

    public boolean isEquipped() {
        return equipped.get();
    }

    public void setEquipped(boolean v) {
        equipped.set(v);
    }

    public IntegerProperty soakProperty() {
        return soak;
    }

    public int getSoak() {
        return soak.get();
    }

    public IntegerProperty mobilityPenaltyProperty() {
        return mobilityPenalty;
    }

    public int getMobilityPenalty() {
        return mobilityPenalty.get();
    }

    public IntegerProperty hardnessProperty() {
        return hardness;
    }

    public int getHardness() {
        return hardness.get();
    }

    public IntegerProperty attunementProperty() {
        return attunement;
    }

    public int getAttunement() {
        return attunement.get();
    }

    // --- Persistence Support (DTO) ---
    public static class ArmorData {
        public String id;
        public String sourceId;
        public String instanceId;
        public String name;
        public String description;
        public Map<Stat, Integer> statOverrides;
        public ArmorType type;
        public ArmorWeight weight;
        public java.util.List<String> tags;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ArmorData that)) return false;
            return java.util.Objects.equals(id, that.id)
                    && java.util.Objects.equals(sourceId, that.sourceId)
                    && java.util.Objects.equals(instanceId, that.instanceId)
                    && java.util.Objects.equals(name, that.name)
                    && java.util.Objects.equals(description, that.description)
                    && java.util.Objects.equals(statOverrides, that.statOverrides)
                    && java.util.Objects.equals(type, that.type)
                    && java.util.Objects.equals(weight, that.weight)
                    && java.util.Objects.equals(tags, that.tags);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(
                    id, sourceId, instanceId, name, description, statOverrides, type, weight, tags);
        }
    }

    public ArmorData toData() {
        ArmorData data = new ArmorData();
        data.id = getId();
        data.sourceId = getSourceId();
        data.instanceId = getInstanceId();
        data.name = getName();
        data.description = description.isEmpty() ? null : description;
        data.statOverrides = statOverrides.isEmpty() ? null : getStatOverrides();
        data.type = getType();
        data.weight = getWeight();
        data.tags = new java.util.ArrayList<>(getTags());
        return data;
    }

    public static Armor fromData(ArmorData data) {
        if (data == null) return null;
        Armor a = new Armor(data.id, data.instanceId, data.name, data.type, data.weight);
        a.setSourceId(data.sourceId);
        a.setDescription(data.description);
        a.setStatOverrides(data.statOverrides);
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
