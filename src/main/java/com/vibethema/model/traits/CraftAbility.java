package com.vibethema.model.traits;

import java.util.UUID;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class CraftAbility {
    private final UUID id;
    private final StringProperty expertise = new SimpleStringProperty("");
    private final IntegerProperty rating = new SimpleIntegerProperty(0);
    private final BooleanProperty isCaste = new SimpleBooleanProperty(false);
    private final BooleanProperty isFavored = new SimpleBooleanProperty(false);

    public CraftAbility(String expertise, int rating) {
        this(UUID.randomUUID(), expertise, rating);
    }

    public CraftAbility(UUID id, String expertise, int rating) {
        this.id = id;
        this.expertise.set(expertise);
        this.rating.set(rating);
    }

    public UUID getId() {
        return id;
    }

    public StringProperty expertiseProperty() {
        return expertise;
    }

    public String getExpertise() {
        return expertise.get();
    }

    public void setExpertise(String expertise) {
        this.expertise.set(expertise);
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

    public BooleanProperty casteProperty() {
        return isCaste;
    }

    public boolean isCaste() {
        return isCaste.get();
    }

    public void setCaste(boolean caste) {
        this.isCaste.set(caste);
    }

    public BooleanProperty favoredProperty() {
        return isFavored;
    }

    public boolean isFavored() {
        return isFavored.get();
    }

    public void setFavored(boolean favored) {
        this.isFavored.set(favored);
    }
}
