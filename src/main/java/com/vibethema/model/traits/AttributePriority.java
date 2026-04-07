package com.vibethema.model.traits;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.social.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.logic.*;


public enum AttributePriority {
    PRIMARY(8, "Primary (8)"), 
    SECONDARY(6, "Secondary (6)"), 
    TERTIARY(4, "Tertiary (4)");

    private final int freeDots;
    private final String displayName;

    AttributePriority(int freeDots, String displayName) {
        this.freeDots = freeDots;
        this.displayName = displayName;
    }

    public int getFreeDots() { return freeDots; }
    public String getDisplayName() { return displayName; }

    @Override
    public String toString() { return displayName; }
}