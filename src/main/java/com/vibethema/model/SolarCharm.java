package com.vibethema.model;

import java.util.List;

public class SolarCharm extends Charm {
    private transient String ability;
    private int minAbility;

    public SolarCharm() {
        super();
        setCategory("solar");
    }

    @Override
    public void setAbility(String ability) { this.ability = ability; }
    @Override
    public String getAbility() { return ability; }
    @Override
    public void setMinAbility(int minAbility) { this.minAbility = minAbility; }
    @Override
    public int getMinAbility() { return minAbility; }

    @Override
    public boolean isEligible(CharacterData data) {
        if (data == null) return false;
        
        int effectiveEssence = data.essenceProperty().get();
        String supernal = data.supernalAbilityProperty().get();
        
        // Handle Supernal Ability
        if (ability != null && !supernal.isEmpty()) {
            boolean isSupernal = ability.equals(supernal);
            // Supernal Martial Arts covers all styles
            if (!isSupernal && "Martial Arts".equals(supernal) && data.isMartialArtsStyle(ability)) {
                isSupernal = true;
            }
            // Supernal Craft covers all expertise types
            if (!isSupernal && "Craft".equals(supernal) && data.isCraftExpertise(ability)) {
                isSupernal = true;
            }
            
            // Exception: Sorcery Circle Charms always require Essence minimums
            if (isSupernal && ("Celestial Circle Sorcery".equals(getName()) || "Solar Circle Sorcery".equals(getName()))) {
                isSupernal = false;
            }
            
            if (isSupernal) {
                effectiveEssence = 5;
            }
        }
        
        if (effectiveEssence < getMinEssence()) return false;
        if (data.getAbilityRating(ability) < minAbility) return false;
        
        List<String> prereqs = getPrerequisites();
        if (prereqs != null) {
            for (String reqId : prereqs) {
                if (!data.hasCharm(reqId)) return false;
            }
        }
        return true;
    }
}
