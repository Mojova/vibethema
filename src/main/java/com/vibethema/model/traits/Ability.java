package com.vibethema.model.traits;

import com.vibethema.model.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;

public enum Ability {
    ARCHERY("Archery"),
    ATHLETICS("Athletics"),
    AWARENESS("Awareness"),
    BRAWL("Brawl"),
    BUREAUCRACY("Bureaucracy"),
    CRAFT("Craft"),
    DODGE("Dodge"),
    INTEGRITY("Integrity"),
    INVESTIGATION("Investigation"),
    LARCENY("Larceny"),
    LINGUISTICS("Linguistics"),
    LORE("Lore"),
    MARTIAL_ARTS("Martial Arts"),
    MEDICINE("Medicine"),
    MELEE("Melee"),
    OCCULT("Occult"),
    PERFORMANCE("Performance"),
    PRESENCE("Presence"),
    RESISTANCE("Resistance"),
    RIDE("Ride"),
    SAIL("Sail"),
    SOCIALIZE("Socialize"),
    STEALTH("Stealth"),
    SURVIVAL("Survival"),
    THROWN("Thrown"),
    WAR("War");

    private final String displayName;

    Ability(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Ability fromString(String name) {
        if (name == null) return null;
        for (Ability a : values()) {
            if (a.displayName.equalsIgnoreCase(name.trim())) return a;
        }
        return null;
    }
}
