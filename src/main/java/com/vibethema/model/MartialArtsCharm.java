package com.vibethema.model;

import java.util.List;

public class MartialArtsCharm extends Charm {
    private transient String styleName;
    private int minAbility;

    public MartialArtsCharm() {
        super();
        setCategory("martialArts");
    }

    @Override
    public void setAbility(String ability) { this.styleName = ability; }
    @Override
    public String getAbility() { return styleName; }
    @Override
    public void setMinAbility(int minAbility) { this.minAbility = minAbility; }
    @Override
    public int getMinAbility() { return minAbility; }

    @Override
    public boolean isEligible(CharacterData data) {
        if (data == null) return false;
        
        int effectiveEssence = data.essenceProperty().get();
        String supernal = data.supernalAbilityProperty().get();
        
        // Supernal Martial Arts covers all styles
        if ("Martial Arts".equals(supernal)) {
            effectiveEssence = 5;
        }
        
        if (effectiveEssence < getMinEssence()) return false;
        if (data.getAbilityRatingByName(styleName) < minAbility) return false;
        
        List<String> prereqs = getPrerequisites();
        if (prereqs != null) {
            for (String reqId : prereqs) {
                if (!data.hasCharm(reqId)) return false;
            }
        }
        return true;
    }
}
