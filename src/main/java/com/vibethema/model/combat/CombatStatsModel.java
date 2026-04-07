package com.vibethema.model.combat;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.social.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.logic.*;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CombatStatsModel {
    private final IntegerProperty naturalSoak = new SimpleIntegerProperty(0);
    private final IntegerProperty totalSoak = new SimpleIntegerProperty(0);

    private final IntegerProperty evasion = new SimpleIntegerProperty(0);
    private final IntegerProperty evasionBonus = new SimpleIntegerProperty(0);
    private final BooleanProperty hasEvasionSpecialty = new SimpleBooleanProperty(false);
    private final IntegerProperty resolve = new SimpleIntegerProperty(0);
    private final IntegerProperty resolveBonus = new SimpleIntegerProperty(0);
    private final BooleanProperty hasResolveSpecialty = new SimpleBooleanProperty(false);
    private final IntegerProperty guile = new SimpleIntegerProperty(0);
    private final IntegerProperty guileBonus = new SimpleIntegerProperty(0);
    private final BooleanProperty hasGuileSpecialty = new SimpleBooleanProperty(false);
    private final IntegerProperty joinBattle = new SimpleIntegerProperty(0);
    private final BooleanProperty hasJoinBattleSpecialty = new SimpleBooleanProperty(false);

    private final IntegerProperty personalMotes = new SimpleIntegerProperty(0);
    private final IntegerProperty peripheralMotes = new SimpleIntegerProperty(0);
    private final ObservableList<String> healthLevels = FXCollections.observableArrayList();
    private final ObservableList<AttackPoolData> attackPools = FXCollections.observableArrayList();

    private final TraitModel traitModel;
    private final EquipmentModel equipmentModel;
    private final MysticModel mysticModel;

    public CombatStatsModel(TraitModel traitModel, EquipmentModel equipmentModel, MysticModel mysticModel) {
        this.traitModel = traitModel;
        this.equipmentModel = equipmentModel;
        this.mysticModel = mysticModel;
    }

    public void updateAll() {
        updateCombatStats();
        updateDerivedStats();
        updateAttackPools();
    }

    public void updateCombatStats() {
        int stamina = traitModel.getAttribute(Attribute.STAMINA).get();
        naturalSoak.set(stamina);
        totalSoak.set(stamina + equipmentModel.armorSoakProperty().get());
    }

    public void updateDerivedStats() {
        int dex = traitModel.getAttribute(Attribute.DEXTERITY).get();
        int dodge = traitModel.getAbility(Ability.DODGE).get();
        int wits = traitModel.getAttribute(Attribute.WITS).get();
        int integrity = traitModel.getAbility(Ability.INTEGRITY).get();
        int manipulation = traitModel.getAttribute(Attribute.MANIPULATION).get();
        int socialize = traitModel.getAbility(Ability.SOCIALIZE).get();
        int awareness = traitModel.getAbility(Ability.AWARENESS).get();

        int evasionBase = (int) Math.ceil((dex + dodge) / 2.0);
        int resolveBase = (int) Math.ceil((wits + integrity) / 2.0);
        int guileBase = (int) Math.ceil((manipulation + socialize) / 2.0);

        evasion.set(evasionBase);
        resolve.set(resolveBase);
        guile.set(guileBase);
        joinBattle.set(wits + awareness);

        evasionBonus.set((int) Math.ceil((dex + dodge + 1) / 2.0) - evasionBase);
        resolveBonus.set((int) Math.ceil((wits + integrity + 1) / 2.0) - resolveBase);
        guileBonus.set((int) Math.ceil((manipulation + socialize + 1) / 2.0) - guileBase);

        hasEvasionSpecialty.set(hasSpecialtyFor(Ability.DODGE));
        hasResolveSpecialty.set(hasSpecialtyFor(Ability.INTEGRITY));
        hasGuileSpecialty.set(hasSpecialtyFor(Ability.SOCIALIZE));
        hasJoinBattleSpecialty.set(hasSpecialtyFor(Ability.AWARENESS));

        updateMotesAndHealth();
    }

    private boolean hasSpecialtyFor(Ability ability) {
        return traitModel.getSpecialties().stream()
                .anyMatch(s -> ability.getDisplayName().equals(s.getAbility()) && s.getName() != null && !s.getName().trim().isEmpty());
    }

    public void updateMotesAndHealth() {
        // This normally would use Essence from CharacterData, we'll need to pass it or have it handled.
        // For now, assume we'll update it from CharacterData delegation.
    }

    public void updateMotesAndHealth(int essence) {
        personalMotes.set((essence * 3) + 10);
        peripheralMotes.set((essence * 7) + 26);

        List<String> levels = new ArrayList<>(Arrays.asList("-0", "-1", "-1", "-2", "-2", "-4", "Incap"));
        int stamina = traitModel.getAttribute(Attribute.STAMINA).get();

        int oxBodyCount = 0;
        for (PurchasedCharm pc : mysticModel.getUnlockedCharms()) {
            if (pc.name().equals("Ox-Body Technique")) {
                oxBodyCount++;
            }
        }

        for (int i = 0; i < oxBodyCount; i++) {
            if (stamina >= 5) {
                levels.add("-0");
                levels.add("-1");
                levels.add("-2");
            } else if (stamina >= 3) {
                levels.add("-1");
                levels.add("-2");
                levels.add("-2");
            } else {
                levels.add("-1");
                levels.add("-2");
            }
        }
        Collections.sort(levels);
        healthLevels.setAll(levels);
    }

    public void updateAttackPools() {
        List<AttackPoolData> newPools = new ArrayList<>();
        int dex = traitModel.getAttribute(Attribute.DEXTERITY).get();
        int str = traitModel.getAttribute(Attribute.STRENGTH).get();

        for (Weapon w : equipmentModel.getWeapons()) {
            String abilityName = Ability.MELEE.getDisplayName();
            if (w.getRange() == Weapon.WeaponRange.ARCHERY) abilityName = Ability.ARCHERY.getDisplayName();
            else if (w.getRange() == Weapon.WeaponRange.THROWN) abilityName = Ability.THROWN.getDisplayName();
            else if (w.getTags().contains("Brawl")) abilityName = Ability.BRAWL.getDisplayName();

            int abil = traitModel.getAbilityRating(Ability.fromString(abilityName));
            int spec = (w.getSpecialtyId() != null && !w.getSpecialtyId().isEmpty()) ? 1 : 0;

            String witheringStr;
            if (w.getRange() == Weapon.WeaponRange.CLOSE) {
                int withering = dex + abil + spec + w.getAccuracy();
                witheringStr = String.valueOf(withering);
            } else {
                int base = dex + abil + spec;
                int c = base + w.getCloseRangeBonus();
                int s = base + w.getShortRangeBonus();
                int m = base + w.getMediumRangeBonus();
                int l = base + w.getLongRangeBonus();
                int e = base + w.getExtremeRangeBonus();
                witheringStr = String.format("C:%d | S:%d | M:%d | L:%d | E:%d", c, s, m, l, e);
            }

            int decisive = dex + abil + spec;
            int damage = str + w.getDamage();
            int parry = (int) Math.ceil((dex + abil) / 2.0) + w.getDefense();

            newPools.add(new AttackPoolData(w, abilityName, witheringStr, decisive, damage, parry));
        }
        attackPools.setAll(newPools);
    }

    public IntegerProperty naturalSoakProperty() { return naturalSoak; }
    public IntegerProperty totalSoakProperty() { return totalSoak; }
    public IntegerProperty evasionProperty() { return evasion; }
    public IntegerProperty evasionBonusProperty() { return evasionBonus; }
    public BooleanProperty hasEvasionSpecialtyProperty() { return hasEvasionSpecialty; }
    public IntegerProperty resolveProperty() { return resolve; }
    public IntegerProperty resolveBonusProperty() { return resolveBonus; }
    public BooleanProperty hasResolveSpecialtyProperty() { return hasResolveSpecialty; }
    public IntegerProperty guileProperty() { return guile; }
    public IntegerProperty guileBonusProperty() { return guileBonus; }
    public BooleanProperty hasGuileSpecialtyProperty() { return hasGuileSpecialty; }
    public IntegerProperty joinBattleProperty() { return joinBattle; }
    public BooleanProperty hasJoinBattleSpecialtyProperty() { return hasJoinBattleSpecialty; }
    public IntegerProperty personalMotesProperty() { return personalMotes; }
    public IntegerProperty peripheralMotesProperty() { return peripheralMotes; }
    public ObservableList<String> healthLevelsProperty() { return healthLevels; }
    public ObservableList<AttackPoolData> getAttackPools() { return attackPools; }
}