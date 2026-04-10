package com.vibethema.model.traits;

import java.util.List;

/**
 * Data model for reference merits loaded from merits.json.
 * This class corresponds to the structure of the merits in data_source/reference/merits/merits.json.
 */
public class MeritReference {
    private String id;
    private String name;
    private String category;
    private List<Integer> ratings;
    private String description;
    private String prerequisites;
    private String rawData;

    public MeritReference() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<Integer> getRatings() {
        return ratings;
    }

    public void setRatings(List<Integer> ratings) {
        this.ratings = ratings;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(String prerequisites) {
        this.prerequisites = prerequisites;
    }

    public String getRawData() {
        return rawData;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
    }

    @Override
    public String toString() {
        return name;
    }
}
