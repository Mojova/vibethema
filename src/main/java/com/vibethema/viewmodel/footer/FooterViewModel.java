package com.vibethema.viewmodel.footer;

import com.vibethema.model.*;
import com.vibethema.model.logic.*;
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
    private final StringProperty meritsText = new SimpleStringProperty();
    private final StringProperty validationText = new SimpleStringProperty();
    private final StringProperty validationTitle = new SimpleStringProperty();
    private final java.util.function.Supplier<CreationValidator.Result> validator;
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
        this(data, finalizeAction, () -> CreationValidator.validate(data));
    }

    public FooterViewModel(
            CharacterData data,
            Runnable finalizeAction,
            java.util.function.Supplier<CreationValidator.Result> validator) {
        this.data = data;
        this.finalizeAction = finalizeAction;
        this.validator = validator;

        setupListeners();
        update();
    }

    private void setupListeners() {
        ChangeListener<Object> updater = (obs, oldV, newV) -> update();
        ListChangeListener<Object> listUpdater = c -> update();

        data.modeProperty().addListener(updater);
        data.exaltTypeProperty().addListener(updater);
        data.limitTriggerProperty().addListener(updater);
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
        observeItems(
                data.getMerits(), m -> java.util.List.of(m.ratingProperty(), m.nameProperty()));
        data.getSpecialties().addListener(listUpdater);
        observeItems(
                data.getCrafts(),
                c ->
                        java.util.List.of(
                                c.ratingProperty(),
                                c.expertiseProperty(),
                                c.casteProperty(),
                                c.favoredProperty()));
        observeItems(
                data.getMartialArtsStyles(),
                m ->
                        java.util.List.of(
                                m.ratingProperty(),
                                m.styleNameProperty(),
                                m.casteProperty(),
                                m.favoredProperty()));
        data.getXpAwards().addListener(listUpdater);
        data.getSpells().addListener(listUpdater);
        data.getIntimacies().addListener(listUpdater);
        data.getWeapons().addListener(listUpdater);
        data.getArmors().addListener(listUpdater);
        data.getOtherEquipment().addListener(listUpdater);
    }

    /** Attach to item edits and detach when items leave the character (including undo/load). */
    private <T> void observeItems(
            javafx.collections.ObservableList<T> items,
            java.util.function.Function<T, java.util.List<javafx.beans.Observable>> properties) {
        java.util.Map<T, java.util.List<javafx.beans.Observable>> observed =
                new java.util.IdentityHashMap<>();
        javafx.beans.InvalidationListener listener = ignored -> update();
        Runnable sync =
                () -> {
                    var iterator = observed.entrySet().iterator();
                    while (iterator.hasNext()) {
                        var entry = iterator.next();
                        if (!items.contains(entry.getKey())) {
                            entry.getValue().forEach(p -> p.removeListener(listener));
                            iterator.remove();
                        }
                    }
                    for (T item : items) {
                        if (!observed.containsKey(item)) {
                            var itemProperties = properties.apply(item);
                            itemProperties.forEach(p -> p.addListener(listener));
                            observed.put(item, itemProperties);
                        }
                    }
                };
        sync.run();
        items.addListener(
                (ListChangeListener<T>)
                        change -> {
                            sync.run();
                            update();
                        });
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
        bpStyle.set(status.overBonusPoints ? "vbtm-status-warning" : "vbtm-status-normal");
        casteText.set("Caste: " + data.casteAbilityCountProperty().get() + "/5");
        favoredText.set("Favored: " + data.favoredAbilityCountProperty().get() + "/5");
        attrText.set(
                String.format(
                        "Attributes: %d/%d/%d",
                        status.physicalDots, status.socialDots, status.mentalDots));
        abilitiesText.set("Abilities: " + status.abilitiesSpent + "/28");
        specialtiesText.set("Specialties: " + status.specialtiesSpent + "/4");
        charmsText.set("Charms: " + status.charmsSpent + "/15");

        meritsText.set("Merits: " + status.meritsSpent + "/10");
        CreationValidator.Result result = validator.get();
        finalizeDisabled.set(!result.isReady());
        validationTitle.set(
                result.isReady()
                        ? "Creation checklist: ready"
                        : "Creation checklist: " + result.issues().size() + " remaining");
        validationText.set(
                (result.isReady() ? "All automatic creation checks passed." : result.describe())
                        + "\n"
                        + "Review your Intimacies with your Storyteller: include at least one"
                        + " positive and one negative Intimacy.");
        finalizeTooltip.set(
                result.isReady()
                        ? "All automatic checks passed. Click to finalize."
                        : result.describe());
    }

    private void updateExperiencedStatus() {
        ExperienceRuleEngine.ExperienceStatus status =
                ExperienceRuleEngine.calculateStatus(data, data.getCreationSnapshot());
        regularXpText.set(
                "Regular XP: " + status.regularXpSpent + "/" + status.totalRegularXpAwarded);
        regularXpStyle.set(
                status.getRegularXpRemaining() < 0 ? "vbtm-status-warning" : "vbtm-status-normal");
        solarXpText.set("Solar XP: " + status.solarXpSpent + "/" + status.totalSolarXpAwarded);
        solarXpStyle.set(
                status.getSolarXpRemaining() < 0 ? "vbtm-status-warning" : "vbtm-status-accent");
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

    public StringProperty meritsTextProperty() {
        return meritsText;
    }

    public StringProperty validationTextProperty() {
        return validationText;
    }

    public StringProperty validationTitleProperty() {
        return validationTitle;
    }

    public void finalizeCharacter() {
        update();
        if (data.getMode() == CharacterMode.CREATION
                && !finalizeDisabled.get()
                && finalizeAction != null) {
            finalizeAction.run();
        }
    }
}
