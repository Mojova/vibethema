package com.vibethema.viewmodel.footer;

import com.vibethema.model.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;
import com.vibethema.model.traits.*;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;

public class FooterViewModel implements ViewModel {
    private final CharacterData data;

    // Creation Mode Properties
    private final StringProperty bpSpentText = new SimpleStringProperty();
    private final StringProperty bpStyle = new SimpleStringProperty();
    private final StringProperty casteText = new SimpleStringProperty();
    private final StringProperty favoredText = new SimpleStringProperty();
    private final StringProperty attrText = new SimpleStringProperty();
    private final StringProperty abilitiesText = new SimpleStringProperty();
    private final StringProperty specialtiesText = new SimpleStringProperty();
    private final StringProperty charmsText = new SimpleStringProperty();
    private final BooleanProperty creationModeVisible = new SimpleBooleanProperty();

    // Finalize Button
    private final BooleanProperty finalizeDisabled = new SimpleBooleanProperty();
    private final StringProperty finalizeTooltip = new SimpleStringProperty();

    // Experience Mode Properties
    private final StringProperty regularXpText = new SimpleStringProperty();
    private final StringProperty regularXpStyle = new SimpleStringProperty();
    private final StringProperty solarXpText = new SimpleStringProperty();
    private final StringProperty solarXpStyle = new SimpleStringProperty();
    private final BooleanProperty experiencedModeVisible = new SimpleBooleanProperty();

    private final Runnable finalizeAction;

    public FooterViewModel(CharacterData data, Runnable finalizeAction) {
        this.data = data;
        this.finalizeAction = finalizeAction;

        setupListeners();
        update();
    }

    private void setupListeners() {
        ChangeListener<Object> updater = (obs, oldV, newV) -> update();
        ListChangeListener<Object> listUpdater = c -> update();

        data.modeProperty().addListener(updater);
        data.casteProperty().addListener(updater);
        data.supernalAbilityProperty().addListener(updater);
        data.essenceProperty().addListener(updater);
        data.willpowerProperty().addListener(updater);

        data.getAttributes().values().forEach(p -> p.addListener(updater));
        data.getAbilities().values().forEach(p -> p.addListener(updater));
        data.getCasteAbilities().values().forEach(p -> p.addListener(updater));
        data.getFavoredAbilities().values().forEach(p -> p.addListener(updater));
        data.getAttributePriorities().values().forEach(p -> p.addListener(updater));

        data.getUnlockedCharms().addListener(listUpdater);
        data.getMerits().addListener(listUpdater);
        data.getSpecialties().addListener(listUpdater);
        data.getCrafts().addListener(listUpdater);
        data.getMartialArtsStyles().addListener(listUpdater);
        data.getXpAwards().addListener(listUpdater);
    }

    public void update() {
        CharacterMode mode = data.getMode();
        creationModeVisible.set(mode == CharacterMode.CREATION);
        experiencedModeVisible.set(mode == CharacterMode.EXPERIENCED);

        if (mode == CharacterMode.CREATION) {
            updateCreationStatus();
        } else {
            updateExperiencedStatus();
        }
    }

    private void updateCreationStatus() {
        CreationRuleEngine.CreationStatus status = CreationRuleEngine.calculateStatus(data);
        bpSpentText.set("BP Spent: " + status.bonusPointsSpent + "/15");
        bpStyle.set(
                status.overBonusPoints
                        ? "-fx-text-fill: red; -fx-font-weight: bold;"
                        : "-fx-text-fill: white;");
        casteText.set("Caste: " + data.casteAbilityCountProperty().get() + "/5");
        favoredText.set("Favored: " + data.favoredAbilityCountProperty().get() + "/5");
        attrText.set(
                String.format(
                        "Attributes: %d/%d/%d",
                        status.physicalDots, status.socialDots, status.mentalDots));
        abilitiesText.set("Abilities: " + status.abilitiesSpent + "/28");
        specialtiesText.set("Specialties: " + status.specialtiesSpent + "/4");
        charmsText.set("Charms: " + status.charmsSpent + "/15");

        finalizeDisabled.set(!status.isReadyToFinalize);
        if (status.isReadyToFinalize) {
            finalizeTooltip.set("All creation points spent. Click to finalize.");
        } else {
            finalizeTooltip.set(
                    "Check creation pools: Attributes (18), Abilities (28), Merits (10),"
                            + " Specialties (4), Charms (15), and 15 BP.");
        }
    }

    private void updateExperiencedStatus() {
        ExperienceRuleEngine.ExperienceStatus status =
                ExperienceRuleEngine.calculateStatus(data, data.getCreationSnapshot());
        regularXpText.set(
                "Regular XP: " + status.regularXpSpent + "/" + status.totalRegularXpAwarded);
        regularXpStyle.set(
                status.getRegularXpRemaining() < 0
                        ? "-fx-text-fill: red; -fx-font-weight: bold;"
                        : "-fx-text-fill: white;");
        solarXpText.set("Solar XP: " + status.solarXpSpent + "/" + status.totalSolarXpAwarded);
        solarXpStyle.set(
                status.getSolarXpRemaining() < 0
                        ? "-fx-text-fill: red; -fx-font-weight: bold;"
                        : "-fx-text-fill: #d4af37;");
    }

    // Properties accessors
    public StringProperty bpSpentTextProperty() {
        return bpSpentText;
    }

    public StringProperty bpStyleProperty() {
        return bpStyle;
    }

    public StringProperty casteTextProperty() {
        return casteText;
    }

    public StringProperty favoredTextProperty() {
        return favoredText;
    }

    public StringProperty attrTextProperty() {
        return attrText;
    }

    public StringProperty abilitiesTextProperty() {
        return abilitiesText;
    }

    public StringProperty specialtiesTextProperty() {
        return specialtiesText;
    }

    public StringProperty charmsTextProperty() {
        return charmsText;
    }

    public BooleanProperty creationModeVisibleProperty() {
        return creationModeVisible;
    }

    public BooleanProperty finalizeDisabledProperty() {
        return finalizeDisabled;
    }

    public StringProperty finalizeTooltipProperty() {
        return finalizeTooltip;
    }

    public StringProperty regularXpTextProperty() {
        return regularXpText;
    }

    public StringProperty regularXpStyleProperty() {
        return regularXpStyle;
    }

    public StringProperty solarXpTextProperty() {
        return solarXpText;
    }

    public StringProperty solarXpStyleProperty() {
        return solarXpStyle;
    }

    public BooleanProperty experiencedModeVisibleProperty() {
        return experiencedModeVisible;
    }

    public void finalizeCharacter() {
        if (finalizeAction != null) {
            finalizeAction.run();
        }
    }
}
