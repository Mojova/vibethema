package com.vibethema.model;

public enum Caste {
    DAWN, ZENITH, TWILIGHT, NIGHT, ECLIPSE, NONE;

    @Override
    public String toString() {
        if (this == NONE) return "None";
        String s = super.toString();
        return s.substring(0, 1) + s.substring(1).toLowerCase();
    }
}
