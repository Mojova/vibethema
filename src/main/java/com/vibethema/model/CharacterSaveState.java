package com.vibethema.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CharacterSaveState {
    public String name;
    public CharacterData.Caste caste;
    public String supernalAbility;
    public int essence, willpower;
    public Map<String, Integer> attributes;
    public Map<String, Integer> abilities;
    public List<String> casteAbilities;
    public List<String> favoredAbilities;
    public List<PurchasedCharm> unlockedCharms;
    public Map<String, Integer> merits = new HashMap<>(); // name -> rating
    public List<SpecialtyData> specialties = new ArrayList<>();
    public List<CraftData> crafts = new ArrayList<>();
    public List<MartialArtsData> martialArts = new ArrayList<>();
    
    public static class SpecialtyData {
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
}
