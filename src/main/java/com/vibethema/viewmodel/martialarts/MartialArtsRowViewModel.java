package com.vibethema.viewmodel.martialarts;

import com.vibethema.model.CharacterData;
import com.vibethema.model.traits.Ability;
import com.vibethema.model.traits.MartialArtsStyle;
import java.util.function.BiConsumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** ViewModel for a single Martial Arts style row. */
public class MartialArtsRowViewModel {
    private final MartialArtsStyle model;
    private final CharacterData data;
    private final BiConsumer<String, String> onCommitCallback;

    private final StringProperty draftName = new SimpleStringProperty("");
    private final BooleanProperty editing = new SimpleBooleanProperty(false);

    public MartialArtsRowViewModel(
            CharacterData data,
            MartialArtsStyle model,
            BiConsumer<String, String> onCommitCallback) {
        this.data = data;
        this.model = model;
        this.onCommitCallback = onCommitCallback;
        this.draftName.set(model.getStyleName());
    }

    public StringProperty nameProperty() {
        return model.styleNameProperty();
    }

    public IntegerProperty ratingProperty() {
        return model.ratingProperty();
    }

    public BooleanProperty isCaste() {
        return data.getCasteAbility(Ability.MARTIAL_ARTS);
    }

    public BooleanProperty isFavored() {
        return data.getFavoredAbility(Ability.MARTIAL_ARTS);
    }

    public StringProperty draftNameProperty() {
        return draftName;
    }

    public BooleanProperty editingProperty() {
        return editing;
    }

    public void beginEdit() {
        draftName.set(model.getStyleName());
        editing.set(true);
    }

    public void commitEdit() {
        String newName = draftName.get().trim();
        if (!newName.isEmpty()) {
            model.setStyleName(newName);
            if (onCommitCallback != null) {
                onCommitCallback.accept(model.getId(), newName);
            }
        }
        editing.set(false);
    }

    public void cancelEdit() {
        editing.set(false);
    }

    public MartialArtsStyle getModel() {
        return model;
    }
}
