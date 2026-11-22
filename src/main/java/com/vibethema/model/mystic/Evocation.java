package com.vibethema.model.mystic;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.vibethema.model.*;

public class Evocation extends Charm {
    private static final Gson GSON = new Gson();
    private transient String artifactId;

    public Evocation() {
        super();
        setFamily(CharmKind.EVOCATION, null);
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public JsonObject toCharacterData() {
        return GSON.toJsonTree(this).getAsJsonObject();
    }

    public static Evocation fromCharacterData(JsonObject json, String instanceId) {
        Evocation evocation = GSON.fromJson(json, Evocation.class);
        evocation.setArtifactId(instanceId);
        evocation.setCustom(true);
        return evocation;
    }

    public Evocation copyForArtifact(String instanceId) {
        return fromCharacterData(toCharacterData(), instanceId);
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

        return getPrerequisites() == null || getPrerequisites().isSatisfied(data);
    }
}
