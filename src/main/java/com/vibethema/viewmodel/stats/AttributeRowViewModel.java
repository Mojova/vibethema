package com.vibethema.viewmodel.stats;

import com.vibethema.model.*;
import com.vibethema.model.traits.Attribute;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** ViewModel for a single Attribute row. */
public class AttributeRowViewModel {
    private final Attribute attribute;
    private final CharacterData data;
    private final StringProperty displayName = new SimpleStringProperty();

    public AttributeRowViewModel(CharacterData data, Attribute attribute) {
        this.data = data;
        this.attribute = attribute;
        this.displayName.set(attribute.getDisplayName());
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public StringProperty displayNameProperty() {
        return displayName;
    }

    public IntegerProperty ratingProperty() {
        return data.getAttribute(attribute);
    }
}
