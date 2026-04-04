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
import javafx.collections.ObservableMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CharacterData {
    public enum Caste {
        DAWN, ZENITH, TWILIGHT, NIGHT, ECLIPSE, NONE;

        @Override
        public String toString() {
            if (this == NONE) return "None";
            String s = super.toString();
            return s.substring(0, 1) + s.substring(1).toLowerCase();
        }
    }

    private StringProperty name = new SimpleStringProperty("");
    private ObjectProperty<Caste> caste = new SimpleObjectProperty<>(Caste.NONE);
    private StringProperty supernalAbility = new SimpleStringProperty("");

    private ObservableMap<String, IntegerProperty> attributes = FXCollections.observableHashMap();
    private ObservableMap<String, IntegerProperty> abilities = FXCollections.observableHashMap();
    private ObservableMap<String, BooleanProperty> casteAbilities = FXCollections.observableHashMap();
    private ObservableMap<String, BooleanProperty> favoredAbilities = FXCollections.observableHashMap();
    
    private IntegerProperty essence = new SimpleIntegerProperty(1);
    private IntegerProperty willpower = new SimpleIntegerProperty(5);
    
    private final ObservableList<PurchasedCharm> unlockedCharms = FXCollections.observableArrayList();
    private final ObservableList<Merit> merits = FXCollections.observableArrayList();
    private final ObservableList<Specialty> specialties = FXCollections.observableArrayList();
    private final ObservableList<CraftAbility> crafts = FXCollections.observableArrayList();
    private final ObservableList<MartialArtsStyle> martialArtsStyles = FXCollections.observableArrayList();
    private final ObservableList<Weapon> weapons = FXCollections.observableArrayList();

    private BooleanProperty dirty = new SimpleBooleanProperty(false);
    private boolean isImporting = false;

    public static final List<String> PHYSICAL_ATTRIBUTES = Arrays.asList("Strength", "Dexterity", "Stamina");
    public static final List<String> SOCIAL_ATTRIBUTES = Arrays.asList("Charisma", "Manipulation", "Appearance");
    public static final List<String> MENTAL_ATTRIBUTES = Arrays.asList("Perception", "Intelligence", "Wits");

    public static final List<String> ATTRIBUTES = Arrays.asList(
        "Strength", "Dexterity", "Stamina",
        "Charisma", "Manipulation", "Appearance",
        "Perception", "Intelligence", "Wits"
    );

    public static final List<String> ABILITIES = Arrays.asList(
        "Archery", "Athletics", "Awareness", "Brawl", "Bureaucracy",
        "Craft", "Dodge", "Integrity", "Investigation", "Larceny",
        "Linguistics", "Lore", "Martial Arts", "Medicine", "Melee",
        "Occult", "Performance", "Presence", "Resistance", "Ride",
        "Sail", "Socialize", "Stealth", "Survival", "Thrown", "War"
    );

    public static final Map<Caste, List<String>> CASTE_OPTIONS = new HashMap<>();
    static {
        CASTE_OPTIONS.put(Caste.DAWN, Arrays.asList("Archery", "Awareness", "Brawl", "Dodge", "Melee", "Resistance", "Thrown", "War"));
        CASTE_OPTIONS.put(Caste.ZENITH, Arrays.asList("Athletics", "Integrity", "Performance", "Lore", "Presence", "Resistance", "Survival", "War"));
        CASTE_OPTIONS.put(Caste.TWILIGHT, Arrays.asList("Bureaucracy", "Craft", "Integrity", "Investigation", "Linguistics", "Lore", "Medicine", "Occult"));
        CASTE_OPTIONS.put(Caste.NIGHT, Arrays.asList("Athletics", "Awareness", "Dodge", "Investigation", "Larceny", "Ride", "Stealth", "Socialize"));
        CASTE_OPTIONS.put(Caste.ECLIPSE, Arrays.asList("Bureaucracy", "Larceny", "Linguistics", "Occult", "Presence", "Ride", "Sail", "Socialize"));
        CASTE_OPTIONS.put(Caste.NONE, new ArrayList<>());
    }

    public CharacterData() {
        for (String attr : ATTRIBUTES) {
            attributes.put(attr, new SimpleIntegerProperty(1));
            attributes.get(attr).addListener((obs, oldV, newV) -> markDirty());
        }
        for (String ability : ABILITIES) {
            abilities.put(ability, new SimpleIntegerProperty(0));
            abilities.get(ability).addListener((obs, oldV, newV) -> markDirty());
            casteAbilities.put(ability, new SimpleBooleanProperty(false));
            casteAbilities.get(ability).addListener((obs, oldV, newV) -> markDirty());
            favoredAbilities.put(ability, new SimpleBooleanProperty(false));
            favoredAbilities.get(ability).addListener((obs, oldV, newV) -> markDirty());
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

        merits.addListener((javafx.collections.ListChangeListener.Change<? extends Merit> c) -> markDirty());
        specialties.addListener((javafx.collections.ListChangeListener<? super Specialty>) c -> markDirty());
        crafts.addListener((javafx.collections.ListChangeListener<? super CraftAbility>) c -> markDirty());
        martialArtsStyles.addListener((javafx.collections.ListChangeListener<? super MartialArtsStyle>) c -> markDirty());
        weapons.addListener((javafx.collections.ListChangeListener<? super Weapon>) c -> markDirty());
        
        merits.add(new Merit("", 1));
        specialties.add(new Specialty("", ""));
        dirty.set(false);
    }

    private void markDirty() {
        if (!isImporting) dirty.set(true);
    }

    public BooleanProperty dirtyProperty() { return dirty; }
    public boolean isDirty() { return dirty.get(); }
    public void setDirty(boolean dirty) { this.dirty.set(dirty); }

    public StringProperty nameProperty() { return name; }
    public ObjectProperty<Caste> casteProperty() { return caste; }
    public StringProperty supernalAbilityProperty() { return supernalAbility; }
    public IntegerProperty getAttribute(String name) { return attributes.get(name); }
    public IntegerProperty getAbility(String name) { return abilities.get(name); }
    public BooleanProperty getCasteAbility(String name) { return casteAbilities.get(name); }
    public BooleanProperty getFavoredAbility(String name) { return favoredAbilities.get(name); }
    
    public IntegerProperty essenceProperty() { return essence; }
    public IntegerProperty willpowerProperty() { return willpower; }

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
    public ObservableList<CraftAbility> getCrafts() { return crafts; }
    public ObservableList<MartialArtsStyle> getMartialArtsStyles() { return martialArtsStyles; }
    public ObservableList<Weapon> getWeapons() { return weapons; }
    
    public boolean hasCharm(String id) {
        if (id == null) return false;
        return unlockedCharms.stream().anyMatch(c -> c.id().equals(id));
    }
    
    public int getCharmCount(String id) {
        return (int) unlockedCharms.stream().filter(c -> c.id().equals(id) || c.name().equals(id)).count();
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
    
    public int getPersonalMotes() { return (essence.get() * 3) + 10; }
    public int getPeripheralMotes() { return (essence.get() * 7) + 26; }
    
    public int getAttributeTotal(List<String> attrCategory) {
        return attrCategory.stream().mapToInt(attr -> attributes.get(attr).get() - 1).sum();
    }
    
    public int getAbilityTotal() {
        int total = ABILITIES.stream().filter(abil -> !"Craft".equals(abil) && !"Martial Arts".equals(abil))
                .mapToInt(ability -> abilities.get(ability).get()).sum();
        for (CraftAbility ca : crafts) total += ca.getRating();
        for (MartialArtsStyle mas : martialArtsStyles) total += mas.getRating();
        return total;
    }
    
    public int getMeritTotal() {
        int total = 0;
        for (Merit m : merits) total += m.getRating();
        return total;
    }
    
    public int getBonusPointsSpent() {
        int bp = 0;
        int phys = getAttributeTotal(PHYSICAL_ATTRIBUTES);
        int soc = getAttributeTotal(SOCIAL_ATTRIBUTES);
        int ment = getAttributeTotal(MENTAL_ATTRIBUTES);
        int minAttrBP = Integer.MAX_VALUE;
        int[][] permutations = {{8, 6, 4}, {8, 4, 6}, {6, 8, 4}, {6, 4, 8}, {4, 8, 6}, {4, 6, 8}};
        for (int[] p : permutations) {
            int cost = 0;
            cost += Math.max(0, phys - p[0]) * (p[0] == 4 ? 3 : 4);
            cost += Math.max(0, soc - p[1]) * (p[1] == 4 ? 3 : 4);
            cost += Math.max(0, ment - p[2]) * (p[2] == 4 ? 3 : 4);
            if (cost < minAttrBP) minAttrBP = cost;
        }
        bp += minAttrBP;
        
        int mustBp = 0;
        List<Integer> poolDots = new ArrayList<>();
        for (String abil : ABILITIES) {
            if ("Craft".equals(abil) || "Martial Arts".equals(abil)) continue;
            int dots = abilities.get(abil).get();
            boolean isFavored = casteAbilities.get(abil).get() || favoredAbilities.get(abil).get();
            int costPerDot = isFavored ? 1 : 2;
            for (int i = 1; i <= dots; i++) {
                if (i > 3) mustBp += costPerDot;
                else poolDots.add(costPerDot);
            }
        }
        for (CraftAbility ca : crafts) {
            int dots = ca.getRating();
            int costPerDot = (ca.isCaste() || ca.isFavored()) ? 1 : 2;
            for (int i = 1; i <= dots; i++) {
                if (i > 3) mustBp += costPerDot;
                else poolDots.add(costPerDot);
            }
        }
        for (MartialArtsStyle mas : martialArtsStyles) {
            int dots = mas.getRating();
            int costPerDot = (mas.isCaste() || mas.isFavored()) ? 1 : 2;
            for (int i = 1; i <= dots; i++) {
                if (i > 3) mustBp += costPerDot;
                else poolDots.add(costPerDot);
            }
        }
        Collections.sort(poolDots, Collections.reverseOrder());
        int freeLeft = 28;
        int optPoolBp = 0;
        for (int cost : poolDots) {
            if (freeLeft > 0) freeLeft--;
            else optPoolBp += cost;
        }
        bp += mustBp + optPoolBp;
        if (willpower.get() > 5) bp += (willpower.get() - 5) * 2;
        
        List<Integer> charmCosts = new ArrayList<>();
        for (PurchasedCharm pc : unlockedCharms) {
            String ability = pc.ability();
            boolean isFavored = false;
            if (casteAbilities.containsKey(ability)) {
                isFavored = casteAbilities.get(ability).get() || favoredAbilities.get(ability).get();
            }
            charmCosts.add(isFavored ? 4 : 5);
        }
        Collections.sort(charmCosts, Collections.reverseOrder());
        int freeCharmsLeft = 15;
        for (int cost : charmCosts) {
            if (freeCharmsLeft > 0) freeCharmsLeft--;
            else bp += cost;
        }
        bp += Math.max(0, getMeritTotal() - 10);
        bp += Math.max(0, specialties.size() - 4);
        return bp;
    }

    public List<String> getHealthLevels() {
        List<String> levels = new ArrayList<>(Arrays.asList("-0", "-1", "-1", "-2", "-2", "-4", "Incap"));
        int stamina = getAttribute("Stamina").get();
        // Use ID for Ox-Body lookup
        String oxBodyId = java.util.UUID.nameUUIDFromBytes(("Ox-Body Technique" + "|" + "Resistance").getBytes()).toString();
        int oxBodyCount = getCharmCount(oxBodyId);
        
        for (int i = 0; i < oxBodyCount; i++) {
            if (stamina >= 5) { levels.add("-0"); levels.add("-1"); levels.add("-2"); }
            else if (stamina >= 3) { levels.add("-1"); levels.add("-2"); levels.add("-2"); }
            else { levels.add("-1"); levels.add("-2"); }
        }
        Collections.sort(levels);
        return levels;
    }
    
    public CharacterSaveState exportState() {
        CharacterSaveState state = new CharacterSaveState();
        state.name = this.name.get();
        state.caste = caste.get();
        state.supernalAbility = supernalAbility.get();
        state.essence = essence.get();
        state.willpower = willpower.get();
        state.attributes = new HashMap<>();
        for (String attr : ATTRIBUTES) state.attributes.put(attr, attributes.get(attr).get());
        state.abilities = new HashMap<>();
        for (String abil : ABILITIES) state.abilities.put(abil, abilities.get(abil).get());
        state.casteAbilities = new ArrayList<>();
        for (String abil : ABILITIES) if (casteAbilities.get(abil).get()) state.casteAbilities.add(abil);
        state.favoredAbilities = new ArrayList<>();
        for (String abil : ABILITIES) if (favoredAbilities.get(abil).get()) state.favoredAbilities.add(abil);
        state.unlockedCharms = new ArrayList<>(unlockedCharms);
        state.merits = new HashMap<>();
        for (Merit m : merits) state.merits.put(m.getName(), m.getRating());
        state.specialties = new ArrayList<>();
        for (Specialty s : specialties) {
            CharacterSaveState.SpecialtyData sd = new CharacterSaveState.SpecialtyData();
            sd.name = s.getName(); sd.ability = s.getAbility();
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
            wd.id = w.getId(); wd.name = w.getName();
            wd.range = w.getRange(); wd.type = w.getType();
            wd.category = w.getCategory();
            wd.tags = new ArrayList<>(w.getTags());
            wd.accuracy = w.getAccuracy(); wd.damage = w.getDamage();
            wd.defense = w.getDefense(); wd.overwhelming = w.getOverwhelming();
            wd.attunement = w.getAttunement();
            wd.closeRangeBonus = w.getCloseRangeBonus(); wd.shortRangeBonus = w.getShortRangeBonus();
            wd.mediumRangeBonus = w.getMediumRangeBonus(); wd.longRangeBonus = w.getLongRangeBonus();
            wd.extremeRangeBonus = w.getExtremeRangeBonus();
            state.weapons.add(wd);
        }
        return state;
    }

    public void importState(CharacterSaveState state) {
        if (state == null) return;
        isImporting = true;
        try {
            this.name.set(state.name != null ? state.name : "");
            caste.set(state.caste != null ? state.caste : Caste.NONE);
            supernalAbility.set(state.supernalAbility != null ? state.supernalAbility : "");
            essence.set(state.essence);
            willpower.set(state.willpower);
            if (state.attributes != null) {
                for (String attr : ATTRIBUTES) if (state.attributes.containsKey(attr)) attributes.get(attr).set(state.attributes.get(attr));
            }
            if (state.abilities != null) {
                for (String abil : ABILITIES) if (state.abilities.containsKey(abil)) abilities.get(abil).set(state.abilities.get(abil));
            }
            for (String abil : ABILITIES) {
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
            if (state.specialties != null) for (CharacterSaveState.SpecialtyData sd : state.specialties) specialties.add(new Specialty(sd.name, sd.ability));
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
                    if (wd.tags != null) w.getTags().setAll(wd.tags);
                    // Stats are automatically calculated by the constructor/listeners, but we can set them explicitly if we allow manual overrides later.
                    // For now, since they are automatic, we just ensure the meta-data (range, type, category) is correct.
                    weapons.add(w);
                }
            }
            dirty.set(false);
        } finally { isImporting = false; }
    }
}
