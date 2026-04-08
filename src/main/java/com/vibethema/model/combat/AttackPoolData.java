package com.vibethema.model.combat;

import com.vibethema.model.equipment.*;

/** Data class to store calculated attack and defense pools for a weapon. */
public class AttackPoolData {
    private final Weapon weapon;
    private final String abilityName;
    private final String witheringPool;
    private final int decisivePool;
    private final int damage;
    private final int parry;

    public AttackPoolData(
            Weapon weapon,
            String abilityName,
            String witheringPool,
            int decisivePool,
            int damage,
            int parry) {
        this.weapon = weapon;
        this.abilityName = abilityName;
        this.witheringPool = witheringPool;
        this.decisivePool = decisivePool;
        this.damage = damage;
        this.parry = parry;
    }

    public Weapon getWeapon() {
        return weapon;
    }

    public String getWeaponName() {
        return weapon.getName();
    }

    public String getAbilityName() {
        return abilityName;
    }

    public String getWitheringPool() {
        return witheringPool;
    }

    public int getDecisivePool() {
        return decisivePool;
    }

    public int getDamage() {
        return damage;
    }

    public int getParry() {
        return parry;
    }
}
