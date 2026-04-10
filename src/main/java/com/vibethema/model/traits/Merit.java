package com.vibethema.model.traits;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Merit {
    private final String id;
    private final StringProperty name = new SimpleStringProperty("");
    private final IntegerProperty rating = new SimpleIntegerProperty(1);

    public Merit(String id, String name, int rating) {
        this.id = id;
        this.name.set(name);
        this.rating.set(rating);
    }

    public String getId() {
        return id;
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
}
