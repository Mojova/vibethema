package com.vibethema.viewmodel.stats;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * ViewModel for an attribute category (Physical, Social, or Mental), managing its priority and its
 * constituent rows.
 */
public class AttributeCategoryViewModel {
    private final Attribute.Category category;
    private final StringProperty title = new SimpleStringProperty();
    private final ObservableList<AttributeRowViewModel> attributeRows =
            FXCollections.observableArrayList();
    private final CharacterData data;

    public AttributeCategoryViewModel(
            CharacterData data, Attribute.Category category, List<Attribute> attrs) {
        this.data = data;
        this.category = category;
        this.title.set(category.name());

        this.attributeRows.setAll(
                attrs.stream()
                        .map(a -> new AttributeRowViewModel(data, a))
                        .collect(Collectors.toList()));
    }

    public StringProperty titleProperty() {
        return title;
    }

    public ObjectProperty<AttributePriority> priorityProperty() {
        return data.getAttributePriority(category);
    }

    public ObjectProperty<CharacterMode> modeProperty() {
        return data.modeProperty();
    }

    public ObservableList<AttributeRowViewModel> getAttributeRows() {
        return attributeRows;
    }
}
