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
    public Map<String, String> attributePriorities;
    
    public Map<String, Integer> abilities;
    public List<String> casteAbilities;
    public List<String> favoredAbilities;
    public List<PurchasedCharm> unlockedCharms;
    public Map<String, Integer> merits = new HashMap<>(); // name -> rating
    public List<SpecialtyData> specialties = new ArrayList<>();
    public List<CraftData> crafts = new ArrayList<>();
    public List<MartialArtsData> martialArts = new ArrayList<>();
    public List<WeaponLink> weapons = new ArrayList<>();
    public List<ArmorLink> armors = new ArrayList<>();
    public List<HearthstoneLink> hearthstones = new ArrayList<>();
    public List<OtherEquipmentLink> otherEquipment = new ArrayList<>();
    public List<IntimacyData> intimacies = new ArrayList<>();
    
    public List<ShapingRitualData> shapingRituals = new ArrayList<>();
    
    public List<SpellData> spells = new ArrayList<>();

    public CharacterSaveState creationSnapshot;
    public List<XpAwardData> xpAwards = new ArrayList<>();
        
    public static class WeaponLink {
        public String id;
        public String instanceId;
        public String specialtyId;
        public boolean equipped;
    }

    public static class ArmorLink {
        public String id;
        public String instanceId;
        public boolean equipped;
    }

    public static class HearthstoneLink {
        public String id;
        public String instanceId;
        public boolean equipped;
    }

    public static class OtherEquipmentLink {
        public String id;
        public String instanceId;
        public boolean equipped;
    }

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

    public static class XpAwardData {
        public String id;
        public String description;
        public int amount;
        public boolean isSolar;
    }
}

