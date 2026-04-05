package com.vibethema.viewmodel.footer;

import com.vibethema.model.CharacterData;
import com.vibethema.model.CharacterMode;
import com.vibethema.model.logic.CreationRuleEngine;
import com.vibethema.model.logic.CreationRuleEngine.CreationStatus;
import com.vibethema.model.logic.ExperienceRuleEngine;
import com.vibethema.model.logic.ExperienceRuleEngine.ExperienceStatus;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

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
        
        // Initial update
        update();
        
        // Listeners
        data.modeProperty().addListener((obs, oldV, newV) -> update());
        // In a real app, we'd listen to many more specific properties, 
        // but for now we can have a global refresh or listen to key ones.
        // BuilderUI currently calls updateFooter() on almost every change.
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
        CreationStatus status = CreationRuleEngine.calculateStatus(data);
        bpSpentText.set("BP Spent: " + status.bonusPointsSpent + "/15");
        bpStyle.set(status.overBonusPoints ? "-fx-text-fill: red; -fx-font-weight: bold;" : "-fx-text-fill: white;");
        casteText.set("Caste: " + data.casteAbilityCountProperty().get() + "/5");
        favoredText.set("Favored: " + data.favoredAbilityCountProperty().get() + "/5");
        attrText.set(String.format("Attributes: %d/%d/%d", status.physicalDots, status.socialDots, status.mentalDots));
        abilitiesText.set("Abilities: " + status.abilitiesSpent + "/28");
        specialtiesText.set("Specialties: " + status.specialtiesSpent + "/4");
        charmsText.set("Charms: " + status.charmsSpent + "/15");
        
        finalizeDisabled.set(!status.isReadyToFinalize);
        if (status.isReadyToFinalize) {
            finalizeTooltip.set("All creation points spent. Click to finalize.");
        } else {
            finalizeTooltip.set("Check creation pools: Attributes (18), Abilities (28), Merits (10), Specialties (4), Charms (15), and 15 BP.");
        }
    }

    private void updateExperiencedStatus() {
        ExperienceStatus status = ExperienceRuleEngine.calculateStatus(data, data.getCreationSnapshot());
        regularXpText.set("Regular XP: " + status.regularXpSpent + "/" + status.totalRegularXpAwarded);
        regularXpStyle.set(status.getRegularXpRemaining() < 0 ? "-fx-text-fill: red; -fx-font-weight: bold;" : "-fx-text-fill: white;");
        solarXpText.set("Solar XP: " + status.solarXpSpent + "/" + status.totalSolarXpAwarded);
        solarXpStyle.set(status.getSolarXpRemaining() < 0 ? "-fx-text-fill: red; -fx-font-weight: bold;" : "-fx-text-fill: #d4af37;");
    }

    // Properties accessors
    public StringProperty bpSpentTextProperty() { return bpSpentText; }
    public StringProperty bpStyleProperty() { return bpStyle; }
    public StringProperty casteTextProperty() { return casteText; }
    public StringProperty favoredTextProperty() { return favoredText; }
    public StringProperty attrTextProperty() { return attrText; }
    public StringProperty abilitiesTextProperty() { return abilitiesText; }
    public StringProperty specialtiesTextProperty() { return specialtiesText; }
    public StringProperty charmsTextProperty() { return charmsText; }
    public BooleanProperty creationModeVisibleProperty() { return creationModeVisible; }
    public BooleanProperty finalizeDisabledProperty() { return finalizeDisabled; }
    public StringProperty finalizeTooltipProperty() { return finalizeTooltip; }
    public StringProperty regularXpTextProperty() { return regularXpText; }
    public StringProperty regularXpStyleProperty() { return regularXpStyle; }
    public StringProperty solarXpTextProperty() { return solarXpText; }
    public StringProperty solarXpStyleProperty() { return solarXpStyle; }
    public BooleanProperty experiencedModeVisibleProperty() { return experiencedModeVisible; }

    public void finalizeCharacter() {
        if (finalizeAction != null) {
            finalizeAction.run();
        }
    }
}
