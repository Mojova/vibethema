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
        DAWN, ZENITH, TWILIGHT, NIGHT, ECLIPSE, NONE
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
    
    private ObservableList<PurchasedCharm> unlockedCharms = FXCollections.observableArrayList();
    
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
            attributes.put(attr, new SimpleIntegerProperty(1)); // Attributes start at 1
        }
        for (String ability : ABILITIES) {
            abilities.put(ability, new SimpleIntegerProperty(0)); // Abilities start at 0
            casteAbilities.put(ability, new SimpleBooleanProperty(false));
            favoredAbilities.put(ability, new SimpleBooleanProperty(false));
        }
    }

    public StringProperty nameProperty() { return name; }
    public ObjectProperty<Caste> casteProperty() { return caste; }
    public StringProperty supernalAbilityProperty() { return supernalAbility; }
    public IntegerProperty getAttribute(String name) { return attributes.get(name); }
    public IntegerProperty getAbility(String name) { return abilities.get(name); }
    public BooleanProperty getCasteAbility(String name) { return casteAbilities.get(name); }
    public BooleanProperty getFavoredAbility(String name) { return favoredAbilities.get(name); }
    
    public IntegerProperty essenceProperty() { return essence; }
    public IntegerProperty willpowerProperty() { return willpower; }
    public ObservableList<PurchasedCharm> getUnlockedCharms() { return unlockedCharms; }
    
    public boolean hasCharm(String name) {
        return unlockedCharms.stream().anyMatch(c -> c.name().equals(name));
    }
    
    public void addCharm(PurchasedCharm charm) {
        if (!hasCharm(charm.name())) {
            unlockedCharms.add(charm);
        }
    }
    
    public void removeCharm(String name) {
        unlockedCharms.removeIf(c -> c.name().equals(name));
    }

    public boolean hasExcellency(String ability) {
        if (casteAbilities.containsKey(ability) && favoredAbilities.containsKey(ability)) {
            boolean isFavored = casteAbilities.get(ability).get() || favoredAbilities.get(ability).get();
            if (isFavored && abilities.get(ability).get() > 0) return true;
        }
        return unlockedCharms.stream().anyMatch(c -> c.ability() != null && c.ability().equals(ability));
    }
    
    public int getPersonalMotes() {
        return (essence.get() * 3) + 10;
    }
    
    public int getPeripheralMotes() {
        return (essence.get() * 7) + 26;
    }
    
    public int getAttributeTotal(List<String> attrCategory) {
        return attrCategory.stream()
                .mapToInt(attr -> attributes.get(attr).get() - 1)
                .sum();
    }
    
    public int getAbilityTotal() {
        return ABILITIES.stream()
                .mapToInt(ability -> abilities.get(ability).get())
                .sum();
    }
    
    public int getBonusPointsSpent() {
        int bp = 0;
        
        // Attributes optimal spending
        int phys = getAttributeTotal(PHYSICAL_ATTRIBUTES);
        int soc = getAttributeTotal(SOCIAL_ATTRIBUTES);
        int ment = getAttributeTotal(MENTAL_ATTRIBUTES);
        
        int minAttrBP = Integer.MAX_VALUE;
        int[][] permutations = {
            {8, 6, 4}, {8, 4, 6}, {6, 8, 4}, {6, 4, 8}, {4, 8, 6}, {4, 6, 8}
        };
        for (int[] p : permutations) {
            int cost = 0;
            cost += Math.max(0, phys - p[0]) * (p[0] == 4 ? 3 : 4);
            cost += Math.max(0, soc - p[1]) * (p[1] == 4 ? 3 : 4);
            cost += Math.max(0, ment - p[2]) * (p[2] == 4 ? 3 : 4);
            if (cost < minAttrBP) minAttrBP = cost;
        }
        bp += minAttrBP;
        
        // Abilities optimal spending
        int mustBp = 0;
        List<Integer> poolDots = new ArrayList<>();
        
        for (String abil : ABILITIES) {
            int dots = abilities.get(abil).get();
            boolean isFavored = casteAbilities.get(abil).get() || favoredAbilities.get(abil).get();
            int costPerDot = isFavored ? 1 : 2;
            
            for (int i = 1; i <= dots; i++) {
                if (i > 3) { // dots 4 and 5 must cost bonus points
                    mustBp += costPerDot;
                } else {
                    poolDots.add(costPerDot); // eligible for 28 free dots pool
                }
            }
        }
        
        // Try to spend 28 free dots on the most expensive ones
        Collections.sort(poolDots, Collections.reverseOrder());
        int freeLeft = 28;
        int optPoolBp = 0;
        for (int cost : poolDots) {
            if (freeLeft > 0) {
                freeLeft--;
            } else {
                optPoolBp += cost;
            }
        }
        
        bp += mustBp + optPoolBp;
        
        // Willpower
        if (willpower.get() > 5) {
            bp += (willpower.get() - 5) * 2;
        }
        // Charms optimal spending
        List<Integer> charmCosts = new ArrayList<>();
        for (PurchasedCharm pc : unlockedCharms) {
            String ability = pc.ability();
            boolean isFavored = false;
            if (casteAbilities.containsKey(ability) && favoredAbilities.containsKey(ability)) {
                isFavored = casteAbilities.get(ability).get() || favoredAbilities.get(ability).get();
            }
            charmCosts.add(isFavored ? 4 : 5);
        }
        
        Collections.sort(charmCosts, Collections.reverseOrder());
        int freeCharmsLeft = 15;
        for (int cost : charmCosts) {
            if (freeCharmsLeft > 0) {
                freeCharmsLeft--;
            } else {
                bp += cost;
            }
        }
        
        
        return bp;
    }
    
    public CharacterSaveState exportState() {
        CharacterSaveState state = new CharacterSaveState();
        state.name = this.name.get();
        state.caste = this.caste.get() != null ? this.caste.get().name() : "NONE";
        state.supernalAbility = this.supernalAbility.get();
        state.willpower = this.willpower.get();
        state.essence = this.essence.get();
        
        state.attributes = new HashMap<>();
        for (String attr : ATTRIBUTES) state.attributes.put(attr, attributes.get(attr).get());
        
        state.abilities = new HashMap<>();
        state.casteAbilities = new ArrayList<>();
        state.favoredAbilities = new ArrayList<>();
        
        for (String abil : ABILITIES) {
            state.abilities.put(abil, abilities.get(abil).get());
            if (casteAbilities.get(abil).get()) state.casteAbilities.add(abil);
            if (favoredAbilities.get(abil).get()) state.favoredAbilities.add(abil);
        }
        
        state.unlockedCharms = new ArrayList<>(unlockedCharms);
        return state;
    }

    public void importState(CharacterSaveState state) {
        if (state == null) return;
        this.name.set(state.name != null ? state.name : "");
        try {
            this.caste.set(Caste.valueOf(state.caste != null ? state.caste : "NONE"));
        } catch (Exception e) {
            this.caste.set(Caste.NONE);
        }
        
        this.willpower.set(state.willpower > 0 ? state.willpower : 5);
        this.essence.set(state.essence > 0 ? state.essence : 1);
        
        if (state.attributes != null) {
            for (String attr : ATTRIBUTES) {
                attributes.get(attr).set(state.attributes.getOrDefault(attr, 1));
            }
        }
        
        if (state.abilities != null) {
            for (String abil : ABILITIES) {
                abilities.get(abil).set(state.abilities.getOrDefault(abil, 0));
                casteAbilities.get(abil).set(state.casteAbilities != null && state.casteAbilities.contains(abil));
                favoredAbilities.get(abil).set(state.favoredAbilities != null && state.favoredAbilities.contains(abil));
            }
        }
        
        this.supernalAbility.set(state.supernalAbility != null ? state.supernalAbility : "");

        unlockedCharms.clear();
        if (state.unlockedCharms != null) {
            unlockedCharms.addAll(state.unlockedCharms);
        }
    }
}
