package com.vibethema.viewmodel.stats;

import com.vibethema.model.traits.Merit;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** ViewModel for a single Merit row, wrapping the Merit model. */
public class MeritRowViewModel {
    private final Merit model;
    private final BooleanProperty editing = new SimpleBooleanProperty(false);
    private final StringProperty draftName = new SimpleStringProperty("");

    public MeritRowViewModel(Merit model) {
        this.model = model;
    }

    public Merit getModel() {
        return model;
    }

    public StringProperty nameProperty() {
        return model.nameProperty();
    }

    public IntegerProperty ratingProperty() {
        return model.ratingProperty();
    }

    public StringProperty descriptionProperty() {
        return model.descriptionProperty();
    }

    public BooleanProperty editingProperty() {
        return editing;
    }

    public StringProperty draftNameProperty() {
        return draftName;
    }

    public void beginEdit() {
        draftName.set(model.getName());
        editing.set(true);
    }

    public void commitEdit() {
        model.setName(draftName.get());
        editing.set(false);
    }

    public void cancelEdit() {
        editing.set(false);
    }
}
