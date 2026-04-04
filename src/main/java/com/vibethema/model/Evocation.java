package com.vibethema.model;

import java.util.List;

public class Evocation extends Charm {
    private transient String artifactId;

    public Evocation() {
        super();
        setCategory("evocation");
    }

    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }
    public String getArtifactId() { return artifactId; }

    @Override
    public String getAbility() { return artifactId; }
    @Override
    public void setAbility(String ability) { this.artifactId = ability; }
    @Override
    public int getMinAbility() { return 0; }
    @Override
    public void setMinAbility(int minAbility) { /* Ignored for Evocations */ }

    @Override
    public boolean isEligible(CharacterData data) {
        if (data == null) return false;
        
        int effectiveEssence = data.essenceProperty().get();
        if (effectiveEssence < getMinEssence()) return false;
        
        if (!data.isArtifactPossessed(artifactId)) return false;
        
        List<String> prereqs = getPrerequisites();
        if (prereqs != null) {
            for (String reqId : prereqs) {
                if (!data.hasCharm(reqId)) return false;
            }
        }
        return true;
    }
}
