package com.vibethema.model.mystic;

import com.vibethema.model.*;
import java.util.List;
import java.util.UUID;

public abstract class Charm {
    private String id;
    private String sourceId;
    private List<String> exaltTypes;
    private String name;
    private int minEssence;
    private PrerequisiteRule prerequisites;

    private String cost;
    private String type;
    private List<String> keywords;
    private String duration;
    private String fullText;
    private String rawData;
    private List<String> importWarnings;
    private String category; // "solar", "martialArts", "evocation"
    private transient boolean isCustom;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public List<String> getExaltTypes() {
        return exaltTypes;
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

    public PrerequisiteRule getPrerequisites() {
        return prerequisites;
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

    public void setPrerequisites(PrerequisiteRule prerequisites) {
        this.prerequisites = prerequisites;
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

    public List<String> getImportWarnings() {
        return importWarnings;
    }

    public void setImportWarnings(List<String> importWarnings) {
        this.importWarnings = importWarnings;
    }

    public abstract boolean isEligible(CharacterData data);

    public boolean qualifiesForCasteFavoredDiscount(CharacterData data) {
        return prerequisites == null || prerequisites.qualifiesForCasteFavoredDiscount(data);
    }

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
