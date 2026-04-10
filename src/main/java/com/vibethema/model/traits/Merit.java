package com.vibethema.model.traits;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Merit {
    private final String id;
    private final String definitionId;
    private final StringProperty name = new SimpleStringProperty("");
    private final IntegerProperty rating = new SimpleIntegerProperty(1);
    private final StringProperty description = new SimpleStringProperty("");

    public Merit(String id, String name, int rating) {
        this(id, null, name, rating, "");
    }

    public Merit(String id, String definitionId, String name, int rating) {
        this(id, definitionId, name, rating, "");
    }

    public Merit(String id, String definitionId, String name, int rating, String description) {
        this.id = id;
        this.definitionId = definitionId;
        this.name.set(name);
        this.rating.set(rating);
        this.description.set(description);
    }

    public String getId() {
        return id;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public IntegerProperty ratingProperty() {
        return rating;
    }

    public int getRating() {
        return rating.get();
    }

    public void setRating(int rating) {
        this.rating.set(rating);
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public String getDescription() {
        return description.get();
    }

    public void setDescription(String description) {
        this.description.set(description);
    }
}
