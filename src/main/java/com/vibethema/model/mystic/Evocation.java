package com.vibethema.model.mystic;

import com.vibethema.model.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;
import com.vibethema.model.traits.*;
import java.util.List;

public class Evocation extends Charm {
    private transient String artifactId;

    public Evocation() {
        super();
        setCategory("evocation");
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public String getAbility() {
        return artifactId;
    }

    @Override
    public void setAbility(String ability) {
        this.artifactId = ability;
    }

    @Override
    public int getMinAbility() {
        return 0;
    }

    @Override
    public void setMinAbility(int minAbility) {
        /* Ignored for Evocations */
    }

    @Override
    public boolean isEligible(CharacterData data) {
        if (data == null) return false;

        int effectiveEssence = data.essenceProperty().get();
        if (effectiveEssence < getMinEssence()) return false;

        if (!data.isArtifactPossessed(artifactId)) return false;

        List<PrerequisiteGroup> groups = getPrerequisiteGroups();
        if (groups != null) {
            for (PrerequisiteGroup group : groups) {
                long metCount = group.getCharmIds().stream().filter(data::hasCharm).count();

                int required =
                        group.getMinCount() > 0 ? group.getMinCount() : group.getCharmIds().size();
                if (metCount < required) return false;
            }
        }
        return true;
    }
}
