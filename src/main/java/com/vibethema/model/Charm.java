package com.vibethema.model;

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
    private transient boolean isCustom;

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

    public void setName(String name) { this.name = name; }
    public void setAbility(String ability) { this.ability = ability; }
    public void setMinAbility(int minAbility) { this.minAbility = minAbility; }
    public void setMinEssence(int minEssence) { this.minEssence = minEssence; }
    public void setPrerequisites(List<String> prerequisites) { this.prerequisites = prerequisites; }
    public void setCost(String cost) { this.cost = cost; }
    public void setType(String type) { this.type = type; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public void setDuration(String duration) { this.duration = duration; }
    public void setFullText(String fullText) { this.fullText = fullText; }

    public boolean isCustom() { return isCustom; }
    public void setCustom(boolean custom) { isCustom = custom; }

    public boolean isEligible(CharacterData data) {
        int effectiveEssence = data.essenceProperty().get();
        if (ability != null && ability.equals(data.supernalAbilityProperty().get())) {
            effectiveEssence = 5;
        }
        
        if (effectiveEssence < minEssence) return false;
        if (data.getAbility(ability).get() < minAbility) return false;
        if (prerequisites != null) {
            for (String req : prerequisites) {
                if (!data.hasCharm(req)) return false;
            }
        }
        return true;
    }
}
