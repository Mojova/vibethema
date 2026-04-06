package com.vibethema.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import javafx.collections.ObservableMap;

public class CharacterData {
    // Enums moved to standalone files

    private StringProperty name = new SimpleStringProperty("");
    private ObjectProperty<Caste> caste = new SimpleObjectProperty<>(Caste.NONE);
    private ObjectProperty<CharacterMode> mode = new SimpleObjectProperty<>(CharacterMode.CREATION);
    private StringProperty supernalAbility = new SimpleStringProperty("");

    private ObservableMap<Attribute, IntegerProperty> attributes = FXCollections.observableHashMap();
    private ObservableMap<Ability, IntegerProperty> abilities = FXCollections.observableHashMap();
    private ObservableMap<Ability, BooleanProperty> casteAbilities = FXCollections.observableHashMap();
    private ObservableMap<Ability, BooleanProperty> favoredAbilities = FXCollections.observableHashMap();
    private ObservableMap<Attribute.Category, ObjectProperty<AttributePriority>> attributePriorities = FXCollections
            .observableHashMap();

    private IntegerProperty essence = new SimpleIntegerProperty(1);
    private IntegerProperty willpower = new SimpleIntegerProperty(5);
    private StringProperty limitTrigger = new SimpleStringProperty("");
    private IntegerProperty limit = new SimpleIntegerProperty(0);

    private final ObservableList<PurchasedCharm> unlockedCharms = FXCollections.observableArrayList();
    private final ObservableList<Merit> merits = FXCollections.observableArrayList();
    private final ObservableList<Specialty> specialties = FXCollections
            .observableArrayList(s -> new javafx.beans.Observable[] { s.nameProperty(), s.abilityProperty() });
    private final ObservableList<CraftAbility> crafts = FXCollections.observableArrayList();
    private final ObservableList<MartialArtsStyle> martialArtsStyles = FXCollections.observableArrayList();
    private final ObservableList<Weapon> weapons = FXCollections.observableArrayList();
    private final ObservableList<Armor> armors = FXCollections.observableArrayList();
    private final ObservableList<Hearthstone> hearthstones = FXCollections.observableArrayList();
    private final ObservableList<OtherEquipment> otherEquipment = FXCollections.observableArrayList();
    private final ObservableList<Intimacy> intimacies = FXCollections.observableArrayList(
            i -> new javafx.beans.Observable[] { i.nameProperty(), i.intensityProperty(), i.descriptionProperty() });
    private final ObservableList<ShapingRitual> shapingRituals = FXCollections
            .observableArrayList(r -> new javafx.beans.Observable[] { r.nameProperty(), r.descriptionProperty() });
    private final ObservableList<Spell> spells = FXCollections.observableArrayList(
            s -> new javafx.beans.Observable[] { s.nameProperty(), s.descriptionProperty(), s.circleProperty() });

    private final ObservableList<XpAward> xpAwards = FXCollections.observableArrayList(
            x -> new javafx.beans.Observable[] { x.descriptionProperty(), x.amountProperty(), x.isSolarProperty() });

    private CharacterSaveState creationSnapshot = null;

    private final IntegerProperty naturalSoak = new SimpleIntegerProperty(0);
    private final IntegerProperty armorSoak = new SimpleIntegerProperty(0);
    private final IntegerProperty totalSoak = new SimpleIntegerProperty(0);
    private final IntegerProperty totalHardness = new SimpleIntegerProperty(0);

    private final IntegerProperty evasion = new SimpleIntegerProperty(0);
    private final IntegerProperty evasionBonus = new SimpleIntegerProperty(0);
    private final BooleanProperty hasEvasionSpecialty = new SimpleBooleanProperty(false);
    private final IntegerProperty resolve = new SimpleIntegerProperty(0);
    private final IntegerProperty resolveBonus = new SimpleIntegerProperty(0);
    private final BooleanProperty hasResolveSpecialty = new SimpleBooleanProperty(false);
    private final IntegerProperty guile = new SimpleIntegerProperty(0);
    private final IntegerProperty guileBonus = new SimpleIntegerProperty(0);
    private final BooleanProperty hasGuileSpecialty = new SimpleBooleanProperty(false);

    private final IntegerProperty joinBattle = new SimpleIntegerProperty(0);
    private final BooleanProperty hasJoinBattleSpecialty = new SimpleBooleanProperty(false);

    private final ObservableList<String> validSupernalAbilities = FXCollections.observableArrayList();

    private final IntegerProperty casteAbilityCount = new SimpleIntegerProperty(0);
    private final IntegerProperty favoredAbilityCount = new SimpleIntegerProperty(0);
    private final BooleanProperty overBonusPointLimit = new SimpleBooleanProperty(false);

    private final ObservableList<AttackPoolData> attackPools = FXCollections.observableArrayList();

    private final BooleanProperty terrestrialSorceryAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty celestialSorceryAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty solarSorceryAvailable = new SimpleBooleanProperty(false);

    private BooleanProperty dirty = new SimpleBooleanProperty(false);
    private boolean isImporting = false;

    // Handled by SystemData now.

    public CharacterData() {
        for (Attribute attr : SystemData.ATTRIBUTES) {
            attributes.put(attr, new SimpleIntegerProperty(1));
            attributes.get(attr).addListener((obs, oldV, newV) -> markDirty());
        }
        for (Attribute.Category cat : Attribute.Category.values()) {
            ObjectProperty<AttributePriority> priority = new SimpleObjectProperty<>(null);
            priority.addListener((obs, oldV, newV) -> {
                if (newV != null && !isImporting) {
                    // Collision handling: unset other categories with the same priority
                    for (Map.Entry<Attribute.Category, ObjectProperty<AttributePriority>> entry : attributePriorities
                            .entrySet()) {
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
            abilities.get(ability).addListener((obs, oldV, newV) -> markDirty());
            casteAbilities.put(ability, new SimpleBooleanProperty(false));
            casteAbilities.get(ability).addListener((obs, oldV, newV) -> {
                updateCasteFavoredCounts();
                updateValidSupernalAbilities();
                markDirty();
            });
            favoredAbilities.put(ability, new SimpleBooleanProperty(false));
            favoredAbilities.get(ability).addListener((obs, oldV, newV) -> {
                updateCasteFavoredCounts();
                markDirty();
            });
        }

        armors.addListener((javafx.collections.ListChangeListener.Change<? extends Armor> c) -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (Armor a : c.getAddedSubList()) {
                        a.equippedProperty().addListener((obs, old, nv) -> {
                            if (nv && !isImporting) {
                                for (Armor other : armors) {
                                    if (other != a)
                                        other.setEquipped(false);
                                }
                            }
                            updateCombatStats();
                        });
                    }
                }
            }
            updateCombatStats();
        });

        attributes.get(Attribute.STAMINA).addListener((obs, oldV, newV) -> updateCombatStats());
        attributes.get(Attribute.DEXTERITY).addListener((obs, oldV, newV) -> updateDerivedStats());
        attributes.get(Attribute.WITS).addListener((obs, oldV, newV) -> updateDerivedStats());
        attributes.get(Attribute.MANIPULATION).addListener((obs, oldV, newV) -> updateDerivedStats());

        getAbilityProperty(Ability.DODGE).addListener((obs, old, nv) -> updateDerivedStats());
        getAbilityProperty(Ability.INTEGRITY).addListener((obs, old, nv) -> updateDerivedStats());
        getAbilityProperty(Ability.SOCIALIZE).addListener((obs, old, nv) -> updateDerivedStats());
        getAbilityProperty(Ability.AWARENESS).addListener((obs, old, nv) -> updateDerivedStats());

        // Attack pool listeners
        attributes.get(Attribute.DEXTERITY).addListener((obs, old, nv) -> updateAttackPools());
        attributes.get(Attribute.STRENGTH).addListener((obs, old, nv) -> updateAttackPools());
        getAbilityProperty(Ability.MELEE).addListener((obs, old, nv) -> updateAttackPools());
        getAbilityProperty(Ability.ARCHERY).addListener((obs, old, nv) -> updateAttackPools());
        getAbilityProperty(Ability.BRAWL).addListener((obs, old, nv) -> updateAttackPools());
        getAbilityProperty(Ability.THROWN).addListener((obs, old, nv) -> updateAttackPools());

        // Default weapon for new characters if tags exist
        if (java.nio.file.Files.exists(com.vibethema.service.EquipmentDataService.getEquipmentTagsPath())) {
            Weapon unarmed = new Weapon("Unarmed");
            unarmed.setRange(Weapon.WeaponRange.CLOSE);
            unarmed.setType(Weapon.WeaponType.MORTAL);
            unarmed.setCategory(Weapon.WeaponCategory.LIGHT);
            unarmed.getTags().addAll(java.util.Arrays.asList("Bashing", "Brawl", "Grappling", "Natural"));
            weapons.add(unarmed);
        }

        getAbilityProperty(Ability.CRAFT).addListener((obs, old, nv) -> {
            boolean isCaste = getCasteAbility(Ability.CRAFT).get();
            boolean isFavored = getFavoredAbility(Ability.CRAFT).get();
            for (CraftAbility ca : crafts) {
                ca.setCaste(isCaste);
                ca.setFavored(isFavored);
            }
        });

        getCasteAbility(Ability.BRAWL).addListener((obs, old, nv) -> {
            getCasteAbility(Ability.MARTIAL_ARTS).set(nv);
            for (MartialArtsStyle mas : martialArtsStyles)
                mas.setCaste(nv);
        });
        getFavoredAbility(Ability.BRAWL).addListener((obs, old, nv) -> {
            getFavoredAbility(Ability.MARTIAL_ARTS).set(nv);
            for (MartialArtsStyle mas : martialArtsStyles)
                mas.setFavored(nv);
        });

        name.addListener((obs, oldV, newV) -> markDirty());
        caste.addListener((obs, oldV, newV) -> markDirty());
        supernalAbility.addListener((obs, oldV, newV) -> markDirty());
        essence.addListener((obs, oldV, newV) -> markDirty());
        willpower.addListener((obs, oldV, newV) -> markDirty());
        limitTrigger.addListener((obs, oldV, newV) -> markDirty());
        limit.addListener((obs, oldV, newV) -> markDirty());

        merits.addListener((javafx.collections.ListChangeListener.Change<? extends Merit> c) -> markDirty());
        specialties.addListener((javafx.collections.ListChangeListener<? super Specialty>) c -> {
            markDirty();
            updateDerivedStats();
            updateAttackPools();
        });
        crafts.addListener((javafx.collections.ListChangeListener<? super CraftAbility>) c -> markDirty());
        martialArtsStyles.addListener((javafx.collections.ListChangeListener<? super MartialArtsStyle>) c -> {
            markDirty();
            while (c.next()) {
                if (c.wasAdded()) {
                    for (MartialArtsStyle mas : c.getAddedSubList()) {
                        mas.ratingProperty().addListener((obs, ov, nv) -> markDirty());
                        mas.styleNameProperty().addListener((obs, ov, nv) -> markDirty());
                    }
                }
            }
        });
        weapons.addListener((javafx.collections.ListChangeListener<? super Weapon>) c -> {
            markDirty();
            updateAttackPools();
        });
        intimacies.addListener((javafx.collections.ListChangeListener<? super Intimacy>) c -> markDirty());
        otherEquipment.addListener((javafx.collections.ListChangeListener<? super OtherEquipment>) c -> {
            markDirty();
            while (c.next()) {
                if (c.wasAdded()) {
                    for (OtherEquipment oe : c.getAddedSubList()) {
                        oe.nameProperty().addListener((obs, ov, nv) -> markDirty());
                        oe.descriptionProperty().addListener((obs, ov, nv) -> markDirty());
                        oe.artifactProperty().addListener((obs, ov, nv) -> markDirty());
                    }
                }
            }
        });
        shapingRituals.addListener((javafx.collections.ListChangeListener<? super ShapingRitual>) c -> {
            markDirty();
            while (c.next()) {
                if (c.wasAdded()) {
                    for (ShapingRitual r : c.getAddedSubList()) {
                        r.nameProperty().addListener((obs, ov, nv) -> markDirty());
                        r.descriptionProperty().addListener((obs, ov, nv) -> markDirty());
                    }
                }
            }
        });
        spells.addListener((javafx.collections.ListChangeListener<? super Spell>) c -> {
            markDirty();
            while (c.next()) {
                if (c.wasAdded()) {
                    for (Spell s : c.getAddedSubList()) {
                        s.nameProperty().addListener((obs, ov, nv) -> markDirty());
                        s.descriptionProperty().addListener((obs, ov, nv) -> markDirty());
                        s.circleProperty().addListener((obs, ov, nv) -> markDirty());
                        s.costProperty().addListener((obs, ov, nv) -> markDirty());
                        s.durationProperty().addListener((obs, ov, nv) -> markDirty());
                        s.getKeywords()
                                .addListener((javafx.collections.ListChangeListener<? super String>) kc -> markDirty());
                    }
                }
            }
        });

        unlockedCharms.addListener((javafx.collections.ListChangeListener<? super PurchasedCharm>) c -> {
            updateSorceryAvailability();
            markDirty();
        });

        merits.add(new Merit("", 1));
        specialties.add(new Specialty("", ""));
        updateCombatStats();
        updateDerivedStats();
        updateValidSupernalAbilities();
        updateAttackPools();
        updateSorceryAvailability();
        updateCasteFavoredCounts();
        dirty.set(false);
    }

    private void updateCasteFavoredCounts() {
        Set<Ability> castes = new HashSet<>();
        Set<Ability> favoreds = new HashSet<>();

        for (Ability abil : SystemData.ABILITIES) {
            if (casteAbilities.get(abil).get())
                castes.add(abil);
            if (favoredAbilities.get(abil).get())
                favoreds.add(abil);
        }

        int c = 0;
        int f = 0;

        boolean pooledCaste = castes.remove(Ability.BRAWL) | castes.remove(Ability.MARTIAL_ARTS);
        boolean pooledFavored = favoreds.remove(Ability.BRAWL) | favoreds.remove(Ability.MARTIAL_ARTS);

        c = castes.size() + (pooledCaste ? 1 : 0);
        f = favoreds.size() + (pooledFavored ? 1 : 0);

        casteAbilityCount.set(c);
        favoredAbilityCount.set(f);
    }

    private void updateSorceryAvailability() {
        terrestrialSorceryAvailable.set(hasCharmByName(SystemData.TERRESTRIAL_CIRCLE_SORCERY));
        celestialSorceryAvailable.set(hasCharmByName(SystemData.CELESTIAL_CIRCLE_SORCERY));
        solarSorceryAvailable.set(hasCharmByName(SystemData.SOLAR_CIRCLE_SORCERY));
    }

    private void markDirty() {
        if (!isImporting)
            dirty.set(true);
    }

    public BooleanProperty dirtyProperty() {
        return dirty;
    }

    public boolean isDirty() {
        return dirty.get();
    }

    public void setDirty(boolean dirty) {
        this.dirty.set(dirty);
    }

    public boolean isImporting() {
        return isImporting;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public ObjectProperty<Caste> casteProperty() {
        return caste;
    }

    public ObjectProperty<CharacterMode> modeProperty() {
        return mode;
    }

    public CharacterMode getMode() {
        return mode.get();
    }

    public void setMode(CharacterMode mode) {
        this.mode.set(mode);
    }

    public boolean isExperienced() {
        return mode.get() == CharacterMode.EXPERIENCED;
    }

    public StringProperty supernalAbilityProperty() {
        return supernalAbility;
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

    public ObjectProperty<AttributePriority> getAttributePriority(Attribute.Category category) {
        return attributePriorities.get(category);
    }

    public ObservableMap<Attribute.Category, ObjectProperty<AttributePriority>> getAttributePriorities() {
        return attributePriorities;
    }

    public IntegerProperty casteAbilityCountProperty() {
        return casteAbilityCount;
    }

    public IntegerProperty favoredAbilityCountProperty() {
        return favoredAbilityCount;
    }

    public BooleanProperty overBonusPointLimitProperty() {
        return overBonusPointLimit;
    }

    public BooleanProperty terrestrialSorceryAvailableProperty() {
        return terrestrialSorceryAvailable;
    }

    public BooleanProperty celestialSorceryAvailableProperty() {
        return celestialSorceryAvailable;
    }

    public BooleanProperty solarSorceryAvailableProperty() {
        return solarSorceryAvailable;
    }

    public ObservableList<AttackPoolData> getAttackPools() {
        return attackPools;
    }

    public ObservableList<String> getValidSupernalAbilities() {
        return validSupernalAbilities;
    }

    public IntegerProperty essenceProperty() {
        return essence;
    }

    public IntegerProperty willpowerProperty() {
        return willpower;
    }

    public StringProperty limitTriggerProperty() {
        return limitTrigger;
    }

    public IntegerProperty limitProperty() {
        return limit;
    }

    public IntegerProperty evasionProperty() {
        return evasion;
    }

    public IntegerProperty evasionBonusProperty() {
        return evasionBonus;
    }

    public BooleanProperty hasEvasionSpecialtyProperty() {
        return hasEvasionSpecialty;
    }

    public IntegerProperty resolveProperty() {
        return resolve;
    }

    public IntegerProperty resolveBonusProperty() {
        return resolveBonus;
    }

    public BooleanProperty hasResolveSpecialtyProperty() {
        return hasResolveSpecialty;
    }

    public IntegerProperty guileProperty() {
        return guile;
    }

    public IntegerProperty guileBonusProperty() {
        return guileBonus;
    }

    public BooleanProperty hasGuileSpecialtyProperty() {
        return hasGuileSpecialty;
    }

    public IntegerProperty joinBattleProperty() {
        return joinBattle;
    }

    public BooleanProperty hasJoinBattleSpecialtyProperty() {
        return hasJoinBattleSpecialty;
    }

    public IntegerProperty getAbilityProperty(Ability name) {
        return abilities.get(name);
    }

    public int getAbilityRating(Ability name) {
        if (name == null)
            return 0;
        return abilities.get(name).get();
    }

    public int getAbilityRatingByName(String name) {
        if (name == null)
            return 0;
        Ability common = Ability.fromString(name);
        if (common != null)
            return abilities.get(common).get();

        for (MartialArtsStyle mas : martialArtsStyles) {
            if (mas.getStyleName().equals(name))
                return mas.getRating();
        }
        for (CraftAbility ca : crafts) {
            if (ca.getExpertise().equals(name))
                return ca.getRating();
        }
        return 0;
    }

    public boolean isMartialArtsStyle(String name) {
        if (name == null)
            return false;
        return martialArtsStyles.stream().anyMatch(s -> s.getStyleName().equals(name));
    }

    public boolean isCraftExpertise(String name) {
        if (name == null)
            return false;
        return crafts.stream().anyMatch(c -> c.getExpertise().equals(name));
    }

    public IntegerProperty getAbilityPropertyByName(String name) {
        if (name == null)
            return null;
        Ability common = Ability.fromString(name);
        if (common != null)
            return abilities.get(common);

        for (MartialArtsStyle mas : martialArtsStyles) {
            if (mas.getStyleName().equals(name))
                return mas.ratingProperty();
        }
        for (CraftAbility ca : crafts) {
            if (ca.getExpertise().equals(name))
                return ca.ratingProperty();
        }
        return null;
    }

    public ObservableList<PurchasedCharm> getUnlockedCharms() {
        return unlockedCharms;
    }

    public ObservableList<Merit> getMerits() {
        return merits;
    }

    public ObservableList<Specialty> getSpecialties() {
        return specialties;
    }

    public Specialty getSpecialtyById(String id) {
        if (id == null || id.isEmpty())
            return null;
        return specialties.stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null);
    }

    public ObservableList<CraftAbility> getCrafts() {
        return crafts;
    }

    public ObservableList<MartialArtsStyle> getMartialArtsStyles() {
        return martialArtsStyles;
    }

    public ObservableList<Weapon> getWeapons() {
        return weapons;
    }

    public ObservableList<Armor> getArmors() {
        return armors;
    }

    public ObservableList<Hearthstone> getHearthstones() {
        return hearthstones;
    }

    public ObservableList<OtherEquipment> getOtherEquipment() {
        return otherEquipment;
    }

    public ObservableList<Intimacy> getIntimacies() {
        return intimacies;
    }

    public ObservableList<ShapingRitual> getShapingRituals() {
        return shapingRituals;
    }

    public ObservableList<Spell> getSpells() {
        return spells;
    }

    public ObservableList<XpAward> getXpAwards() {
        return xpAwards;
    }

    public CharacterSaveState getCreationSnapshot() {
        return creationSnapshot;
    }

    public void setCreationSnapshot(CharacterSaveState snapshot) {
        this.creationSnapshot = snapshot;
        markDirty();
    }

    public void removeMerit(Merit merit) {
        merits.remove(merit);
        markDirty();
    }

    public IntegerProperty naturalSoakProperty() {
        return naturalSoak;
    }

    public IntegerProperty armorSoakProperty() {
        return armorSoak;
    }

    public IntegerProperty totalSoakProperty() {
        return totalSoak;
    }

    public IntegerProperty totalHardnessProperty() {
        return totalHardness;
    }

    public void updateCombatStats() {
        int stamina = attributes.get(Attribute.STAMINA).get();
        int armorSoakVal = 0;
        int armorHardnessVal = 0;
        for (Armor a : armors) {
            if (a.isEquipped()) {
                armorSoakVal = a.getSoak();
                armorHardnessVal = a.getHardness();
                break;
            }
        }
        naturalSoak.set(stamina);
        armorSoak.set(armorSoakVal);
        totalSoak.set(stamina + armorSoakVal);
        totalHardness.set(armorHardnessVal);
    }

    public void updateDerivedStats() {
        int dex = attributes.get(Attribute.DEXTERITY).get();
        int dodge = abilities.get(Ability.DODGE).get();
        int wits = attributes.get(Attribute.WITS).get();
        int integrity = abilities.get(Ability.INTEGRITY).get();
        int manipulation = attributes.get(Attribute.MANIPULATION).get();
        int socialize = abilities.get(Ability.SOCIALIZE).get();
        int awareness = abilities.get(Ability.AWARENESS).get();

        int evasionBase = (int) Math.ceil((dex + dodge) / 2.0);
        int resolveBase = (int) Math.ceil((wits + integrity) / 2.0);
        int guileBase = (int) Math.ceil((manipulation + socialize) / 2.0);

        evasion.set(evasionBase);
        resolve.set(resolveBase);
        guile.set(guileBase);
        joinBattle.set(wits + awareness);

        evasionBonus.set((int) Math.ceil((dex + dodge + 1) / 2.0) - evasionBase);
        resolveBonus.set((int) Math.ceil((wits + integrity + 1) / 2.0) - resolveBase);
        guileBonus.set((int) Math.ceil((manipulation + socialize + 1) / 2.0) - guileBase);

        boolean evasionSpec = specialties.stream().anyMatch(s -> Ability.DODGE.getDisplayName().equals(s.getAbility())
                && s.getName() != null && !s.getName().trim().isEmpty());
        boolean resolveSpec = specialties.stream()
                .anyMatch(s -> Ability.INTEGRITY.getDisplayName().equals(s.getAbility()) && s.getName() != null
                        && !s.getName().trim().isEmpty());
        boolean guileSpec = specialties.stream().anyMatch(s -> Ability.SOCIALIZE.getDisplayName().equals(s.getAbility())
                && s.getName() != null && !s.getName().trim().isEmpty());
        boolean joinBattleSpec = specialties.stream()
                .anyMatch(s -> Ability.AWARENESS.getDisplayName().equals(s.getAbility()) && s.getName() != null
                        && !s.getName().trim().isEmpty());

        hasEvasionSpecialty.set(evasionSpec);
        hasResolveSpecialty.set(resolveSpec);
        hasGuileSpecialty.set(guileSpec);
        hasJoinBattleSpecialty.set(joinBattleSpec);
    }

    public void updateAttackPools() {
        List<AttackPoolData> newPools = new ArrayList<>();
        int dex = attributes.get(Attribute.DEXTERITY).get();
        int str = attributes.get(Attribute.STRENGTH).get();

        for (Weapon w : weapons) {
            String abilityName = Ability.MELEE.getDisplayName();
            if (w.getRange() == Weapon.WeaponRange.ARCHERY)
                abilityName = Ability.ARCHERY.getDisplayName();
            else if (w.getRange() == Weapon.WeaponRange.THROWN)
                abilityName = Ability.THROWN.getDisplayName();
            else if (w.getTags().contains("Brawl"))
                abilityName = Ability.BRAWL.getDisplayName();

            int abil = getAbilityRating(Ability.fromString(abilityName));
            int spec = (w.getSpecialtyId() != null && !w.getSpecialtyId().isEmpty()) ? 1 : 0;

            String witheringStr;
            if (w.getRange() == Weapon.WeaponRange.CLOSE) {
                int withering = dex + abil + spec + w.getAccuracy();
                witheringStr = String.valueOf(withering);
            } else {
                int base = dex + abil + spec;
                witheringStr = String.format("C:%d | S:%d | M:%d | L:%d | E:%d",
                        base + w.getCloseRangeBonus(),
                        base + w.getShortRangeBonus(),
                        base + w.getMediumRangeBonus(),
                        base + w.getLongRangeBonus(),
                        base + w.getExtremeRangeBonus());
            }

            int decisive = dex + abil + spec;
            int damage = str + w.getDamage();
            int parryPool = dex + abil + spec;
            int parry = (int) Math.ceil(parryPool / 2.0) + w.getDefense();

            newPools.add(new AttackPoolData(w, abilityName, witheringStr, decisive, damage, parry));
        }
        attackPools.setAll(newPools);
    }

    private void updateValidSupernalAbilities() {
        String current = supernalAbility.get();
        List<String> options = new ArrayList<>();
        options.add(""); // None
        for (Ability abil : SystemData.ABILITIES) {
            if (casteAbilities.get(abil).get()) {
                options.add(abil.getDisplayName());
            }
        }
        validSupernalAbilities.setAll(options);
        if (!options.contains(current)) {
            supernalAbility.set("");
        }
    }

    public boolean isArtifactPossessed(String artifactId) {
        if (artifactId == null || artifactId.isEmpty())
            return false;
        for (Weapon w : weapons)
            if (w.getId().equals(artifactId) && w.getType() == Weapon.WeaponType.ARTIFACT)
                return true;
        for (Armor a : armors)
            if (a.getId().equals(artifactId) && a.getType() == Armor.ArmorType.ARTIFACT)
                return true;
        for (OtherEquipment oe : otherEquipment)
            if (oe.getId().equals(artifactId) && oe.isArtifact())
                return true;
        return false;
    }

    public String getArtifactName(String artifactId) {
        if (artifactId == null || artifactId.isEmpty())
            return "";

        // Check if it's a UUID pattern; if not, it's likely a legacy name already.
        if (!artifactId.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
            return artifactId;
        }

        for (Weapon w : weapons) {
            if (w.getId().equals(artifactId))
                return w.getName();
        }
        for (Armor a : armors) {
            if (a.getId().equals(artifactId))
                return a.getName();
        }
        for (OtherEquipment oe : otherEquipment) {
            if (oe.getId().equals(artifactId))
                return oe.getName();
        }
        return artifactId; // Fallback to ID if not found among possessed items
    }

    public boolean hasCharm(String id) {
        if (id == null)
            return false;
        return unlockedCharms.stream().anyMatch(c -> c.id().equals(id));
    }

    public int getCharmCount(String identity) {
        if (identity == null || identity.trim().isEmpty()) return 0;
        String trimmed = identity.trim();
        return (int) unlockedCharms.stream()
                .filter(c -> c.id().equals(trimmed) || c.name().equalsIgnoreCase(trimmed))
                .count();
    }

    public boolean hasCharmByName(String name) {
        if (name == null)
            return false;
        return unlockedCharms.stream().anyMatch(c -> c.name().equalsIgnoreCase(name.trim()));
    }

    public void addCharm(PurchasedCharm pc) {
        unlockedCharms.add(pc);
        markDirty();
    }

    public void removeCharm(String id) {
        unlockedCharms.removeIf(c -> c.id().equals(id) || c.name().equals(id));
        markDirty();
    }

    public void removeOneCharm(String id) {
        for (int i = unlockedCharms.size() - 1; i >= 0; i--) {
            PurchasedCharm c = unlockedCharms.get(i);
            if (c.id().equals(id) || c.name().equals(id)) {
                unlockedCharms.remove(i);
                break;
            }
        }
        markDirty();
    }

    public boolean hasExcellency(Ability ability) {
        if (casteAbilities.containsKey(ability)) {
            boolean isFavored = casteAbilities.get(ability).get() || favoredAbilities.get(ability).get();
            if (isFavored && abilities.get(ability).get() > 0)
                return true;
        }
        return unlockedCharms.stream()
                .anyMatch(c -> c.ability() != null && c.ability().equals(ability.getDisplayName()));
    }

    public int getPersonalMotes() {
        return (essence.get() * 3) + 10;
    }

    public int getPeripheralMotes() {
        return (essence.get() * 7) + 26;
    }

    public List<String> getHealthLevels() {
        List<String> levels = new ArrayList<>(Arrays.asList("-0", "-1", "-1", "-2", "-2", "-4", "Incap"));
        int stamina = attributes.get(Attribute.STAMINA).get();

        // Solar Ox-Body Technique ID calculation - specify UTF-8 for consistency
        String oxBodyId = java.util.UUID.nameUUIDFromBytes(
                ("Ox-Body Technique" + "|" + Ability.RESISTANCE.getDisplayName())
                .getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        int oxBodyCount = getCharmCount(oxBodyId);
        if (oxBodyCount == 0) {
            // Fallback to name search if ID doesn't match (e.g. legacy or custom ID)
            oxBodyCount = getCharmCount("Ox-Body Technique");
        }

        for (int i = 0; i < oxBodyCount; i++) {
            if (stamina >= 5) {
                levels.add("-0");
                levels.add("-1");
                levels.add("-2");
            } else if (stamina >= 3) {
                levels.add("-1");
                levels.add("-2");
                levels.add("-2");
            } else {
                levels.add("-1");
                levels.add("-2");
            }
        }
        Collections.sort(levels);
        return levels;
    }

    public CharacterSaveState exportState() {
        CharacterSaveState state = new CharacterSaveState();
        state.name = this.name.get();
        state.mode = this.mode.get() != null ? this.mode.get().name() : CharacterMode.CREATION.name();
        state.caste = caste.get() != null ? caste.get().name() : Caste.NONE.name();
        Ability supernalEnum = Ability.fromString(supernalAbility.get());
        state.supernalAbility = supernalEnum != null ? supernalEnum.name() : "";
        state.essence = essence.get();
        state.willpower = willpower.get();
        state.limitTrigger = limitTrigger.get();
        state.limit = limit.get();
        state.attributes = new HashMap<>();
        for (Attribute attr : SystemData.ATTRIBUTES)
            state.attributes.put(attr.name(), attributes.get(attr).get());
        state.attributePriorities = new HashMap<>();
        for (Map.Entry<Attribute.Category, ObjectProperty<AttributePriority>> entry : attributePriorities.entrySet()) {
            if (entry.getValue().get() != null) {
                state.attributePriorities.put(entry.getKey().name(), entry.getValue().get().name());
            }
        }
        state.abilities = new HashMap<>();
        for (Ability abil : SystemData.ABILITIES)
            state.abilities.put(abil.name(), abilities.get(abil).get());
        state.casteAbilities = new ArrayList<>();
        for (Ability abil : SystemData.ABILITIES)
            if (casteAbilities.get(abil).get())
                state.casteAbilities.add(abil.name());
        state.favoredAbilities = new ArrayList<>();
        for (Ability abil : SystemData.ABILITIES)
            if (favoredAbilities.get(abil).get())
                state.favoredAbilities.add(abil.name());
        state.unlockedCharms = new ArrayList<>(unlockedCharms);
        state.merits = new HashMap<>();
        for (Merit m : merits)
            state.merits.put(m.getName(), m.getRating());
        state.specialties = new ArrayList<>();
        for (Specialty s : specialties) {
            CharacterSaveState.SpecialtyData sd = new CharacterSaveState.SpecialtyData();
            sd.id = s.getId();
            sd.name = s.getName();
            sd.ability = s.getAbility();
            state.specialties.add(sd);
        }
        state.crafts = new ArrayList<>();
        for (CraftAbility ca : crafts) {
            CharacterSaveState.CraftData cd = new CharacterSaveState.CraftData();
            cd.expertise = ca.getExpertise();
            cd.rating = ca.getRating();
            cd.isCaste = ca.isCaste();
            cd.isFavored = ca.isFavored();
            state.crafts.add(cd);
        }
        state.martialArts = new ArrayList<>();
        for (MartialArtsStyle mas : martialArtsStyles) {
            CharacterSaveState.MartialArtsData md = new CharacterSaveState.MartialArtsData();
            md.id = mas.getId();
            md.styleName = mas.getStyleName();
            md.rating = mas.getRating();
            md.isCaste = mas.isCaste();
            md.isFavored = mas.isFavored();
            state.martialArts.add(md);
        }
        state.weapons = new ArrayList<>();
        for (Weapon w : weapons) {
            CharacterSaveState.WeaponLink wl = new CharacterSaveState.WeaponLink();
            wl.id = w.getId();
            wl.specialtyId = w.getSpecialtyId();
            wl.equipped = w.isEquipped();
            state.weapons.add(wl);
        }
        state.armors = new ArrayList<>();
        for (Armor a : armors) {
            CharacterSaveState.ArmorLink al = new CharacterSaveState.ArmorLink();
            al.id = a.getId();
            al.equipped = a.isEquipped();
            state.armors.add(al);
        }
        state.hearthstones = new ArrayList<>();
        for (Hearthstone h : hearthstones) {
            state.hearthstones.add(h.getId());
        }
        state.otherEquipment = new ArrayList<>();
        for (OtherEquipment o : otherEquipment) {
            state.otherEquipment.add(o.getId());
        }
        state.intimacies = new ArrayList<>();
        for (Intimacy i : intimacies) {
            CharacterSaveState.IntimacyData idata = new CharacterSaveState.IntimacyData();
            idata.id = i.getId();
            idata.name = i.getName();
            idata.type = i.getType();
            idata.intensity = i.getIntensity();
            idata.description = i.getDescription();
            state.intimacies.add(idata);
        }
        state.shapingRituals = new ArrayList<>();
        for (ShapingRitual r : shapingRituals) {
            CharacterSaveState.ShapingRitualData rd = new CharacterSaveState.ShapingRitualData();
            rd.id = r.getId();
            rd.name = r.getName();
            rd.description = r.getDescription();
            state.shapingRituals.add(rd);
        }
        state.spells = new ArrayList<>();
        for (Spell s : spells) {
            CharacterSaveState.SpellData sd = new CharacterSaveState.SpellData();
            sd.id = s.getId();
            sd.name = s.getName();
            sd.circle = s.getCircle();
            sd.cost = s.getCost();
            sd.keywords = new ArrayList<>(s.getKeywords());
            sd.duration = s.getDuration();
            sd.description = s.getDescription();
            state.spells.add(sd);
        }

        state.xpAwards = new ArrayList<>();
        for (XpAward a : xpAwards) {
            CharacterSaveState.XpAwardData xd = new CharacterSaveState.XpAwardData();
            xd.id = a.getId();
            xd.description = a.getDescription();
            xd.amount = a.getAmount();
            xd.isSolar = a.isSolar();
            state.xpAwards.add(xd);
        }

        state.creationSnapshot = this.creationSnapshot;

        return state;
    }

    public void importState(CharacterSaveState state, com.vibethema.service.EquipmentDataService equipmentService) {
        if (state == null)
            return;
        isImporting = true;
        try {
            this.name.set(state.name != null ? state.name : "");
            if (state.mode != null) {
                try {
                    mode.set(CharacterMode.valueOf(state.mode.trim().toUpperCase()));
                } catch (Exception e) {
                    mode.set(CharacterMode.CREATION);
                }
            }
            if (state.caste != null) {
                try {
                    caste.set(Caste.valueOf(state.caste.trim().toUpperCase()));
                } catch (Exception e) {
                    caste.set(Caste.NONE);
                }
            } else {
                caste.set(Caste.NONE);
            }
            if (state.supernalAbility != null && !state.supernalAbility.isEmpty()) {
                try {
                    supernalAbility.set(Ability.valueOf(state.supernalAbility).getDisplayName());
                } catch (IllegalArgumentException e) {
                    // Legacy display-name format — try parsing directly
                    supernalAbility.set(state.supernalAbility);
                }
            } else {
                supernalAbility.set("");
            }
            essence.set(state.essence);
            willpower.set(state.willpower);
            limitTrigger.set(state.limitTrigger != null ? state.limitTrigger : "");
            limit.set(state.limit);
            if (state.attributes != null) {
                for (Attribute attr : SystemData.ATTRIBUTES)
                    if (state.attributes.containsKey(attr.name()))
                        attributes.get(attr).set(state.attributes.get(attr.name()));
            }
            if (state.attributePriorities != null) {
                for (Attribute.Category cat : Attribute.Category.values()) {
                    String priorityName = state.attributePriorities.get(cat.name());
                    if (priorityName != null) {
                        try {
                            attributePriorities.get(cat).set(AttributePriority.valueOf(priorityName));
                        } catch (Exception e) {
                            attributePriorities.get(cat).set(null);
                        }
                    } else {
                        attributePriorities.get(cat).set(null);
                    }
                }
            } else {
                for (ObjectProperty<AttributePriority> p : attributePriorities.values())
                    p.set(null);
            }
            if (state.abilities != null) {
                for (Ability abil : SystemData.ABILITIES)
                    if (state.abilities.containsKey(abil.name()))
                        abilities.get(abil).set(state.abilities.get(abil.name()));
            }
            for (Ability abil : SystemData.ABILITIES) {
                casteAbilities.get(abil)
                        .set(state.casteAbilities != null && state.casteAbilities.contains(abil.name()));
                favoredAbilities.get(abil)
                        .set(state.favoredAbilities != null && state.favoredAbilities.contains(abil.name()));
            }
            if (state.unlockedCharms != null) {
                List<PurchasedCharm> migrated = new ArrayList<>();
                for (PurchasedCharm pc : state.unlockedCharms) {
                    String id = pc.id();
                    if (id == null || id.isEmpty()) {
                        id = java.util.UUID.nameUUIDFromBytes((pc.name().trim() + "|" + pc.ability().trim()).getBytes())
                                .toString();
                    }
                    migrated.add(new PurchasedCharm(id, pc.name(), pc.ability()));
                }
                unlockedCharms.setAll(migrated);
            } else
                unlockedCharms.clear();

            merits.clear();
            if (state.merits != null)
                for (Map.Entry<String, Integer> e : state.merits.entrySet())
                    merits.add(new Merit(e.getKey(), e.getValue()));
            if (merits.isEmpty())
                merits.add(new Merit("", 1));
            specialties.clear();
            if (state.specialties != null) {
                for (CharacterSaveState.SpecialtyData sd : state.specialties) {
                    specialties.add(new Specialty(sd.id, sd.name, sd.ability));
                }
            }
            if (specialties.isEmpty())
                specialties.add(new Specialty("", ""));
            crafts.clear();
            if (state.crafts != null) {
                for (CharacterSaveState.CraftData cd : state.crafts) {
                    CraftAbility ca = new CraftAbility(cd.expertise, cd.rating);
                    ca.setCaste(cd.isCaste);
                    ca.setFavored(cd.isFavored);
                    crafts.add(ca);
                }
            }
            martialArtsStyles.clear();
            if (state.martialArts != null) {
                for (CharacterSaveState.MartialArtsData md : state.martialArts) {
                    String id = md.id;
                    if (id == null || id.isEmpty())
                        id = java.util.UUID.randomUUID().toString();
                    MartialArtsStyle mas = new MartialArtsStyle(id, md.styleName, md.rating);
                    mas.setCaste(md.isCaste);
                    mas.setFavored(md.isFavored);
                    martialArtsStyles.add(mas);
                }
            }
            for (MartialArtsStyle mas : martialArtsStyles) {
                mas.setCaste(getCasteAbility(Ability.BRAWL).get());
                mas.setFavored(getFavoredAbility(Ability.BRAWL).get());
            }
            weapons.clear();
            if (state.weapons != null) {
                for (CharacterSaveState.WeaponLink wl : state.weapons) {
                    Weapon w = equipmentService.loadWeapon(wl.id);
                    if (w != null) {
                        w.setSpecialtyId(wl.specialtyId);
                        w.setEquipped(wl.equipped);
                        weapons.add(w);
                    }
                }
            }
            armors.clear();
            if (state.armors != null) {
                for (CharacterSaveState.ArmorLink al : state.armors) {
                    Armor a = equipmentService.loadArmor(al.id);
                    if (a != null) {
                        a.setEquipped(al.equipped);
                        armors.add(a);
                    }
                }
            }
            hearthstones.clear();
            if (state.hearthstones != null) {
                for (String id : state.hearthstones) {
                    Hearthstone h = equipmentService.loadHearthstone(id);
                    if (h != null) hearthstones.add(h);
                }
            }
            otherEquipment.clear();
            if (state.otherEquipment != null) {
                for (String id : state.otherEquipment) {
                    OtherEquipment oe = equipmentService.loadOtherEquipment(id);
                    if (oe != null) otherEquipment.add(oe);
                }
            }
            intimacies.clear();
            if (state.intimacies != null) {
                for (CharacterSaveState.IntimacyData idata : state.intimacies) {
                    Intimacy i = new Intimacy(idata.id, idata.name, idata.type, idata.intensity);
                    i.setDescription(idata.description);
                    intimacies.add(i);
                }
            }
            shapingRituals.clear();
            if (state.shapingRituals != null) {
                for (CharacterSaveState.ShapingRitualData rd : state.shapingRituals) {
                    shapingRituals.add(new ShapingRitual(rd.id, rd.name, rd.description));
                }
            }
            spells.clear();
            if (state.spells != null) {
                for (CharacterSaveState.SpellData sd : state.spells) {
                    spells.add(new Spell(sd.id, sd.name, sd.circle, sd.cost, sd.keywords, sd.duration, sd.description));
                }
            }
            xpAwards.clear();
            if (state.xpAwards != null) {
                for (CharacterSaveState.XpAwardData xd : state.xpAwards) {
                    xpAwards.add(new XpAward(xd.id, xd.description, xd.amount, xd.isSolar));
                }
            }
            this.creationSnapshot = state.creationSnapshot;

            updateValidSupernalAbilities();
            dirty.set(false);
        } finally {
            isImporting = false;
        }
    }
}
