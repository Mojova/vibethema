package com.vibethema.model.logic;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.social.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.logic.*;

public class ExperiencePurchase {
    private final String description;
    private final int cost;
    private final boolean canUseSolarXp;

    public ExperiencePurchase(String description, int cost, boolean canUseSolarXp) {
        this.description = description;
        this.cost = cost;
        this.canUseSolarXp = canUseSolarXp;
    }

    public String getDescription() { return description; }
    public int getCost() { return cost; }
    public boolean canUseSolarXp() { return canUseSolarXp; }
}