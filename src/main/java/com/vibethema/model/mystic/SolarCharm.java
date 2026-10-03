package com.vibethema.model.mystic;

import com.vibethema.model.*;

public class SolarCharm extends Charm {
    private transient String ability;
    private int minAbility;

    public SolarCharm() {
        super();
        setCategory("solar");
    }

    @Override
    public void setAbility(String ability) {
        this.ability = ability;
    }

    @Override
    public String getAbility() {
        return ability;
    }

    @Override
    public void setMinAbility(int minAbility) {
        this.minAbility = minAbility;
    }

    @Override
    public int getMinAbility() {
        return minAbility;
    }

    @Override
    public boolean isEligible(CharacterData data) {
        if (data == null) return false;

        int effectiveEssence = data.essenceProperty().get();
        String supernal = data.supernalAbilityProperty().get();

        // Handle Supernal Ability
        if (ability != null && !supernal.isEmpty()) {
            boolean isSupernal = ability.equalsIgnoreCase(supernal);
            // Supernal Martial Arts covers all styles
            if (!isSupernal
                    && "Martial Arts".equalsIgnoreCase(supernal)
                    && data.isMartialArtsStyle(ability)) {
                isSupernal = true;
            }
            // Supernal Craft covers all expertise types
            if (!isSupernal
                    && "Craft".equalsIgnoreCase(supernal)
                    && data.isCraftExpertise(ability)) {
                isSupernal = true;
            }

            // Exception: Sorcery Circle Charms always require Essence minimums
            if (isSupernal
                    && ("Celestial Circle Sorcery".equalsIgnoreCase(getName())
                            || "Solar Circle Sorcery".equalsIgnoreCase(getName()))) {
                isSupernal = false;
            }

            if (isSupernal) {
                effectiveEssence = 5;
            }
        }

        if (effectiveEssence < getMinEssence()) return false;
        if (data.getAbilityRatingByName(ability) < minAbility) return false;

        return getPrerequisites() == null || getPrerequisites().isSatisfied(data);
    }
}
