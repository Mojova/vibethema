package com.vibethema.model.logic;

import com.vibethema.model.CharacterData;
import com.vibethema.model.mystic.Charm;
import com.vibethema.model.mystic.MartialArtsCharm;
import com.vibethema.model.traits.Ability;

/** Shared eligibility and repeat limits for purchase actions and creation validation. */
public final class CharmPurchaseRules {
    private CharmPurchaseRules() {}

    public static boolean isStackable(Charm charm) {
        return charm.getKeywords() != null && charm.getKeywords().contains("Stackable");
    }

    public static int purchaseLimit(Charm charm, CharacterData data) {
        if (!isStackable(charm)) return 1;
        return "Ox-Body Technique".equals(charm.getName())
                ? data.getAbility(Ability.RESISTANCE).get()
                : 10;
    }

    public static boolean meetsRequirements(Charm charm, CharacterData data) {
        return charm.isEligible(data)
                && (!(charm instanceof MartialArtsCharm) || MeritRules.hasMartialArtist(data));
    }

    public static boolean canPurchase(Charm charm, CharacterData data) {
        return meetsRequirements(charm, data)
                && data.getCharmCount(charm.getId()) < purchaseLimit(charm, data);
    }
}
