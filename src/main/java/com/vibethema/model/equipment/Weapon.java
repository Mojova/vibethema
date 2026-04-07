package com.vibethema.model.equipment;

import com.vibethema.model.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;
import com.vibethema.model.traits.*;
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

public class Weapon {
    public static final String UNARMED_ID = "63d76e46-5c6a-356c-82e7-ef437a371c6d";

    public enum WeaponRange {
        CLOSE,
        THROWN,
        ARCHERY;

        @Override
        public String toString() {
            String s = super.toString();
            return s.substring(0, 1) + s.substring(1).toLowerCase();
        }
    }

    public enum WeaponType {
        MORTAL,
        ARTIFACT;

        @Override
        public String toString() {
            String s = super.toString();
            return s.substring(0, 1) + s.substring(1).toLowerCase();
        }
    }

    public enum WeaponCategory {
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
    private final StringProperty instanceId =
            new SimpleStringProperty(UUID.randomUUID().toString());
    private final StringProperty name = new SimpleStringProperty("");
    private final ObjectProperty<WeaponRange> range = new SimpleObjectProperty<>(WeaponRange.CLOSE);
    private final ObjectProperty<WeaponType> type = new SimpleObjectProperty<>(WeaponType.MORTAL);
    private final ObjectProperty<WeaponCategory> category =
            new SimpleObjectProperty<>(WeaponCategory.MEDIUM);
    private final ObservableList<String> tags = FXCollections.observableArrayList();
    private final StringProperty specialtyId = new SimpleStringProperty("");
    private final BooleanProperty equipped = new SimpleBooleanProperty(false);

    // Stats
    private final IntegerProperty accuracy = new SimpleIntegerProperty(0);
    private final IntegerProperty damage = new SimpleIntegerProperty(0);
    private final IntegerProperty defense = new SimpleIntegerProperty(0);
    private final IntegerProperty overwhelming = new SimpleIntegerProperty(0);
    private final IntegerProperty attunement = new SimpleIntegerProperty(0);

    // Range Bonuses
    private final IntegerProperty closeRangeBonus = new SimpleIntegerProperty(0);
    private final IntegerProperty shortRangeBonus = new SimpleIntegerProperty(0);
    private final IntegerProperty mediumRangeBonus = new SimpleIntegerProperty(0);
    private final IntegerProperty longRangeBonus = new SimpleIntegerProperty(0);
    private final IntegerProperty extremeRangeBonus = new SimpleIntegerProperty(0);

    public Weapon(String name) {
        this.name.set(name);
        setupListeners();
        updateStats();
    }

    public Weapon(
            String id, String name, WeaponRange range, WeaponType type, WeaponCategory category) {
        this.id.set(id);
        this.instanceId.set(UUID.randomUUID().toString());
        this.name.set(name);
        this.range.set(range);
        this.type.set(type);
        this.category.set(category);
        setupListeners();
        updateStats();
    }

    public Weapon(
            String id,
            String instanceId,
            String name,
            WeaponRange range,
            WeaponType type,
            WeaponCategory category) {
        this.id.set(id);
        this.instanceId.set(instanceId);
        this.name.set(name);
        this.range.set(range);
        this.type.set(type);
        this.category.set(category);
        setupListeners();
        updateStats();
    }

    private void setupListeners() {
        range.addListener((obs, old, ov) -> updateStats());
        type.addListener((obs, old, ov) -> updateStats());
        category.addListener((obs, old, ov) -> updateStats());
    }

    private void updateStats() {
        WeaponRange r = range.get();
        WeaponType t = type.get();
        WeaponCategory c = category.get();

        if (t == WeaponType.MORTAL) {
            attunement.set(0);
            if (r == WeaponRange.CLOSE) {
                switch (c) {
                    case LIGHT:
                        accuracy.set(4);
                        damage.set(7);
                        defense.set(0);
                        overwhelming.set(1);
                        break;
                    case MEDIUM:
                        accuracy.set(2);
                        damage.set(9);
                        defense.set(1);
                        overwhelming.set(1);
                        break;
                    case HEAVY:
                        accuracy.set(0);
                        damage.set(11);
                        defense.set(-1);
                        overwhelming.set(1);
                        break;
                }
                clearRangeBonuses();
            } else if (r == WeaponRange.THROWN) {
                accuracy.set(0); // Accuracy for ranged is usually 0, range bonuses apply
                defense.set(0); // Ranged weapons usually have no defense bonus
                switch (c) {
                    case LIGHT:
                        damage.set(7);
                        overwhelming.set(1);
                        break;
                    case MEDIUM:
                        damage.set(9);
                        overwhelming.set(1);
                        break;
                    case HEAVY:
                        damage.set(11);
                        overwhelming.set(1);
                        break;
                }
                closeRangeBonus.set(4);
                shortRangeBonus.set(3);
                mediumRangeBonus.set(2);
                longRangeBonus.set(-1);
                extremeRangeBonus.set(-3);
            } else if (r == WeaponRange.ARCHERY) {
                accuracy.set(0);
                defense.set(0);
                switch (c) {
                    case LIGHT:
                        damage.set(7);
                        overwhelming.set(1);
                        break;
                    case MEDIUM:
                        damage.set(9);
                        overwhelming.set(1);
                        break;
                    case HEAVY:
                        damage.set(11);
                        overwhelming.set(1);
                        break;
                }
                closeRangeBonus.set(-2);
                shortRangeBonus.set(4);
                mediumRangeBonus.set(2);
                longRangeBonus.set(0);
                extremeRangeBonus.set(-2);
            }
        } else { // ARTIFACT
            attunement.set(5);
            if (r == WeaponRange.CLOSE) {
                switch (c) {
                    case LIGHT:
                        accuracy.set(5);
                        damage.set(10);
                        defense.set(0);
                        overwhelming.set(3);
                        break;
                    case MEDIUM:
                        accuracy.set(3);
                        damage.set(12);
                        defense.set(1);
                        overwhelming.set(4);
                        break;
                    case HEAVY:
                        accuracy.set(1);
                        damage.set(14);
                        defense.set(0);
                        overwhelming.set(5);
                        break;
                }
                clearRangeBonuses();
            } else if (r == WeaponRange.THROWN) {
                accuracy.set(0);
                defense.set(0);
                switch (c) {
                    case LIGHT:
                        damage.set(10);
                        overwhelming.set(3);
                        break;
                    case MEDIUM:
                        damage.set(12);
                        overwhelming.set(4);
                        break;
                    case HEAVY:
                        damage.set(14);
                        overwhelming.set(5);
                        break;
                }
                closeRangeBonus.set(5);
                shortRangeBonus.set(4);
                mediumRangeBonus.set(3);
                longRangeBonus.set(0);
                extremeRangeBonus.set(-2);
            } else if (r == WeaponRange.ARCHERY) {
                accuracy.set(0);
                defense.set(0);
                switch (c) {
                    case LIGHT:
                        damage.set(10);
                        overwhelming.set(3);
                        break;
                    case MEDIUM:
                        damage.set(12);
                        overwhelming.set(4);
                        break;
                    case HEAVY:
                        damage.set(14);
                        overwhelming.set(5);
                        break;
                }
                closeRangeBonus.set(-1);
                shortRangeBonus.set(5);
                mediumRangeBonus.set(3);
                longRangeBonus.set(1);
                extremeRangeBonus.set(-1);
            }
        }
    }

    private void clearRangeBonuses() {
        closeRangeBonus.set(0);
        shortRangeBonus.set(0);
        mediumRangeBonus.set(0);
        longRangeBonus.set(0);
        extremeRangeBonus.set(0);
    }

    // Accessors for new properties
    public IntegerProperty accuracyProperty() {
        return accuracy;
    }

    public int getAccuracy() {
        return accuracy.get();
    }

    public void setAccuracy(int v) {
        accuracy.set(v);
    }

    public IntegerProperty damageProperty() {
        return damage;
    }

    public int getDamage() {
        return damage.get();
    }

    public void setDamage(int v) {
        damage.set(v);
    }

    public IntegerProperty defenseProperty() {
        return defense;
    }

    public int getDefense() {
        return defense.get();
    }

    public void setDefense(int v) {
        defense.set(v);
    }

    public IntegerProperty overwhelmingProperty() {
        return overwhelming;
    }

    public int getOverwhelming() {
        return overwhelming.get();
    }

    public void setOverwhelming(int v) {
        overwhelming.set(v);
    }

    public IntegerProperty attunementProperty() {
        return attunement;
    }

    public int getAttunement() {
        return attunement.get();
    }

    public void setAttunement(int v) {
        attunement.set(v);
    }

    public IntegerProperty closeRangeBonusProperty() {
        return closeRangeBonus;
    }

    public int getCloseRangeBonus() {
        return closeRangeBonus.get();
    }

    public IntegerProperty shortRangeBonusProperty() {
        return shortRangeBonus;
    }

    public int getShortRangeBonus() {
        return shortRangeBonus.get();
    }

    public IntegerProperty mediumRangeBonusProperty() {
        return mediumRangeBonus;
    }

    public int getMediumRangeBonus() {
        return mediumRangeBonus.get();
    }

    public IntegerProperty longRangeBonusProperty() {
        return longRangeBonus;
    }

    public int getLongRangeBonus() {
        return longRangeBonus.get();
    }

    public IntegerProperty extremeRangeBonusProperty() {
        return extremeRangeBonus;
    }

    public int getExtremeRangeBonus() {
        return extremeRangeBonus.get();
    }

    // Existing accessors
    public StringProperty idProperty() {
        return id;
    }

    public String getId() {
        return id.get();
    }

    public void setId(String id) {
        this.id.set(id);
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

    public void setName(String name) {
        this.name.set(name);
    }

    public ObjectProperty<WeaponRange> rangeProperty() {
        return range;
    }

    public WeaponRange getRange() {
        return range.get();
    }

    public void setRange(WeaponRange range) {
        this.range.set(range);
    }

    public ObjectProperty<WeaponType> typeProperty() {
        return type;
    }

    public WeaponType getType() {
        return type.get();
    }

    public void setType(WeaponType type) {
        this.type.set(type);
    }

    public ObjectProperty<WeaponCategory> categoryProperty() {
        return category;
    }

    public WeaponCategory getCategory() {
        return category.get();
    }

    public void setCategory(WeaponCategory category) {
        this.category.set(category);
    }

    public ObservableList<String> getTags() {
        return tags;
    }

    public StringProperty specialtyIdProperty() {
        return specialtyId;
    }

    public String getSpecialtyId() {
        return specialtyId.get();
    }

    public void setSpecialtyId(String id) {
        this.specialtyId.set(id);
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

    // --- Persistance Support (DTO) ---
    public static class WeaponData {
        public String id;
        public String instanceId;
        public String name;
        public WeaponRange range;
        public WeaponType type;
        public WeaponCategory category;
        public java.util.List<String> tags;
    }

    public WeaponData toData() {
        WeaponData data = new WeaponData();
        data.id = getId();
        data.instanceId = getInstanceId();
        data.name = getName();
        data.range = getRange();
        data.type = getType();
        data.category = getCategory();
        data.tags = new java.util.ArrayList<>(getTags());
        return data;
    }

    public static Weapon fromData(WeaponData data) {
        if (data == null) return null;
        Weapon w =
                new Weapon(
                        data.id, data.instanceId, data.name, data.range, data.type, data.category);
        w.getTags().setAll(data.tags != null ? data.tags : java.util.Collections.emptyList());
        return w;
    }

    public Weapon copy() {
        Weapon w = fromData(toData());
        w.setInstanceId(UUID.randomUUID().toString());
        return w;
    }

    @Override
    public String toString() {
        return getName();
    }
}
