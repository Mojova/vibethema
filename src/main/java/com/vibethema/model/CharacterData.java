package com.vibethema.model;

import com.vibethema.model.combat.AttackPoolData;
import com.vibethema.model.combat.CombatStatsModel;
import com.vibethema.model.equipment.Armor;
import com.vibethema.model.equipment.EquipmentModel;
import com.vibethema.model.equipment.Hearthstone;
import com.vibethema.model.equipment.OtherEquipment;
import com.vibethema.model.equipment.Weapon;
import com.vibethema.model.mystic.MysticModel;
import com.vibethema.model.mystic.PurchasedCharm;
import com.vibethema.model.mystic.ShapingRitual;
import com.vibethema.model.mystic.Spell;
import com.vibethema.model.progression.ProgressionModel;
import com.vibethema.model.progression.XpAward;
import com.vibethema.model.social.Intimacy;
import com.vibethema.model.social.SocialModel;
import com.vibethema.model.traits.Ability;
import com.vibethema.model.traits.Attribute;
import com.vibethema.model.traits.AttributePriority;
import com.vibethema.model.traits.CraftAbility;
import com.vibethema.model.traits.MartialArtsStyle;
import com.vibethema.model.traits.Merit;
import com.vibethema.model.traits.Specialty;
import com.vibethema.model.traits.TraitModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

public class CharacterData {
    private final StringProperty name = new SimpleStringProperty("");
    private final ObjectProperty<Caste> caste = new SimpleObjectProperty<>(Caste.NONE);
    private final ObjectProperty<CharacterMode> mode =
            new SimpleObjectProperty<>(CharacterMode.CREATION);
    private final StringProperty supernalAbility = new SimpleStringProperty("");
    private final IntegerProperty essence = new SimpleIntegerProperty(1);
    private final IntegerProperty willpower = new SimpleIntegerProperty(5);
    private final StringProperty limitTrigger = new SimpleStringProperty("");
    private final IntegerProperty limit = new SimpleIntegerProperty(0);

    private final TraitModel traitModel;
    private final EquipmentModel equipmentModel;
    private final MysticModel mysticModel;
    private final SocialModel socialModel;
    private final ProgressionModel progressionModel;
    private final CombatStatsModel combatStatsModel;

    private final BooleanProperty overBonusPointLimit = new SimpleBooleanProperty(false);
    private final BooleanProperty dirty = new SimpleBooleanProperty(false);
    private boolean isImporting = false;
    private boolean isUndoRedoInProgress = false;

    public CharacterData() {
        this.traitModel = new TraitModel(v -> markDirty(), () -> updateAllStats());
        this.equipmentModel = new EquipmentModel(v -> markDirty(), () -> updateAttackPools());
        this.mysticModel = new MysticModel(v -> markDirty(), () -> updateDerivedStats());
        this.socialModel = new SocialModel(v -> markDirty());
        this.progressionModel = new ProgressionModel(v -> markDirty());
        this.combatStatsModel = new CombatStatsModel(traitModel, equipmentModel, mysticModel);

        name.addListener((obs, oldV, newV) -> markDirty());
        caste.addListener((obs, oldV, newV) -> markDirty());
        supernalAbility.addListener((obs, oldV, newV) -> markDirty());
        essence.addListener(
                (obs, oldV, newV) -> {
                    updateDerivedStats();
                    markDirty();
                });
        willpower.addListener((obs, oldV, newV) -> markDirty());
        limitTrigger.addListener((obs, oldV, newV) -> markDirty());
        limit.addListener((obs, oldV, newV) -> markDirty());

        traitModel
                .getValidSupernalAbilities()
                .addListener(
                        (javafx.collections.ListChangeListener<String>)
                                c -> {
                                    if (isImporting) return;
                                    if (!traitModel
                                            .getValidSupernalAbilities()
                                            .contains(supernalAbility.get())) {
                                        supernalAbility.set("");
                                    }
                                });

        getCrafts()
                .addListener(
                        (javafx.collections.ListChangeListener<? super CraftAbility>)
                                c -> {
                                    while (c.next()) {
                                        if (c.wasAdded()) {
                                            for (CraftAbility ca : c.getAddedSubList()) {
                                                ca.casteProperty()
                                                        .bind(getCasteAbility(Ability.CRAFT));
                                                ca.favoredProperty()
                                                        .bind(getFavoredAbility(Ability.CRAFT));
                                            }
                                        }
                                    }
                                });

        isImporting = true;
        try {
            updateCombatStats();
            updateDerivedStats();
            updateAttackPools();
        } finally {
            isImporting = false;
        }
        dirty.set(false);
    }

    private void markDirty() {
        if (!isImporting) dirty.set(true);
    }

    // Delegated Methods - TraitModel
    public IntegerProperty getAttribute(Attribute name) {
        return traitModel.getAttribute(name);
    }

    public ObservableMap<Attribute, IntegerProperty> getAttributes() {
        return traitModel.getAttributes();
    }

    public IntegerProperty getAbility(Ability name) {
        return traitModel.getAbility(name);
    }

    public ObservableMap<Ability, IntegerProperty> getAbilities() {
        return traitModel.getAbilities();
    }

    public BooleanProperty getCasteAbility(Ability name) {
        return traitModel.getCasteAbility(name);
    }

    public BooleanProperty getFavoredAbility(Ability name) {
        return traitModel.getFavoredAbility(name);
    }

    public ObservableMap<Ability, BooleanProperty> getCasteAbilities() {
        return traitModel.getCasteAbilities();
    }

    public ObservableMap<Ability, BooleanProperty> getFavoredAbilities() {
        return traitModel.getFavoredAbilities();
    }

    public ObjectProperty<AttributePriority> getAttributePriority(Attribute.Category category) {
        return traitModel.getAttributePriority(category);
    }

    public ObservableMap<Attribute.Category, ObjectProperty<AttributePriority>>
            getAttributePriorities() {
        return traitModel.getAttributePriorities();
    }

    public ObservableList<Specialty> getSpecialties() {
        return traitModel.getSpecialties();
    }

    public ObservableList<CraftAbility> getCrafts() {
        return traitModel.getCrafts();
    }

    public ObservableList<MartialArtsStyle> getMartialArtsStyles() {
        return traitModel.getMartialArtsStyles();
    }

    public ObservableList<Merit> getMerits() {
        return traitModel.getMerits();
    }

    public IntegerProperty casteAbilityCountProperty() {
        return traitModel.casteAbilityCountProperty();
    }

    public IntegerProperty favoredAbilityCountProperty() {
        return traitModel.favoredAbilityCountProperty();
    }

    public ObservableList<String> getValidSupernalAbilities() {
        return traitModel.getValidSupernalAbilities();
    }

    public int getAbilityRating(Ability name) {
        return traitModel.getAbilityRating(name);
    }

    public int getAbilityRatingByName(String name) {
        return traitModel.getAbilityRatingByName(name);
    }

    public IntegerProperty getAbilityPropertyByName(String name) {
        return traitModel.getAbilityPropertyByName(name);
    }

    public boolean isMartialArtsStyle(String name) {
        return traitModel.isMartialArtsStyle(name);
    }

    public boolean isCraftExpertise(String name) {
        return traitModel.isCraftExpertise(name);
    }

    public Specialty getSpecialtyById(String id) {
        return getSpecialties().stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null);
    }

    public void removeMerit(Merit merit) {
        getMerits().remove(merit);
        markDirty();
    }

    // Delegated Methods - EquipmentModel
    public ObservableList<Weapon> getWeapons() {
        return equipmentModel.getWeapons();
    }

    public ObservableList<Armor> getArmors() {
        return equipmentModel.getArmors();
    }

    public ObservableList<Hearthstone> getHearthstones() {
        return equipmentModel.getHearthstones();
    }

    public ObservableList<OtherEquipment> getOtherEquipment() {
        return equipmentModel.getOtherEquipment();
    }

    public IntegerProperty armorSoakProperty() {
        return equipmentModel.armorSoakProperty();
    }

    public IntegerProperty totalHardnessProperty() {
        return equipmentModel.totalHardnessProperty();
    }

    // Delegated Methods - MysticModel
    public ObservableList<PurchasedCharm> getUnlockedCharms() {
        return mysticModel.getUnlockedCharms();
    }

    public ObservableList<ShapingRitual> getShapingRituals() {
        return mysticModel.getShapingRituals();
    }

    public ObservableList<Spell> getSpells() {
        return mysticModel.getSpells();
    }

    public BooleanProperty terrestrialSorceryAvailableProperty() {
        return mysticModel.terrestrialSorceryAvailableProperty();
    }

    public BooleanProperty celestialSorceryAvailableProperty() {
        return mysticModel.celestialSorceryAvailableProperty();
    }

    public BooleanProperty solarSorceryAvailableProperty() {
        return mysticModel.solarSorceryAvailableProperty();
    }

    public boolean hasCharm(String id) {
        return getUnlockedCharms().stream().anyMatch(c -> c.id().equals(id));
    }

    public int getCharmCount(String identity) {
        if (identity == null || identity.trim().isEmpty()) return 0;
        String trimmed = identity.trim();
        return (int)
                getUnlockedCharms().stream()
                        .filter(c -> c.id().equals(trimmed) || c.name().equalsIgnoreCase(trimmed))
                        .count();
    }

    public boolean hasCharmByName(String name) {
        if (name == null) return false;
        return getUnlockedCharms().stream().anyMatch(c -> c.name().equalsIgnoreCase(name.trim()));
    }

    public void addCharm(PurchasedCharm pc) {
        getUnlockedCharms().add(pc);
        markDirty();
    }

    public void removeCharm(String id) {
        getUnlockedCharms().removeIf(c -> c.id().equals(id) || c.name().equals(id));
        markDirty();
    }

    public void removeOneCharm(String id) {
        for (int i = getUnlockedCharms().size() - 1; i >= 0; i--) {
            PurchasedCharm c = getUnlockedCharms().get(i);
            if (c.id().equals(id) || c.name().equals(id)) {
                getUnlockedCharms().remove(i);
                break;
            }
        }
        markDirty();
    }

    // Delegated Methods - SocialModel
    public ObservableList<Intimacy> getIntimacies() {
        return socialModel.getIntimacies();
    }

    // Delegated Methods - ProgressionModel
    public ObservableList<XpAward> getXpAwards() {
        return progressionModel.getXpAwards();
    }

    public CharacterSaveState getCreationSnapshot() {
        return progressionModel.getCreationSnapshot();
    }

    public void setCreationSnapshot(CharacterSaveState snapshot) {
        progressionModel.setCreationSnapshot(snapshot);
    }

    // Delegated Methods - CombatStatsModel
    public IntegerProperty naturalSoakProperty() {
        return combatStatsModel.naturalSoakProperty();
    }

    public IntegerProperty totalSoakProperty() {
        return combatStatsModel.totalSoakProperty();
    }

    public IntegerProperty evasionProperty() {
        return combatStatsModel.evasionProperty();
    }

    public IntegerProperty evasionBonusProperty() {
        return combatStatsModel.evasionBonusProperty();
    }

    public BooleanProperty hasEvasionSpecialtyProperty() {
        return combatStatsModel.hasEvasionSpecialtyProperty();
    }

    public IntegerProperty resolveProperty() {
        return combatStatsModel.resolveProperty();
    }

    public IntegerProperty resolveBonusProperty() {
        return combatStatsModel.resolveBonusProperty();
    }

    public BooleanProperty hasResolveSpecialtyProperty() {
        return combatStatsModel.hasResolveSpecialtyProperty();
    }

    public IntegerProperty guileProperty() {
        return combatStatsModel.guileProperty();
    }

    public IntegerProperty guileBonusProperty() {
        return combatStatsModel.guileBonusProperty();
    }

    public BooleanProperty hasGuileSpecialtyProperty() {
        return combatStatsModel.hasGuileSpecialtyProperty();
    }

    public IntegerProperty joinBattleProperty() {
        return combatStatsModel.joinBattleProperty();
    }

    public BooleanProperty hasJoinBattleSpecialtyProperty() {
        return combatStatsModel.hasJoinBattleSpecialtyProperty();
    }

    public IntegerProperty personalMotesProperty() {
        return combatStatsModel.personalMotesProperty();
    }

    public IntegerProperty peripheralMotesProperty() {
        return combatStatsModel.peripheralMotesProperty();
    }

    public ObservableList<String> healthLevelsProperty() {
        return combatStatsModel.healthLevelsProperty();
    }

    public ObservableList<AttackPoolData> getAttackPools() {
        return combatStatsModel.getAttackPools();
    }

    public int getPersonalMotes() {
        return combatStatsModel.personalMotesProperty().get();
    }

    public int getPeripheralMotes() {
        return combatStatsModel.peripheralMotesProperty().get();
    }

    public List<String> getHealthLevels() {
        return new ArrayList<>(combatStatsModel.healthLevelsProperty());
    }

    public void updateCombatStats() {
        combatStatsModel.updateCombatStats();
    }

    public void updateDerivedStats() {
        combatStatsModel.updateDerivedStats();
        combatStatsModel.updateMotesAndHealth(essence.get());
    }

    public void updateAttackPools() {
        combatStatsModel.updateAttackPools();
    }

    public void updateAllStats() {
        updateCombatStats();
        updateDerivedStats();
        updateAttackPools();
    }

    // Core Properties
    public StringProperty nameProperty() {
        return name;
    }

    public ObjectProperty<Caste> casteProperty() {
        return caste;
    }

    public ObjectProperty<CharacterMode> modeProperty() {
        return mode;
    }

    public StringProperty supernalAbilityProperty() {
        return supernalAbility;
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

    public BooleanProperty dirtyProperty() {
        return dirty;
    }

    public BooleanProperty overBonusPointLimitProperty() {
        return overBonusPointLimit;
    }

    public CharacterMode getMode() {
        return mode.get();
    }

    public void setMode(CharacterMode v) {
        mode.set(v);
    }

    public boolean isExperienced() {
        return mode.get() == CharacterMode.EXPERIENCED;
    }

    public boolean isDirty() {
        return dirty.get();
    }

    public void setDirty(boolean v) {
        dirty.set(v);
    }

    public boolean isImporting() {
        return isImporting;
    }

    public boolean isUndoRedoInProgress() {
        return isUndoRedoInProgress;
    }

    public void setUndoRedoInProgress(boolean v) {
        this.isUndoRedoInProgress = v;
    }

    public void clear() {
        isImporting = true;
        try {
            name.set("");
            caste.set(Caste.NONE);
            mode.set(CharacterMode.CREATION);
            supernalAbility.set("");
            essence.set(1);
            willpower.set(5);
            limitTrigger.set("");
            limit.set(0);

            traitModel.getAttributes().values().forEach(p -> p.set(1));
            traitModel.getAbilities().values().forEach(p -> p.set(0));
            traitModel.getCasteAbilities().values().forEach(p -> p.set(false));
            traitModel.getFavoredAbilities().values().forEach(p -> p.set(false));
            traitModel.getAttributePriorities().values().forEach(p -> p.set(null));

            getUnlockedCharms().clear();
            getMerits().clear();
            getMerits().add(new Merit(java.util.UUID.randomUUID().toString(), "", 1));
            getSpecialties().clear();
            getSpecialties().add(new Specialty("", ""));
            getCrafts().clear();
            getMartialArtsStyles().clear();
            getWeapons().clear();
            getArmors().clear();
            getHearthstones().clear();
            getOtherEquipment().clear();
            getIntimacies().clear();
            getShapingRituals().clear();
            getSpells().clear();
            getXpAwards().clear();
            setCreationSnapshot(null);

            updateCombatStats();
            updateDerivedStats();
            updateAttackPools();
        } finally {
            isImporting = false;
            dirty.set(false);
        }
    }

    // Helpers
    public boolean isArtifactPossessed(String artifactId) {
        if (artifactId == null || artifactId.isEmpty()) return false;
        if (getWeapons().stream()
                .anyMatch(
                        w ->
                                w.getId().equals(artifactId)
                                        && w.getType() == Weapon.WeaponType.ARTIFACT)) return true;
        if (getArmors().stream()
                .anyMatch(
                        a ->
                                a.getId().equals(artifactId)
                                        && a.getType() == Armor.ArmorType.ARTIFACT)) return true;
        return getOtherEquipment().stream()
                .anyMatch(oe -> oe.getId().equals(artifactId) && oe.isArtifact());
    }

    public String getArtifactName(String artifactId) {
        if (artifactId == null || artifactId.isEmpty()) return "";
        for (Weapon w : getWeapons()) if (w.getId().equals(artifactId)) return w.getName();
        for (Armor a : getArmors()) if (a.getId().equals(artifactId)) return a.getName();
        for (OtherEquipment oe : getOtherEquipment())
            if (oe.getId().equals(artifactId)) return oe.getName();
        return artifactId;
    }

    public boolean hasExcellency(Ability ability) {
        if (ability == null) return false;
        boolean isFavored = getCasteAbility(ability).get() || getFavoredAbility(ability).get();
        if (isFavored && getAbility(ability).get() > 0) return true;
        return getUnlockedCharms().stream()
                .anyMatch(c -> c.ability() != null && c.ability().equals(ability.getDisplayName()));
    }

    public BooleanBinding excellencyProperty(Ability ability) {
        return Bindings.createBooleanBinding(
                () -> hasExcellency(ability),
                getUnlockedCharms(),
                essence,
                getCasteAbility(ability),
                getFavoredAbility(ability),
                getAbility(ability));
    }

    // Persistence logic remains in CharacterData for now as it coordinates all models
    public CharacterSaveState exportState() {
        CharacterSaveState state = new CharacterSaveState();
        state.name = this.name.get();
        state.mode = this.mode.get().name();
        state.caste = caste.get().name();
        Ability supernalEnum = Ability.fromString(supernalAbility.get());
        state.supernalAbility = supernalEnum != null ? supernalEnum.name() : "";
        state.essence = essence.get();
        state.willpower = willpower.get();
        state.limitTrigger = limitTrigger.get();
        state.limit = limit.get();

        state.attributes = new HashMap<>();
        traitModel.getAttributes().forEach((k, v) -> state.attributes.put(k.name(), v.get()));

        state.attributePriorities = new HashMap<>();
        traitModel
                .getAttributePriorities()
                .forEach(
                        (k, v) -> {
                            if (v.get() != null)
                                state.attributePriorities.put(k.name(), v.get().name());
                        });

        state.abilities = new HashMap<>();
        traitModel.getAbilities().forEach((k, v) -> state.abilities.put(k.name(), v.get()));

        state.casteAbilities = new ArrayList<>();
        traitModel
                .getCasteAbilities()
                .forEach(
                        (k, v) -> {
                            if (v.get()) state.casteAbilities.add(k.name());
                        });

        state.favoredAbilities = new ArrayList<>();
        traitModel
                .getFavoredAbilities()
                .forEach(
                        (k, v) -> {
                            if (v.get()) state.favoredAbilities.add(k.name());
                        });

        state.unlockedCharms = new ArrayList<>(getUnlockedCharms());

        state.merits = new HashMap<>();
        getMerits().forEach(m -> state.merits.put(m.getName(), m.getRating()));

        state.specialties = new ArrayList<>();
        getSpecialties()
                .forEach(
                        s -> {
                            CharacterSaveState.SpecialtyData sd =
                                    new CharacterSaveState.SpecialtyData();
                            sd.id = s.getId();
                            sd.name = s.getName();
                            sd.ability = s.getAbility();
                            state.specialties.add(sd);
                        });

        state.crafts = new ArrayList<>();
        getCrafts()
                .forEach(
                        ca -> {
                            CharacterSaveState.CraftData cd = new CharacterSaveState.CraftData();
                            cd.id = ca.getId().toString();
                            cd.expertise = ca.getExpertise();
                            cd.rating = ca.getRating();
                            cd.isCaste = ca.isCaste();
                            cd.isFavored = ca.isFavored();
                            state.crafts.add(cd);
                        });

        state.martialArts = new ArrayList<>();
        getMartialArtsStyles()
                .forEach(
                        mas -> {
                            CharacterSaveState.MartialArtsData md =
                                    new CharacterSaveState.MartialArtsData();
                            md.id = mas.getId();
                            md.styleName = mas.getStyleName();
                            md.rating = mas.getRating();
                            md.isCaste = mas.isCaste();
                            md.isFavored = mas.isFavored();
                            state.martialArts.add(md);
                        });

        state.weapons = new ArrayList<>();
        getWeapons()
                .forEach(
                        w -> {
                            CharacterSaveState.WeaponLink wl = new CharacterSaveState.WeaponLink();
                            wl.id = w.getId();
                            wl.instanceId = w.getInstanceId();
                            wl.specialtyId = w.getSpecialtyId();
                            wl.equipped = w.isEquipped();
                            state.weapons.add(wl);
                        });

        state.armors = new ArrayList<>();
        getArmors()
                .forEach(
                        a -> {
                            CharacterSaveState.ArmorLink al = new CharacterSaveState.ArmorLink();
                            al.id = a.getId();
                            al.instanceId = a.getInstanceId();
                            al.equipped = a.isEquipped();
                            state.armors.add(al);
                        });

        state.hearthstones = new ArrayList<>();
        getHearthstones()
                .forEach(
                        h -> {
                            CharacterSaveState.HearthstoneLink hl =
                                    new CharacterSaveState.HearthstoneLink();
                            hl.id = h.getId();
                            hl.instanceId = h.getInstanceId();
                            hl.equipped = h.isEquipped();
                            state.hearthstones.add(hl);
                        });

        state.otherEquipment = new ArrayList<>();
        getOtherEquipment()
                .forEach(
                        o -> {
                            CharacterSaveState.OtherEquipmentLink ol =
                                    new CharacterSaveState.OtherEquipmentLink();
                            ol.id = o.getId();
                            ol.instanceId = o.getInstanceId();
                            ol.equipped = o.isEquipped();
                            state.otherEquipment.add(ol);
                        });

        state.intimacies = new ArrayList<>();
        getIntimacies()
                .forEach(
                        i -> {
                            CharacterSaveState.IntimacyData idata =
                                    new CharacterSaveState.IntimacyData();
                            idata.id = i.getId();
                            idata.name = i.getName();
                            idata.type = i.getType();
                            idata.intensity = i.getIntensity();
                            idata.description = i.getDescription();
                            state.intimacies.add(idata);
                        });

        state.shapingRituals = new ArrayList<>();
        getShapingRituals()
                .forEach(
                        r ->
                                state.shapingRituals.add(
                                        new CharacterSaveState.ShapingRitualData(
                                                r.getId(), r.getName(), r.getDescription())));

        state.spells = new ArrayList<>();
        getSpells()
                .forEach(
                        s ->
                                state.spells.add(
                                        new CharacterSaveState.SpellData(
                                                s.getId(),
                                                s.getName(),
                                                s.getCircle(),
                                                s.getCost(),
                                                s.getKeywords(),
                                                s.getDuration(),
                                                s.getDescription())));

        state.xpAwards = new ArrayList<>();
        getXpAwards()
                .forEach(
                        x ->
                                state.xpAwards.add(
                                        new CharacterSaveState.XpAwardData(
                                                x.getId(),
                                                x.getDescription(),
                                                x.getAmount(),
                                                x.isSolar())));

        state.creationSnapshot = getCreationSnapshot();
        return state;
    }

    public void importState(
            CharacterSaveState state, com.vibethema.service.EquipmentDataService equipmentService) {
        if (state == null) return;
        isImporting = true;
        traitModel.setImporting(true);
        equipmentModel.setImporting(true);
        mysticModel.setImporting(true);
        try {
            this.name.set(state.name != null ? state.name : "");
            if (state.mode != null) mode.set(CharacterMode.valueOf(state.mode.toUpperCase()));
            if (state.caste != null) caste.set(Caste.valueOf(state.caste.toUpperCase()));
            essence.set(state.essence);
            willpower.set(state.willpower);
            limitTrigger.set(state.limitTrigger != null ? state.limitTrigger : "");
            limit.set(state.limit);

            if (state.attributes != null) {
                for (Attribute attr : SystemData.ATTRIBUTES)
                    if (state.attributes.containsKey(attr.name()))
                        getAttribute(attr).set(state.attributes.get(attr.name()));
            }

            if (state.attributePriorities != null) {
                traitModel
                        .getAttributePriorities()
                        .forEach(
                                (cat, prop) -> {
                                    String pName = state.attributePriorities.get(cat.name());
                                    prop.set(
                                            pName != null
                                                    ? AttributePriority.valueOf(pName)
                                                    : null);
                                });
            }

            if (state.abilities != null) {
                for (Ability abil : SystemData.ABILITIES)
                    if (state.abilities.containsKey(abil.name()))
                        getAbility(abil).set(state.abilities.get(abil.name()));
            }

            for (Ability abil : SystemData.ABILITIES) {
                getCasteAbility(abil)
                        .set(
                                state.casteAbilities != null
                                        && state.casteAbilities.contains(abil.name()));
                getFavoredAbility(abil)
                        .set(
                                state.favoredAbilities != null
                                        && state.favoredAbilities.contains(abil.name()));
            }

            if (state.supernalAbility != null && !state.supernalAbility.isEmpty()) {
                try {
                    supernalAbility.set(Ability.valueOf(state.supernalAbility).getDisplayName());
                } catch (Exception e) {
                    supernalAbility.set(state.supernalAbility);
                }
            }

            if (state.unlockedCharms != null) getUnlockedCharms().setAll(state.unlockedCharms);
            else getUnlockedCharms().clear();

            getMerits().clear();
            if (state.merits != null)
                state.merits.forEach(
                        (k, v) ->
                                getMerits()
                                        .add(
                                                new Merit(
                                                        java.util.UUID.randomUUID().toString(),
                                                        k,
                                                        v)));
            if (getMerits().isEmpty())
                getMerits().add(new Merit(java.util.UUID.randomUUID().toString(), "", 1));

            getSpecialties().clear();
            if (state.specialties != null)
                state.specialties.forEach(
                        sd -> getSpecialties().add(new Specialty(sd.id, sd.name, sd.ability)));
            if (getSpecialties().isEmpty()) getSpecialties().add(new Specialty("", ""));

            getCrafts().clear();
            if (state.crafts != null)
                state.crafts.forEach(
                        cd -> {
                            java.util.UUID craftId =
                                    (cd.id != null)
                                            ? java.util.UUID.fromString(cd.id)
                                            : java.util.UUID.randomUUID();
                            CraftAbility ca = new CraftAbility(craftId, cd.expertise, cd.rating);
                            getCrafts().add(ca);
                        });

            getMartialArtsStyles().clear();
            if (state.martialArts != null)
                state.martialArts.forEach(
                        md -> {
                            MartialArtsStyle mas =
                                    new MartialArtsStyle(md.id, md.styleName, md.rating);
                            mas.setCaste(md.isCaste);
                            mas.setFavored(md.isFavored);
                            getMartialArtsStyles().add(mas);
                        });

            getWeapons().clear();
            if (state.weapons != null)
                state.weapons.forEach(
                        wl -> {
                            Weapon w = equipmentService.loadWeapon(wl.id);
                            if (w != null) {
                                w.setInstanceId(
                                        wl.instanceId != null
                                                ? wl.instanceId
                                                : java.util.UUID.randomUUID().toString());
                                w.setSpecialtyId(wl.specialtyId);
                                w.setEquipped(wl.equipped);
                                getWeapons().add(w);
                            }
                        });

            getArmors().clear();
            if (state.armors != null)
                state.armors.forEach(
                        al -> {
                            Armor a = equipmentService.loadArmor(al.id);
                            if (a != null) {
                                a.setInstanceId(
                                        al.instanceId != null
                                                ? al.instanceId
                                                : java.util.UUID.randomUUID().toString());
                                a.setEquipped(al.equipped);
                                getArmors().add(a);
                            }
                        });

            getHearthstones().clear();
            if (state.hearthstones != null)
                state.hearthstones.forEach(
                        hl -> {
                            Hearthstone h = equipmentService.loadHearthstone(hl.id);
                            if (h != null) {
                                h.setInstanceId(
                                        hl.instanceId != null
                                                ? hl.instanceId
                                                : java.util.UUID.randomUUID().toString());
                                h.setEquipped(hl.equipped);
                                getHearthstones().add(h);
                            }
                        });

            getOtherEquipment().clear();
            if (state.otherEquipment != null)
                state.otherEquipment.forEach(
                        ol -> {
                            OtherEquipment oe = equipmentService.loadOtherEquipment(ol.id);
                            if (oe != null) {
                                oe.setInstanceId(
                                        ol.instanceId != null
                                                ? ol.instanceId
                                                : java.util.UUID.randomUUID().toString());
                                oe.setEquipped(ol.equipped);
                                getOtherEquipment().add(oe);
                            }
                        });

            getIntimacies().clear();
            if (state.intimacies != null)
                state.intimacies.forEach(
                        idata -> {
                            Intimacy i =
                                    new Intimacy(idata.id, idata.name, idata.type, idata.intensity);
                            i.setDescription(idata.description);
                            getIntimacies().add(i);
                        });

            getShapingRituals().clear();
            if (state.shapingRituals != null)
                state.shapingRituals.forEach(
                        rd ->
                                getShapingRituals()
                                        .add(new ShapingRitual(rd.id, rd.name, rd.description)));

            getSpells().clear();
            if (state.spells != null)
                state.spells.forEach(
                        sd ->
                                getSpells()
                                        .add(
                                                new Spell(
                                                        sd.id,
                                                        sd.name,
                                                        sd.circle,
                                                        sd.cost,
                                                        sd.keywords,
                                                        sd.duration,
                                                        sd.description)));

            getXpAwards().clear();
            if (state.xpAwards != null)
                state.xpAwards.forEach(
                        xd ->
                                getXpAwards()
                                        .add(
                                                new XpAward(
                                                        xd.id,
                                                        xd.description,
                                                        xd.amount,
                                                        xd.isSolar)));

            setCreationSnapshot(state.creationSnapshot);
            traitModel.updateValidSupernalAbilities();
            dirty.set(false);
        } finally {
            isImporting = false;
            traitModel.setImporting(false);
            equipmentModel.setImporting(false);
            mysticModel.setImporting(false);
            updateCombatStats();
            updateDerivedStats();
            updateAttackPools();
        }
    }

    public void restoreState(
            CharacterSaveState state, com.vibethema.service.EquipmentDataService equipmentService) {
        isUndoRedoInProgress = true;
        try {
            importState(state, equipmentService);
        } finally {
            isUndoRedoInProgress = false;
        }
    }
}
