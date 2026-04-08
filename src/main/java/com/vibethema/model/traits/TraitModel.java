package com.vibethema.model.traits;

import com.vibethema.model.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

public class TraitModel {
    private final ObservableMap<Attribute, IntegerProperty> attributes =
            FXCollections.observableHashMap();
    private final ObservableMap<Ability, IntegerProperty> abilities =
            FXCollections.observableHashMap();
    private final ObservableMap<Ability, BooleanProperty> casteAbilities =
            FXCollections.observableHashMap();
    private final ObservableMap<Ability, BooleanProperty> favoredAbilities =
            FXCollections.observableHashMap();
    private final ObservableMap<Attribute.Category, ObjectProperty<AttributePriority>>
            attributePriorities = FXCollections.observableHashMap();

    private final ObservableList<Specialty> specialties =
            FXCollections.observableArrayList(
                    s -> new javafx.beans.Observable[] {s.nameProperty(), s.abilityProperty()});
    private final ObservableList<Merit> merits = FXCollections.observableArrayList();
    private final ObservableList<CraftAbility> crafts = FXCollections.observableArrayList();
    private final ObservableList<MartialArtsStyle> martialArtsStyles =
            FXCollections.observableArrayList();
    private final ObservableList<String> validSupernalAbilities =
            FXCollections.observableArrayList();

    private final IntegerProperty casteAbilityCount = new SimpleIntegerProperty(0);
    private final IntegerProperty favoredAbilityCount = new SimpleIntegerProperty(0);

    private final Consumer<Void> dirtyListener;
    private final Runnable statUpdateTrigger;
    private boolean isImporting = false;

    public TraitModel(Consumer<Void> dirtyListener, Runnable statUpdateTrigger) {
        this.dirtyListener = dirtyListener;
        this.statUpdateTrigger = statUpdateTrigger;

        for (Attribute attr : SystemData.ATTRIBUTES) {
            attributes.put(attr, new SimpleIntegerProperty(1));
            attributes
                    .get(attr)
                    .addListener(
                            (obs, oldV, newV) -> {
                                markDirty();
                                statUpdateTrigger.run();
                            });
        }

        for (Attribute.Category cat : Attribute.Category.values()) {
            ObjectProperty<AttributePriority> priority = new SimpleObjectProperty<>(null);
            priority.addListener(
                    (obs, oldV, newV) -> {
                        if (newV != null && !isImporting) {
                            for (Map.Entry<Attribute.Category, ObjectProperty<AttributePriority>>
                                    entry : attributePriorities.entrySet()) {
                                if (entry.getKey() != cat && entry.getValue().get() == newV) {
                                    entry.getValue().set(null);
                                }
                            }
                        }
                        markDirty();
                    });
            attributePriorities.put(cat, priority);
        }

        for (Ability ability : SystemData.ABILITIES) {
            abilities.put(ability, new SimpleIntegerProperty(0));
            abilities
                    .get(ability)
                    .addListener(
                            (obs, oldV, newV) -> {
                                markDirty();
                                statUpdateTrigger.run();
                            });

            casteAbilities.put(ability, new SimpleBooleanProperty(false));
            casteAbilities
                    .get(ability)
                    .addListener(
                            (obs, oldV, newV) -> {
                                if (newV && !isImporting) {
                                    favoredAbilities.get(ability).set(false);
                                }
                                if (!isImporting
                                        && (ability == Ability.BRAWL
                                                || ability == Ability.MARTIAL_ARTS)) {
                                    isImporting = true;
                                    try {
                                        Ability other =
                                                (ability == Ability.BRAWL)
                                                        ? Ability.MARTIAL_ARTS
                                                        : Ability.BRAWL;
                                        casteAbilities.get(other).set(newV);
                                        favoredAbilities.get(other).set(false);
                                    } finally {
                                        isImporting = false;
                                    }
                                }
                                updateCasteFavoredCounts();
                                updateValidSupernalAbilities();
                                markDirty();
                            });

            favoredAbilities.put(ability, new SimpleBooleanProperty(false));
            favoredAbilities
                    .get(ability)
                    .addListener(
                            (obs, oldV, newV) -> {
                                if (newV && !isImporting) {
                                    casteAbilities.get(ability).set(false);
                                }
                                if (!isImporting
                                        && (ability == Ability.BRAWL
                                                || ability == Ability.MARTIAL_ARTS)) {
                                    isImporting = true;
                                    try {
                                        Ability other =
                                                (ability == Ability.BRAWL)
                                                        ? Ability.MARTIAL_ARTS
                                                        : Ability.BRAWL;
                                        favoredAbilities.get(other).set(newV);
                                        casteAbilities.get(other).set(false);
                                    } finally {
                                        isImporting = false;
                                    }
                                }
                                updateCasteFavoredCounts();
                                markDirty();
                            });
        }

        specialties.addListener(
                (javafx.collections.ListChangeListener<? super Specialty>)
                        c -> {
                            markDirty();
                            statUpdateTrigger.run();
                        });

        martialArtsStyles.addListener(
                (javafx.collections.ListChangeListener<? super MartialArtsStyle>)
                        c -> {
                            markDirty();
                            while (c.next()) {
                                if (c.wasAdded()) {
                                    for (MartialArtsStyle mas : c.getAddedSubList()) {
                                        mas.ratingProperty()
                                                .addListener((obs, ov, nv) -> markDirty());
                                        mas.styleNameProperty()
                                                .addListener((obs, ov, nv) -> markDirty());
                                    }
                                }
                            }
                        });

        crafts.addListener(
                (javafx.collections.ListChangeListener<? super CraftAbility>) c -> markDirty());

        merits.addListener(
                (javafx.collections.ListChangeListener<? super Merit>)
                        c -> {
                            markDirty();
                            statUpdateTrigger.run();
                        });

        // Initial update
        updateValidSupernalAbilities();
        updateCasteFavoredCounts();
    }

    public void setImporting(boolean importing) {
        this.isImporting = importing;
    }

    private void markDirty() {
        if (!isImporting) {
            dirtyListener.accept(null);
        }
    }

    public void updateCasteFavoredCounts() {
        Set<Ability> castes = new HashSet<>();
        Set<Ability> favoreds = new HashSet<>();

        for (Ability abil : SystemData.ABILITIES) {
            if (casteAbilities.get(abil).get()) castes.add(abil);
            if (favoredAbilities.get(abil).get()) favoreds.add(abil);
        }

        int c = 0;
        int f = 0;

        boolean pooledCaste = castes.remove(Ability.BRAWL) | castes.remove(Ability.MARTIAL_ARTS);
        boolean pooledFavored =
                favoreds.remove(Ability.BRAWL) | favoreds.remove(Ability.MARTIAL_ARTS);

        c = castes.size() + (pooledCaste ? 1 : 0);
        f = favoreds.size() + (pooledFavored ? 1 : 0);

        casteAbilityCount.set(c);
        favoredAbilityCount.set(f);
    }

    public void updateValidSupernalAbilities() {
        java.util.List<String> valid = new java.util.ArrayList<>();
        valid.add("");
        for (Ability ability : SystemData.ABILITIES) {
            if (casteAbilities.get(ability).get()) {
                valid.add(ability.getDisplayName());
            }
        }
        validSupernalAbilities.setAll(valid);
    }

    public IntegerProperty getAttribute(Attribute name) {
        return attributes.get(name);
    }

    public ObservableMap<Attribute, IntegerProperty> getAttributes() {
        return attributes;
    }

    public IntegerProperty getAbility(Ability name) {
        return abilities.get(name);
    }

    public ObservableMap<Ability, IntegerProperty> getAbilities() {
        return abilities;
    }

    public BooleanProperty getCasteAbility(Ability name) {
        return casteAbilities.get(name);
    }

    public BooleanProperty getFavoredAbility(Ability name) {
        return favoredAbilities.get(name);
    }

    public ObservableMap<Ability, BooleanProperty> getCasteAbilities() {
        return casteAbilities;
    }

    public ObservableMap<Ability, BooleanProperty> getFavoredAbilities() {
        return favoredAbilities;
    }

    public ObjectProperty<AttributePriority> getAttributePriority(Attribute.Category category) {
        return attributePriorities.get(category);
    }

    public ObservableMap<Attribute.Category, ObjectProperty<AttributePriority>>
            getAttributePriorities() {
        return attributePriorities;
    }

    public ObservableList<Specialty> getSpecialties() {
        return specialties;
    }

    public ObservableList<Merit> getMerits() {
        return merits;
    }

    public ObservableList<CraftAbility> getCrafts() {
        return crafts;
    }

    public ObservableList<MartialArtsStyle> getMartialArtsStyles() {
        return martialArtsStyles;
    }

    public ObservableList<String> getValidSupernalAbilities() {
        return validSupernalAbilities;
    }

    public IntegerProperty casteAbilityCountProperty() {
        return casteAbilityCount;
    }

    public IntegerProperty favoredAbilityCountProperty() {
        return favoredAbilityCount;
    }

    public int getAbilityRating(Ability name) {
        if (name == null) return 0;
        return abilities.get(name).get();
    }

    public int getAbilityRatingByName(String name) {
        if (name == null) return 0;
        Ability common = Ability.fromString(name);
        if (common != null) return abilities.get(common).get();

        for (MartialArtsStyle mas : martialArtsStyles) {
            if (mas.getStyleName().equals(name)) return mas.getRating();
        }
        for (CraftAbility ca : crafts) {
            if (ca.getExpertise().equals(name)) return ca.getRating();
        }
        return 0;
    }

    public IntegerProperty getAbilityPropertyByName(String name) {
        if (name == null) return null;
        Ability common = Ability.fromString(name);
        if (common != null) return abilities.get(common);

        for (MartialArtsStyle mas : martialArtsStyles) {
            if (mas.getStyleName().equals(name)) return mas.ratingProperty();
        }
        for (CraftAbility ca : crafts) {
            if (ca.getExpertise().equals(name)) return ca.ratingProperty();
        }
        return null;
    }

    public boolean isMartialArtsStyle(String name) {
        if (name == null) return false;
        return martialArtsStyles.stream().anyMatch(s -> s.getStyleName().equals(name));
    }

    public boolean isCraftExpertise(String name) {
        if (name == null) return false;
        return crafts.stream().anyMatch(c -> c.getExpertise().equals(name));
    }
}
