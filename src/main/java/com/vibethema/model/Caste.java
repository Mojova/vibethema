package com.vibethema.model;

import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;
import com.vibethema.model.traits.*;

public enum Caste {
    DAWN,
    ZENITH,
    TWILIGHT,
    NIGHT,
    ECLIPSE,
    NONE;

    @Override
    public String toString() {
        if (this == NONE) return "None";
        String s = super.toString();
        return s.substring(0, 1) + s.substring(1).toLowerCase();
    }
}
