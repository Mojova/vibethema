package com.vibethema.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CharacterSaveState {
    public String name;
    public String caste;
    public String mode;
    public String supernalAbility;
    public int essence, willpower;
    public String limitTrigger;
    public int limit;
    public Map<String, Integer> attributes;
    public Map<String, Integer> abilities;
    public List<String> casteAbilities;
    public List<String> favoredAbilities;
    public List<PurchasedCharm> unlockedCharms;
    public Map<String, Integer> merits = new HashMap<>(); // name -> rating
    public List<SpecialtyData> specialties = new ArrayList<>();
    public List<CraftData> crafts = new ArrayList<>();
    public List<MartialArtsData> martialArts = new ArrayList<>();
    public List<WeaponData> weapons = new ArrayList<>();
    public List<ArmorData> armors = new ArrayList<>();
    public List<HearthstoneData> hearthstones = new ArrayList<>();
    public List<OtherEquipmentData> otherEquipment = new ArrayList<>();
    public List<IntimacyData> intimacies = new ArrayList<>();
    
    public List<ShapingRitualData> shapingRituals = new ArrayList<>();
    
    public List<SpellData> spells = new ArrayList<>();
    
    public static class SpecialtyData {
        public String id;
        public String name;
        public String ability;
    }

    public static class CraftData {
        public String expertise;
        public int rating;
        public boolean isCaste;
        public boolean isFavored;
    }

    public static class MartialArtsData {
        public String id;
        public String styleName;
        public int rating;
        public boolean isCaste;
        public boolean isFavored;
    }

    public static class WeaponData {
        public String id;
        public String name;
        public Weapon.WeaponRange range;
        public Weapon.WeaponType type;
        public Weapon.WeaponCategory category;
        public List<String> tags = new ArrayList<>();
        public String specialtyId;
        
        public int accuracy, damage, defense, overwhelming, attunement;
        public int closeRangeBonus, shortRangeBonus, mediumRangeBonus, longRangeBonus, extremeRangeBonus;
    }

    public static class ArmorData {
        public String id;
        public String name;
        public Armor.ArmorType type;
        public Armor.ArmorWeight weight;
        public List<String> tags = new ArrayList<>();
        public boolean equipped;
        
        public int soak, hardness, mobilityPenalty, attunement;
    }

    public static class HearthstoneData {
        public String id;
        public String name;
        public String description;
    }

    public static class OtherEquipmentData {
        public String id;
        public String name;
        public String description;
        public boolean isArtifact;
    }

    public static class IntimacyData {
        public String id;
        public String name;
        public Intimacy.Type type;
        public Intimacy.Intensity intensity;
        public String description;
    }

    public static class ShapingRitualData {
        public String id;
        public String name;
        public String description;
    }

    public static class SpellData {
        public String id;
        public String name;
        public String circle;
        public String cost;
        public List<String> keywords;
        public String duration;
        public String description;
    }
}
