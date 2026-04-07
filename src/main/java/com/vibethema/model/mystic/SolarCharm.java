package com.vibethema.model.mystic;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.social.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.logic.*;

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
            boolean isSupernal = ability.equalsIgnoreCase(supernal);
            // Supernal Martial Arts covers all styles
            if (!isSupernal && "Martial Arts".equalsIgnoreCase(supernal) && data.isMartialArtsStyle(ability)) {
                isSupernal = true;
            }
            // Supernal Craft covers all expertise types
            if (!isSupernal && "Craft".equalsIgnoreCase(supernal) && data.isCraftExpertise(ability)) {
                isSupernal = true;
            }
            
            // Exception: Sorcery Circle Charms always require Essence minimums
            if (isSupernal && ("Celestial Circle Sorcery".equalsIgnoreCase(getName()) || "Solar Circle Sorcery".equalsIgnoreCase(getName()))) {
                isSupernal = false;
            }
            
            if (isSupernal) {
                effectiveEssence = 5;
            }
        }
        
        if (effectiveEssence < getMinEssence()) return false;
        if (data.getAbilityRatingByName(ability) < minAbility) return false;
        
        List<PrerequisiteGroup> groups = getPrerequisiteGroups();
        if (groups != null) {
            for (PrerequisiteGroup group : groups) {
                int metCount = 0;
                for (String id : group.getCharmIds()) {
                    metCount += data.getCharmCount(id);
                }
                
                int required = group.getMinCount() > 0 ? group.getMinCount() : group.getCharmIds().size();
                if (metCount < required) return false;
            }
        }
        return true;
    }
}