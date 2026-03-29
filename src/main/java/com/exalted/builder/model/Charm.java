package com.exalted.builder.model;

import java.util.List;

public class Charm {
    private String name;
    private String ability;
    private int minAbility;
    private int minEssence;
    private List<String> prerequisites;
    
    private String cost;
    private String type;
    private String keywords;
    private String duration;
    private String fullText;

    public Charm() {}

    public String getName() { return name; }
    public String getAbility() { return ability; }
    public int getMinAbility() { return minAbility; }
    public int getMinEssence() { return minEssence; }
    public List<String> getPrerequisites() { return prerequisites; }
    
    public String getCost() { return cost; }
    public String getType() { return type; }
    public String getKeywords() { return keywords; }
    public String getDuration() { return duration; }
    public String getFullText() { return fullText; }

    public boolean isEligible(CharacterData data) {
        if (data.essenceProperty().get() < minEssence) return false;
        if (data.getAbility(ability).get() < minAbility) return false;
        for (String req : prerequisites) {
            if (!data.getUnlockedCharms().contains(req)) return false;
        }
        return true;
    }
}
