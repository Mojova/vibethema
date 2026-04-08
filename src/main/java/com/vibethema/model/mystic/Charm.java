package com.vibethema.model.mystic;

import com.vibethema.model.*;
import java.util.List;
import java.util.UUID;

public abstract class Charm {
    private String id;
    private String name;
    private int minEssence;
    private List<PrerequisiteGroup> prerequisiteGroups;

    public static class PrerequisiteGroup {
        private String label;
        private List<String> charmIds;
        private int minCount;

        public PrerequisiteGroup() {}

        public PrerequisiteGroup(String label, List<String> charmIds, int minCount) {
            this.label = label;
            this.charmIds = charmIds;
            this.minCount = minCount;
        }

        public String getLabel() {
            return label;
        }

        public List<String> getCharmIds() {
            return charmIds;
        }

        public int getMinCount() {
            return minCount;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public void setCharmIds(List<String> charmIds) {
            this.charmIds = charmIds;
        }

        public void setMinCount(int minCount) {
            this.minCount = minCount;
        }
    }

    private String cost;
    private String type;
    private List<String> keywords;
    private String duration;
    private String fullText;
    private String rawData;
    private boolean potentiallyProblematicImport;
    private String category; // "solar", "martialArts", "evocation"
    private transient boolean isCustom;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Charm() {
        this.id = UUID.randomUUID().toString();
    }

    public String getName() {
        return name;
    }

    public int getMinEssence() {
        return minEssence;
    }

    public List<PrerequisiteGroup> getPrerequisiteGroups() {
        return prerequisiteGroups;
    }

    public String getCost() {
        return cost;
    }

    public String getType() {
        return type;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public String getDuration() {
        return duration;
    }

    public String getFullText() {
        return fullText;
    }

    public String getRawData() {
        return rawData;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMinEssence(int minEssence) {
        this.minEssence = minEssence;
    }

    public void setPrerequisiteGroups(List<PrerequisiteGroup> prerequisiteGroups) {
        this.prerequisiteGroups = prerequisiteGroups;
    }

    public void setCost(String cost) {
        this.cost = cost;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setFullText(String fullText) {
        this.fullText = fullText;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
    }

    public boolean isCustom() {
        return isCustom;
    }

    public void setCustom(boolean custom) {
        isCustom = custom;
    }

    public boolean isPotentiallyProblematicImport() {
        return potentiallyProblematicImport;
    }

    public void setPotentiallyProblematicImport(boolean value) {
        this.potentiallyProblematicImport = value;
    }

    public abstract boolean isEligible(CharacterData data);

    public abstract String getAbility();

    public abstract void setAbility(String ability);

    public abstract int getMinAbility();

    public abstract void setMinAbility(int minAbility);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Charm charm = (Charm) o;
        return java.util.Objects.equals(id, charm.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }
}
