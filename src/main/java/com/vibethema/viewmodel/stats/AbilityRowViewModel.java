package com.vibethema.viewmodel.stats;

import com.vibethema.model.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;
import com.vibethema.model.traits.*;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * ViewModel for a single Ability row, encapsulating business rules for caste/favored selection and
 * excellency availability.
 */
public class AbilityRowViewModel {
    private final Ability ability;
    private final CharacterData data;

    private final StringProperty baseDisplayName = new SimpleStringProperty();
    private final BooleanBinding isExcellencyAvailable;
    private final BooleanBinding casteBoxDisabled;
    private final BooleanBinding favoredBoxDisabled;
    private final BooleanBinding isOption;
    private final IntegerBinding minDots;

    public AbilityRowViewModel(CharacterData data, Ability ability) {
        this.data = data;
        this.ability = ability;

        this.baseDisplayName.set(ability.getDisplayName());
        this.isExcellencyAvailable = data.excellencyProperty(ability);

        // Caste Box Disabled Logic:
        // Disabled if:
        // 1. It's Martial Arts (synced with Brawl)
        // 2. The ability is not in the current Caste's list
        // 3. We've reached the 5-caste limit AND it's not currently selected
        this.casteBoxDisabled =
                Bindings.createBooleanBinding(
                        () -> {
                            if (Ability.MARTIAL_ARTS == ability) return true;
                            Caste c = data.casteProperty().get();
                            boolean notInCasteList =
                                    c == null
                                            || c == Caste.NONE
                                            || !SystemData.CASTE_OPTIONS.get(c).contains(ability);
                            boolean atLimit =
                                    data.casteAbilityCountProperty().get() >= 5 && !isCaste().get();
                            return (notInCasteList || atLimit) && !isCaste().get();
                        },
                        data.casteProperty(),
                        data.casteAbilityCountProperty(),
                        isCaste());

        // Favored Box Disabled Logic:
        // Disabled if:
        // 1. It's Martial Arts (synced with Brawl)
        // 2. It's already marked as a Caste ability
        // 3. We've reached the 5-favored limit AND it's not currently selected
        this.favoredBoxDisabled =
                Bindings.createBooleanBinding(
                        () -> {
                            if (Ability.MARTIAL_ARTS == ability) return true;
                            boolean isCaste = isCaste().get();
                            boolean atLimit =
                                    data.favoredAbilityCountProperty().get() >= 5
                                            && !isFavored().get();
                            return (isCaste || atLimit) && !isFavored().get();
                        },
                        isCaste(),
                        data.favoredAbilityCountProperty(),
                        isFavored());

        // Highlight if it's an option for the current caste
        this.isOption =
                Bindings.createBooleanBinding(
                        () -> {
                            Caste c = data.casteProperty().get();
                            return c != null
                                    && c != Caste.NONE
                                    && SystemData.CASTE_OPTIONS.get(c).contains(ability);
                        },
                        data.casteProperty());

        // Brawl special rule: min dots is 1 if favored and no MA styles exist
        this.minDots =
                Bindings.createIntegerBinding(
                        () -> {
                            if (ability != Ability.BRAWL) return 0;
                            int maDots =
                                    data.getMartialArtsStyles().stream()
                                            .mapToInt(mas -> mas.ratingProperty().get())
                                            .sum();
                            return (!data.getFavoredAbility(Ability.BRAWL).get() || maDots > 0)
                                    ? 0
                                    : 1;
                        },
                        data.getFavoredAbility(Ability.BRAWL),
                        data.getMartialArtsStyles());
    }

    public Ability getAbility() {
        return ability;
    }

    public StringProperty baseDisplayNameProperty() {
        return baseDisplayName;
    }

    public IntegerProperty ratingProperty() {
        return data.getAbility(ability);
    }

    public BooleanProperty isCaste() {
        return data.getCasteAbility(ability);
    }

    public BooleanProperty isFavored() {
        return data.getFavoredAbility(ability);
    }

    public BooleanBinding isExcellencyAvailableProperty() {
        return isExcellencyAvailable;
    }

    public BooleanBinding casteBoxDisabledProperty() {
        return casteBoxDisabled;
    }

    public BooleanBinding favoredBoxDisabledProperty() {
        return favoredBoxDisabled;
    }

    public BooleanBinding isOptionProperty() {
        return isOption;
    }

    public IntegerBinding minDotsProperty() {
        return minDots;
    }

    public ObjectProperty<CharacterMode> modeProperty() {
        return data.modeProperty();
    }
}
