package com.vibethema.model.mystic;

import com.vibethema.model.*;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class MysticModel {
    private final ObservableList<PurchasedCharm> unlockedCharms =
            FXCollections.observableArrayList();
    private final ObservableList<ShapingRitual> shapingRituals =
            FXCollections.observableArrayList(
                    r -> new javafx.beans.Observable[] {r.nameProperty(), r.descriptionProperty()});
    private final ObservableList<Spell> spells =
            FXCollections.observableArrayList(
                    s ->
                            new javafx.beans.Observable[] {
                                s.nameProperty(), s.descriptionProperty(), s.circleProperty()
                            });

    private final BooleanProperty terrestrialSorceryAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty celestialSorceryAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty solarSorceryAvailable = new SimpleBooleanProperty(false);

    private final Consumer<Void> dirtyListener;
    private final Runnable statUpdateTrigger;
    private boolean isImporting = false;

    public MysticModel(Consumer<Void> dirtyListener, Runnable statUpdateTrigger) {
        this.dirtyListener = dirtyListener;
        this.statUpdateTrigger = statUpdateTrigger;

        unlockedCharms.addListener(
                (javafx.collections.ListChangeListener<? super PurchasedCharm>)
                        c -> {
                            updateSorceryAvailability();
                            statUpdateTrigger.run();
                            markDirty();
                        });

        shapingRituals.addListener(
                (javafx.collections.ListChangeListener<? super ShapingRitual>)
                        c -> {
                            markDirty();
                            while (c.next()) {
                                if (c.wasAdded()) {
                                    for (ShapingRitual r : c.getAddedSubList()) {
                                        r.nameProperty().addListener((obs, ov, nv) -> markDirty());
                                        r.descriptionProperty()
                                                .addListener((obs, ov, nv) -> markDirty());
                                    }
                                }
                            }
                        });

        spells.addListener(
                (javafx.collections.ListChangeListener<? super Spell>)
                        c -> {
                            markDirty();
                            while (c.next()) {
                                if (c.wasAdded()) {
                                    for (Spell s : c.getAddedSubList()) {
                                        s.nameProperty().addListener((obs, ov, nv) -> markDirty());
                                        s.descriptionProperty()
                                                .addListener((obs, ov, nv) -> markDirty());
                                        s.circleProperty()
                                                .addListener((obs, ov, nv) -> markDirty());
                                        s.costProperty().addListener((obs, ov, nv) -> markDirty());
                                        s.durationProperty()
                                                .addListener((obs, ov, nv) -> markDirty());
                                        s.getKeywords()
                                                .addListener(
                                                        (javafx.collections.ListChangeListener<
                                                                        ? super String>)
                                                                kc -> markDirty());
                                    }
                                }
                            }
                        });
    }

    public void setImporting(boolean importing) {
        this.isImporting = importing;
    }

    private void markDirty() {
        if (!isImporting) {
            dirtyListener.accept(null);
        }
    }

    public void updateSorceryAvailability() {
        terrestrialSorceryAvailable.set(hasCharmByName(SystemData.TERRESTRIAL_CIRCLE_SORCERY));
        celestialSorceryAvailable.set(hasCharmByName(SystemData.CELESTIAL_CIRCLE_SORCERY));
        solarSorceryAvailable.set(hasCharmByName(SystemData.SOLAR_CIRCLE_SORCERY));
    }

    private boolean hasCharmByName(String name) {
        return unlockedCharms.stream().anyMatch(pc -> pc.name() != null && pc.name().equals(name));
    }

    public ObservableList<PurchasedCharm> getUnlockedCharms() {
        return unlockedCharms;
    }

    public ObservableList<ShapingRitual> getShapingRituals() {
        return shapingRituals;
    }

    public ObservableList<Spell> getSpells() {
        return spells;
    }

    public BooleanProperty terrestrialSorceryAvailableProperty() {
        return terrestrialSorceryAvailable;
    }

    public BooleanProperty celestialSorceryAvailableProperty() {
        return celestialSorceryAvailable;
    }

    public BooleanProperty solarSorceryAvailableProperty() {
        return solarSorceryAvailable;
    }
}
