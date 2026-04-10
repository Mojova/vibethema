package com.vibethema.model;

/** Represents the type of Exalt (Solar, Lunar, etc.), which determines the character's theme. */
public enum ExaltType {
    SOLAR,
    LUNAR,
    SIDEREAL,
    ABYSSAL,
    DRAGON_BLOODED,
    LIMINAL,
    GETHIMANU,
    EXIGENT;

    @Override
    public String toString() {
        String name = name().replace('_', ' ');
        return name.substring(0, 1) + name.substring(1).toLowerCase();
    }
}
