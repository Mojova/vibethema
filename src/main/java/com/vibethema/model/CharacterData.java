package com.vibethema.model;

import java.util.ArrayList;
import java.util.Arrays;


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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CharacterData {
    // Enums moved to standalone files

    private StringProperty name = new SimpleStringProperty("");
    private ObjectProperty<Caste> caste = new SimpleObjectProperty<>(Caste.NONE);
    private ObjectProperty<CharacterMode> mode = new SimpleObjectProperty<>(CharacterMode.CREATION);
    private StringProperty supernalAbility = new SimpleStringProperty("");

    private ObservableMap<String, IntegerProperty> attributes = FXCollections.observableHashMap();
    private ObservableMap<String, IntegerProperty> abilities = FXCollections.observableHashMap();
    private ObservableMap<String, BooleanProperty> casteAbilities = FXCollections.observableHashMap();
    private ObservableMap<String, BooleanProperty> favoredAbilities = FXCollections.observableHashMap();
    
    private IntegerProperty essence = new SimpleIntegerProperty(1);
    private IntegerProperty willpower = new SimpleIntegerProperty(5);
    private StringProperty limitTrigger = new SimpleStringProperty("");
    private IntegerProperty limit = new SimpleIntegerProperty(0);
    
    private final ObservableList<PurchasedCharm> unlockedCharms = FXCollections.observableArrayList();
    private final ObservableList<Merit> merits = FXCollections.observableArrayList();
    private final ObservableList<Specialty> specialties = FXCollections.observableArrayList(s -> 
        new javafx.beans.Observable[] { s.nameProperty(), s.abilityProperty() });
    private final ObservableList<CraftAbility> crafts = FXCollections.observableArrayList();
    private final ObservableList<MartialArtsStyle> martialArtsStyles = FXCollections.observableArrayList();
    private final ObservableList<Weapon> weapons = FXCollections.observableArrayList();
    private final ObservableList<Armor> armors = FXCollections.observableArrayList();
    private final ObservableList<Hearthstone> hearthstones = FXCollections.observableArrayList();
    private final ObservableList<OtherEquipment> otherEquipment = FXCollections.observableArrayList();
    private final ObservableList<Intimacy> intimacies = FXCollections.observableArrayList(i -> 
        new javafx.beans.Observable[] { i.nameProperty(), i.intensityProperty(), i.descriptionProperty() });
    private final ObservableList<ShapingRitual> shapingRituals = FXCollections.observableArrayList(r ->
        new javafx.beans.Observable[] { r.nameProperty(), r.descriptionProperty() });
    private final ObservableList<Spell> spells = FXCollections.observableArrayList(s ->
        new javafx.beans.Observable[] { s.nameProperty(), s.descriptionProperty(), s.circleProperty() });

    private final IntegerProperty naturalSoak = new SimpleIntegerProperty(0);
    private final IntegerProperty armorSoak = new SimpleIntegerProperty(0);
    private final IntegerProperty totalSoak = new SimpleIntegerProperty(0);
    private final IntegerProperty totalHardness = new SimpleIntegerProperty(0);

    private final IntegerProperty casteAbilityCount = new SimpleIntegerProperty(0);
    private final IntegerProperty favoredAbilityCount = new SimpleIntegerProperty(0);
    private final BooleanProperty overBonusPointLimit = new SimpleBooleanProperty(false);
    
    private final BooleanProperty terrestrialSorceryAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty celestialSorceryAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty solarSorceryAvailable = new SimpleBooleanProperty(false);

    private BooleanProperty dirty = new SimpleBooleanProperty(false);
    private boolean isImporting = false;

    // Handled by SystemData now.

    public CharacterData() {
        for (String attr : SystemData.ATTRIBUTES) {
            attributes.put(attr, new SimpleIntegerProperty(1));
            attributes.get(attr).addListener((obs, oldV, newV) -> markDirty());
        }
        for (String ability : SystemData.ABILITIES) {
            abilities.put(ability, new SimpleIntegerProperty(0));
            abilities.get(ability).addListener((obs, oldV, newV) -> markDirty());
            casteAbilities.put(ability, new SimpleBooleanProperty(false));
            casteAbilities.get(ability).addListener((obs, oldV, newV) -> {
                updateCasteFavoredCounts();
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
                                    if (other != a) other.setEquipped(false);
                                }
                            }
                            updateCombatStats();
                        });
                    }
                }
            }
            updateCombatStats();
        });

        attributes.get("Stamina").addListener((obs, oldV, newV) -> updateCombatStats());

        // Default weapon for new characters if tags exist
        if (java.nio.file.Files.exists(com.vibethema.service.EquipmentDataService.getEquipmentTagsPath())) {
            Weapon unarmed = new Weapon("Unarmed");
            unarmed.setRange(Weapon.WeaponRange.CLOSE);
            unarmed.setType(Weapon.WeaponType.MORTAL);
            unarmed.setCategory(Weapon.WeaponCategory.LIGHT);
            unarmed.getTags().addAll(java.util.Arrays.asList("Bashing", "Brawl", "Grappling", "Natural"));
            weapons.add(unarmed);
        }
        
        getCasteAbility("Craft").addListener((obs, old, nv) -> {
            for (CraftAbility ca : crafts) ca.setCaste(nv);
        });
        getFavoredAbility("Craft").addListener((obs, old, nv) -> {
            for (CraftAbility ca : crafts) ca.setFavored(nv);
        });

        getCasteAbility("Brawl").addListener((obs, old, nv) -> {
            getCasteAbility("Martial Arts").set(nv);
            for (MartialArtsStyle mas : martialArtsStyles) mas.setCaste(nv);
        });
        getFavoredAbility("Brawl").addListener((obs, old, nv) -> {
            getFavoredAbility("Martial Arts").set(nv);
            for (MartialArtsStyle mas : martialArtsStyles) mas.setFavored(nv);
        });
        
        name.addListener((obs, oldV, newV) -> markDirty());
        caste.addListener((obs, oldV, newV) -> markDirty());
        supernalAbility.addListener((obs, oldV, newV) -> markDirty());
        essence.addListener((obs, oldV, newV) -> markDirty());
        willpower.addListener((obs, oldV, newV) -> markDirty());
        limitTrigger.addListener((obs, oldV, newV) -> markDirty());
        limit.addListener((obs, oldV, newV) -> markDirty());

        merits.addListener((javafx.collections.ListChangeListener.Change<? extends Merit> c) -> markDirty());
        specialties.addListener((javafx.collections.ListChangeListener<? super Specialty>) c -> markDirty());
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
        weapons.addListener((javafx.collections.ListChangeListener<? super Weapon>) c -> markDirty());
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
                        s.getKeywords().addListener((javafx.collections.ListChangeListener<? super String>) kc -> markDirty());
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
        updateSorceryAvailability();
        updateCasteFavoredCounts();
        dirty.set(false);
    }

    private void updateCasteFavoredCounts() {
        int c = 0;
        int f = 0;
        for (String abil : SystemData.ABILITIES) {
            if ("Martial Arts".equals(abil)) continue;
            if (casteAbilities.get(abil).get()) c++;
            if (favoredAbilities.get(abil).get()) f++;
        }
        casteAbilityCount.set(c);
        favoredAbilityCount.set(f);
    }

    private void updateSorceryAvailability() {
        terrestrialSorceryAvailable.set(hasCharmByName(SystemData.TERRESTRIAL_CIRCLE_SORCERY));
        celestialSorceryAvailable.set(hasCharmByName(SystemData.CELESTIAL_CIRCLE_SORCERY));
        solarSorceryAvailable.set(hasCharmByName(SystemData.SOLAR_CIRCLE_SORCERY));
    }

    private void markDirty() {
        if (!isImporting) dirty.set(true);
    }

    public BooleanProperty dirtyProperty() { return dirty; }
    public boolean isDirty() { return dirty.get(); }
    public void setDirty(boolean dirty) { this.dirty.set(dirty); }
    public boolean isImporting() { return isImporting; }

    public StringProperty nameProperty() { return name; }
    public ObjectProperty<Caste> casteProperty() { return caste; }
    public ObjectProperty<CharacterMode> modeProperty() { return mode; }
    public CharacterMode getMode() { return mode.get(); }
    public void setMode(CharacterMode mode) { this.mode.set(mode); }
    public StringProperty supernalAbilityProperty() { return supernalAbility; }
    public IntegerProperty getAttribute(String name) { return attributes.get(name); }
    public IntegerProperty getAbility(String name) { return abilities.get(name); }
    public BooleanProperty getCasteAbility(String name) { return casteAbilities.get(name); }
    public BooleanProperty getFavoredAbility(String name) { return favoredAbilities.get(name); }

    public IntegerProperty casteAbilityCountProperty() { return casteAbilityCount; }
    public IntegerProperty favoredAbilityCountProperty() { return favoredAbilityCount; }
    public BooleanProperty overBonusPointLimitProperty() { return overBonusPointLimit; }
    public BooleanProperty terrestrialSorceryAvailableProperty() { return terrestrialSorceryAvailable; }
    public BooleanProperty celestialSorceryAvailableProperty() { return celestialSorceryAvailable; }
    public BooleanProperty solarSorceryAvailableProperty() { return solarSorceryAvailable; }
    
    public IntegerProperty essenceProperty() { return essence; }
    public IntegerProperty willpowerProperty() { return willpower; }
    
    public StringProperty limitTriggerProperty() { return limitTrigger; }
    public IntegerProperty limitProperty() { return limit; }

    public int getAbilityRating(String name) {
        if (name == null) return 0;
        if (abilities.containsKey(name)) {
            int baseRating = abilities.get(name).get();
            if (name.equals("Craft")) {
                int maxCraft = crafts.stream().mapToInt(CraftAbility::getRating).max().orElse(0);
                return Math.max(baseRating, maxCraft);
            }
            if (name.equals("Martial Arts")) {
                int maxMA = martialArtsStyles.stream().mapToInt(MartialArtsStyle::getRating).max().orElse(0);
                return Math.max(baseRating, maxMA);
            }
            return baseRating;
        }
        for (MartialArtsStyle mas : martialArtsStyles) {
            if (mas.getStyleName().equals(name)) return mas.getRating();
        }
        for (CraftAbility ca : crafts) {
            if (ca.getExpertise().equals(name)) return ca.getRating();
        }
        return 0;
    }

    public boolean isMartialArtsStyle(String name) {
        if (name == null) return false;
        return martialArtsStyles.stream().anyMatch(s -> s.getStyleName().equals(name));
    }

    public boolean isCraftExpertise(String name) {
        if (name == null) return false;
        return crafts.stream().anyMatch(c -> c.getExpertise().equals(name));
    }

    public IntegerProperty getAbilityProperty(String name) {
        if (name == null) return null;
        if (abilities.containsKey(name)) return abilities.get(name);
        for (MartialArtsStyle mas : martialArtsStyles) {
            if (mas.getStyleName().equals(name)) return mas.ratingProperty();
        }
        for (CraftAbility ca : crafts) {
            if (ca.getExpertise().equals(name)) return ca.ratingProperty();
        }
        return null;
    }

    public ObservableList<PurchasedCharm> getUnlockedCharms() { return unlockedCharms; }
    public ObservableList<Merit> getMerits() { return merits; }
    public ObservableList<Specialty> getSpecialties() { return specialties; }
    
    public Specialty getSpecialtyById(String id) {
        if (id == null || id.isEmpty()) return null;
        return specialties.stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null);
    }
    public ObservableList<CraftAbility> getCrafts() { return crafts; }
    public ObservableList<MartialArtsStyle> getMartialArtsStyles() { return martialArtsStyles; }
    public ObservableList<Weapon> getWeapons() { return weapons; }
    public ObservableList<Armor> getArmors() { return armors; }
    public ObservableList<Hearthstone> getHearthstones() { return hearthstones; }
    public ObservableList<OtherEquipment> getOtherEquipment() { return otherEquipment; }
    public ObservableList<Intimacy> getIntimacies() { return intimacies; }
    public ObservableList<ShapingRitual> getShapingRituals() { return shapingRituals; }
    public ObservableList<Spell> getSpells() { return spells; }
    
    public IntegerProperty naturalSoakProperty() { return naturalSoak; }
    public IntegerProperty armorSoakProperty() { return armorSoak; }
    public IntegerProperty totalSoakProperty() { return totalSoak; }
    public IntegerProperty totalHardnessProperty() { return totalHardness; }
    
    public void updateCombatStats() {
        int stamina = attributes.get("Stamina").get();
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
    
    public boolean isArtifactPossessed(String artifactId) {
        if (artifactId == null || artifactId.isEmpty()) return false;
        for (Weapon w : weapons) if (w.getId().equals(artifactId) && w.getType() == Weapon.WeaponType.ARTIFACT) return true;
        for (Armor a : armors) if (a.getId().equals(artifactId) && a.getType() == Armor.ArmorType.ARTIFACT) return true;
        for (OtherEquipment oe : otherEquipment) if (oe.getId().equals(artifactId) && oe.isArtifact()) return true;
        return false;
    }

    public boolean hasCharm(String id) {
        if (id == null) return false;
        return unlockedCharms.stream().anyMatch(c -> c.id().equals(id));
    }
    
    public int getCharmCount(String id) {
        return (int) unlockedCharms.stream().filter(c -> c.id().equals(id) || c.name().equals(id)).count();
    }
    
    public boolean hasCharmByName(String name) {
        if (name == null) return false;
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

    public boolean hasExcellency(String ability) {
        if (casteAbilities.containsKey(ability)) {
            boolean isFavored = casteAbilities.get(ability).get() || favoredAbilities.get(ability).get();
            if (isFavored && abilities.get(ability).get() > 0) return true;
        }
        return unlockedCharms.stream().anyMatch(c -> c.ability() != null && c.ability().equals(ability));
    }
    
    // Moved to Rules Engine
/*
    public int getPersonalMotes() { return (essence.get() * 3) + 10; }
    public int getPeripheralMotes() { return (essence.get() * 7) + 26; }
    
    public int getAttributeTotal(List<String> attrCategory) {
        return attrCategory.stream().mapToInt(attr -> attributes.get(attr).get() - 1).sum();
    }
    
    public int getAbilityTotal() {
        int total = ABILITIES.stream().filter(abil -> !"Craft".equals(abil) && !"Martial Arts".equals(abil))
                .mapToInt(ability -> Math.min(3, abilities.get(ability).get())).sum();
        for (CraftAbility ca : crafts) total += Math.min(3, ca.getRating());
        for (MartialArtsStyle mas : martialArtsStyles) total += Math.min(3, mas.getRating());
        return total;
    }
    
    public int getMeritTotal() {
        int total = 0;
        for (Merit m : merits) total += m.getRating();
        return total;
    }

    public int getTotalCharmPoolUsage() {
        boolean hasTCS = hasCharmByName(TERRESTRIAL_CIRCLE_SORCERY);
        int billableSpells = Math.max(0, spells.size() - (hasTCS ? 1 : 0));
        return unlockedCharms.size() + billableSpells;
    }
    
    public int getBonusPointsSpent() {
        ...
    }

    public List<String> getHealthLevels() {
        ...
    }
*/
    
    public CharacterSaveState exportState() {
        CharacterSaveState state = new CharacterSaveState();
        state.name = this.name.get();
        state.mode = this.mode.get() != null ? this.mode.get().name() : CharacterMode.CREATION.name();
        state.caste = caste.get() != null ? caste.get().name() : Caste.NONE.name();
        state.supernalAbility = supernalAbility.get();
        state.essence = essence.get();
        state.willpower = willpower.get();
        state.limitTrigger = limitTrigger.get();
        state.limit = limit.get();
        state.attributes = new HashMap<>();
        for (String attr : SystemData.ATTRIBUTES) state.attributes.put(attr, attributes.get(attr).get());
        state.abilities = new HashMap<>();
        for (String abil : SystemData.ABILITIES) state.abilities.put(abil, abilities.get(abil).get());
        state.casteAbilities = new ArrayList<>();
        for (String abil : SystemData.ABILITIES) if (casteAbilities.get(abil).get()) state.casteAbilities.add(abil);
        state.favoredAbilities = new ArrayList<>();
        for (String abil : SystemData.ABILITIES) if (favoredAbilities.get(abil).get()) state.favoredAbilities.add(abil);
        state.unlockedCharms = new ArrayList<>(unlockedCharms);
        state.merits = new HashMap<>();
        for (Merit m : merits) state.merits.put(m.getName(), m.getRating());
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
            cd.expertise = ca.getExpertise(); cd.rating = ca.getRating();
            cd.isCaste = ca.isCaste(); cd.isFavored = ca.isFavored();
            state.crafts.add(cd);
        }
        state.martialArts = new ArrayList<>();
        for (MartialArtsStyle mas : martialArtsStyles) {
            CharacterSaveState.MartialArtsData md = new CharacterSaveState.MartialArtsData();
            md.id = mas.getId(); md.styleName = mas.getStyleName(); md.rating = mas.getRating();
            md.isCaste = mas.isCaste(); md.isFavored = mas.isFavored();
            state.martialArts.add(md);
        }
        state.weapons = new ArrayList<>();
        for (Weapon w : weapons) {
            CharacterSaveState.WeaponData wd = new CharacterSaveState.WeaponData();
            wd.id = w.getId();
            wd.name = w.getName();
            wd.range = w.getRange();
            wd.type = w.getType();
            wd.category = w.getCategory();
            wd.tags = new ArrayList<>(w.getTags());
            wd.specialtyId = w.getSpecialtyId();
            wd.accuracy = w.getAccuracy(); wd.damage = w.getDamage();
            wd.defense = w.getDefense(); wd.overwhelming = w.getOverwhelming();
            wd.attunement = w.getAttunement();
            wd.closeRangeBonus = w.getCloseRangeBonus(); wd.shortRangeBonus = w.getShortRangeBonus();
            wd.mediumRangeBonus = w.getMediumRangeBonus(); wd.longRangeBonus = w.getLongRangeBonus();
            state.weapons.add(wd);
        }
        state.armors = new ArrayList<>();
        for (Armor a : armors) {
            CharacterSaveState.ArmorData ad = new CharacterSaveState.ArmorData();
            ad.id = a.getId(); ad.name = a.getName();
            ad.type = a.getType(); ad.weight = a.getWeight();
            ad.tags = new ArrayList<>(a.getTags());
            ad.equipped = a.isEquipped();
            ad.soak = a.getSoak(); ad.hardness = a.getHardness();
            ad.mobilityPenalty = a.getMobilityPenalty(); ad.attunement = a.getAttunement();
            state.armors.add(ad);
        }
        state.hearthstones = new ArrayList<>();
        for (Hearthstone h : hearthstones) {
            CharacterSaveState.HearthstoneData hd = new CharacterSaveState.HearthstoneData();
            hd.id = h.getId(); hd.name = h.getName(); hd.description = h.getDescription();
            state.hearthstones.add(hd);
        }
        state.otherEquipment = new ArrayList<>();
        for (OtherEquipment o : otherEquipment) {
            CharacterSaveState.OtherEquipmentData od = new CharacterSaveState.OtherEquipmentData();
            od.id = o.getId(); od.name = o.getName(); od.description = o.getDescription();
            od.isArtifact = o.isArtifact();
            state.otherEquipment.add(od);
        }
        state.intimacies = new ArrayList<>();
        for (Intimacy i : intimacies) {
            CharacterSaveState.IntimacyData idata = new CharacterSaveState.IntimacyData();
            idata.id = i.getId(); idata.name = i.getName();
            idata.type = i.getType(); idata.intensity = i.getIntensity();
            idata.description = i.getDescription();
            state.intimacies.add(idata);
        }
        state.shapingRituals = new ArrayList<>();
        for (ShapingRitual r : shapingRituals) {
            CharacterSaveState.ShapingRitualData rd = new CharacterSaveState.ShapingRitualData();
            rd.id = r.getId(); rd.name = r.getName(); rd.description = r.getDescription();
            state.shapingRituals.add(rd);
        }
        state.spells = new ArrayList<>();
        for (Spell s : spells) {
            CharacterSaveState.SpellData sd = new CharacterSaveState.SpellData();
            sd.id = s.getId(); sd.name = s.getName(); sd.circle = s.getCircle();
            sd.cost = s.getCost(); sd.keywords = new ArrayList<>(s.getKeywords());
            sd.duration = s.getDuration(); sd.description = s.getDescription();
            state.spells.add(sd);
        }
        return state;
    }

    public void importState(CharacterSaveState state) {
        if (state == null) return;
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
            supernalAbility.set(state.supernalAbility != null ? state.supernalAbility : "");
            essence.set(state.essence);
            willpower.set(state.willpower);
            limitTrigger.set(state.limitTrigger != null ? state.limitTrigger : "");
            limit.set(state.limit);
            if (state.attributes != null) {
                for (String attr : SystemData.ATTRIBUTES) if (state.attributes.containsKey(attr)) attributes.get(attr).set(state.attributes.get(attr));
            }
            if (state.abilities != null) {
                for (String abil : SystemData.ABILITIES) if (state.abilities.containsKey(abil)) abilities.get(abil).set(state.abilities.get(abil));
            }
            for (String abil : SystemData.ABILITIES) {
                casteAbilities.get(abil).set(state.casteAbilities != null && state.casteAbilities.contains(abil));
                favoredAbilities.get(abil).set(state.favoredAbilities != null && state.favoredAbilities.contains(abil));
            }
            if (state.unlockedCharms != null) {
                List<PurchasedCharm> migrated = new ArrayList<>();
                for (PurchasedCharm pc : state.unlockedCharms) {
                    String id = pc.id();
                    if (id == null || id.isEmpty()) {
                        id = java.util.UUID.nameUUIDFromBytes((pc.name().trim() + "|" + pc.ability().trim()).getBytes()).toString();
                    }
                    migrated.add(new PurchasedCharm(id, pc.name(), pc.ability()));
                }
                unlockedCharms.setAll(migrated);
            } else unlockedCharms.clear();
            
            merits.clear();
            if (state.merits != null) for (Map.Entry<String, Integer> e : state.merits.entrySet()) merits.add(new Merit(e.getKey(), e.getValue()));
            if (merits.isEmpty()) merits.add(new Merit("", 1));
            specialties.clear();
            if (state.specialties != null) {
                for (CharacterSaveState.SpecialtyData sd : state.specialties) {
                    specialties.add(new Specialty(sd.id, sd.name, sd.ability));
                }
            }
            if (specialties.isEmpty()) specialties.add(new Specialty("", ""));
            crafts.clear();
            if (state.crafts != null) {
                for (CharacterSaveState.CraftData cd : state.crafts) {
                    CraftAbility ca = new CraftAbility(cd.expertise, cd.rating);
                    ca.setCaste(cd.isCaste); ca.setFavored(cd.isFavored);
                    crafts.add(ca);
                }
            }
            martialArtsStyles.clear();
            if (state.martialArts != null) {
                for (CharacterSaveState.MartialArtsData md : state.martialArts) {
                    String id = md.id;
                    if (id == null || id.isEmpty()) id = java.util.UUID.randomUUID().toString();
                    MartialArtsStyle mas = new MartialArtsStyle(id, md.styleName, md.rating);
                    mas.setCaste(md.isCaste); mas.setFavored(md.isFavored);
                    martialArtsStyles.add(mas);
                }
            }
            for (MartialArtsStyle mas : martialArtsStyles) {
                mas.setCaste(getCasteAbility("Brawl").get());
                mas.setFavored(getFavoredAbility("Brawl").get());
            }
            weapons.clear();
            if (state.weapons != null) {
                for (CharacterSaveState.WeaponData wd : state.weapons) {
                    String id = wd.id;
                    if (id == null || id.isEmpty()) id = java.util.UUID.randomUUID().toString();
                    Weapon w = new Weapon(id, wd.name, wd.range, wd.type, wd.category);
                    if (wd.tags != null) w.getTags().addAll(wd.tags);
                    w.setSpecialtyId(wd.specialtyId);
                    weapons.add(w);
                }
            }
            armors.clear();
            if (state.armors != null) {
                for (CharacterSaveState.ArmorData ad : state.armors) {
                    String id = ad.id;
                    if (id == null || id.isEmpty()) id = java.util.UUID.randomUUID().toString();
                    Armor a = new Armor(id, ad.name, ad.type, ad.weight);
                    if (ad.tags != null) a.getTags().setAll(ad.tags);
                    a.setEquipped(ad.equipped);
                    armors.add(a);
                }
            }
            hearthstones.clear();
            if (state.hearthstones != null) {
                for (CharacterSaveState.HearthstoneData hd : state.hearthstones) {
                    hearthstones.add(new Hearthstone(hd.id, hd.name, hd.description));
                }
            }
            otherEquipment.clear();
            if (state.otherEquipment != null) {
                for (CharacterSaveState.OtherEquipmentData od : state.otherEquipment) {
                    OtherEquipment oe = new OtherEquipment(od.id, od.name, od.description);
                    oe.setArtifact(od.isArtifact);
                    otherEquipment.add(oe);
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
            dirty.set(false);
        } finally { isImporting = false; }
    }
}
