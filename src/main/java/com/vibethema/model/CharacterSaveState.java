package com.vibethema.model;

import java.util.List;
import java.util.Map;

public class CharacterSaveState {
    public String name;
    public String caste;
    public String supernalAbility;
    public int willpower;
    public int essence;
    public Map<String, Integer> attributes;
    public Map<String, Integer> abilities;
    public List<String> casteAbilities;
    public List<String> favoredAbilities;
    public List<PurchasedCharm> unlockedCharms;
}
