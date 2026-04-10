package com.vibethema.model.logic;

import com.vibethema.model.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.traits.*;
import java.util.ArrayList;
import java.util.List;

public class ExperienceRuleEngine {

    public static class ExperienceStatus {
        public int totalRegularXpAwarded;
        public int totalSolarXpAwarded;

        public int regularXpSpent;
        public int solarXpSpent;

        public List<ExperiencePurchase> log = new ArrayList<>();

        public int getRegularXpRemaining() {
            return totalRegularXpAwarded - regularXpSpent;
        }

        public int getSolarXpRemaining() {
            return totalSolarXpAwarded - solarXpSpent;
        }
    }

    public static ExperienceStatus calculateStatus(
            CharacterData current, CharacterSaveState snapshot) {
        ExperienceStatus status = new ExperienceStatus();

        // Tally awarded XP
        if (current.getXpAwards() != null) {
            for (XpAward award : current.getXpAwards()) {
                if (award.isSolar()) {
                    status.totalSolarXpAwarded += award.getAmount();
                } else {
                    status.totalRegularXpAwarded += award.getAmount();
                }
            }
        }

        if (snapshot == null) {
            return status; // Cannot calculate spend without a baseline
        }

        // Tally Trait Costs
        List<ExperiencePurchase> purchases = new ArrayList<>();

        // 1. Attributes
        for (Attribute attr : SystemData.ATTRIBUTES) {
            int currentVal = current.getAttribute(attr).get();
            int snapVal =
                    snapshot.attributes != null
                            ? snapshot.attributes.getOrDefault(attr.name(), 1)
                            : 1;

            for (int i = snapVal; i < currentVal; i++) {
                int cost = i * 4;
                purchases.add(
                        new ExperiencePurchase(
                                attr.getDisplayName() + " " + i + "->" + (i + 1), cost, true));
            }
        }

        // 2. Abilities
        for (Ability abil : SystemData.ABILITIES) {
            int currentVal = current.getAbility(abil).get();
            int snapVal =
                    snapshot.abilities != null
                            ? snapshot.abilities.getOrDefault(abil.name(), 0)
                            : 0;
            boolean isFavored =
                    current.getCasteAbility(abil).get() || current.getFavoredAbility(abil).get();

            for (int i = snapVal; i < currentVal; i++) {
                int cost;
                if (i == 0) {
                    cost = 3; // New Ability cost
                } else {
                    cost = isFavored ? (i * 2) - 1 : (i * 2);
                }
                purchases.add(
                        new ExperiencePurchase(
                                abil.getDisplayName() + " " + i + "->" + (i + 1), cost, true));
            }
        }

        // 3. Crafts
        for (CraftAbility craft : current.getCrafts()) {
            boolean isFavored = craft.isCaste() || craft.isFavored();
            int currentVal = craft.getRating();
            int snapVal = 0;
            if (snapshot.crafts != null) {
                for (CharacterSaveState.CraftData cd : snapshot.crafts) {
                    if (cd.expertise.equals(craft.getExpertise())) {
                        snapVal = cd.rating;
                        break;
                    }
                }
            }

            for (int i = snapVal; i < currentVal; i++) {
                int cost;
                if (i == 0) {
                    cost = 3;
                } else {
                    cost = isFavored ? (i * 2) - 1 : (i * 2);
                }
                purchases.add(
                        new ExperiencePurchase(
                                "Craft (" + craft.getExpertise() + ") " + i + "->" + (i + 1),
                                cost,
                                true));
            }
        }

        // 4. Martial Arts
        for (MartialArtsStyle mas : current.getMartialArtsStyles()) {
            boolean isFavored = mas.isCaste() || mas.isFavored();
            int currentVal = mas.getRating();
            int snapVal = 0;
            if (snapshot.martialArts != null) {
                for (CharacterSaveState.MartialArtsData md : snapshot.martialArts) {
                    if (md.styleName.equals(mas.getStyleName())) {
                        snapVal = md.rating;
                        break;
                    }
                }
            }

            for (int i = snapVal; i < currentVal; i++) {
                int cost;
                if (i == 0) {
                    cost = 3;
                } else {
                    cost = isFavored ? (i * 2) - 1 : (i * 2);
                }
                purchases.add(
                        new ExperiencePurchase(
                                "Martial Arts (" + mas.getStyleName() + ") " + i + "->" + (i + 1),
                                cost,
                                true));
            }
        }

        // 5. Willpower
        int currentWp = current.willpowerProperty().get();
        int snapWp = snapshot.willpower > 0 ? snapshot.willpower : 5;
        for (int i = snapWp; i < currentWp; i++) {
            purchases.add(new ExperiencePurchase("Willpower " + i + "->" + (i + 1), 8, true));
        }

        // 6. Merits
        for (Merit currentMerit : current.getMerits()) {
            if (currentMerit.getName() == null || currentMerit.getName().trim().isEmpty()) continue;

            int currentRating = currentMerit.getRating();
            int snapRating = 0;
            if (snapshot.merits != null) {
                for (CharacterSaveState.MeritData md : snapshot.merits) {
                    if (md.name.equals(currentMerit.getName())) {
                        snapRating = md.rating;
                        break;
                    }
                }
            }

            if (currentRating > snapRating) {
                for (int i = snapRating; i < currentRating; i++) {
                    int cost = (i + 1) * 3;
                    purchases.add(
                            new ExperiencePurchase(
                                    "Merit: " + currentMerit.getName() + " " + i + "->" + (i + 1),
                                    cost,
                                    true));
                }
            }
        }

        // 7. Specialties
        int currentSpCount =
                (int)
                        current.getSpecialties().stream()
                                .filter(s -> s.getName() != null && !s.getName().trim().isEmpty())
                                .count();
        int snapSpCount = snapshot.specialties != null ? snapshot.specialties.size() : 0;
        int newSpCount = Math.max(0, currentSpCount - snapSpCount);
        for (int i = 0; i < newSpCount; i++) {
            purchases.add(new ExperiencePurchase("New Specialty", 3, true));
        }

        // 8. Charms and Evocations
        List<String> snapCharmIds = new ArrayList<>();
        if (snapshot.unlockedCharms != null) {
            for (PurchasedCharm snapPc : snapshot.unlockedCharms) {
                snapCharmIds.add(snapPc.id());
            }
        }

        for (PurchasedCharm pc : current.getUnlockedCharms()) {
            if (!snapCharmIds.contains(pc.id())) {
                String ability = pc.ability();
                Ability enumAbil = Ability.fromString(ability);

                boolean isFavored = false;
                boolean isSolarCharm = false;

                if (enumAbil != null) {
                    isFavored =
                            current.getCasteAbility(enumAbil).get()
                                    || current.getFavoredAbility(enumAbil).get();
                    isSolarCharm = true;
                } else {
                    // Check if it's a Craft ability
                    for (CraftAbility ca : current.getCrafts()) {
                        if (ca.getExpertise().equals(ability)) {
                            isFavored = ca.isCaste() || ca.isFavored();
                            isSolarCharm = true;
                            break;
                        }
                    }
                    if (!isSolarCharm) {
                        // Check if it's an MA style
                        for (MartialArtsStyle mas : current.getMartialArtsStyles()) {
                            if (mas.getStyleName().equals(ability)) {
                                isFavored = mas.isCaste() || mas.isFavored();
                                break;
                            }
                        }
                    }
                }

                int cost = isFavored ? 8 : 10;
                boolean canUseSolarXp = !isSolarCharm; // Evocations and MA charms can use Solar XP

                String desc =
                        isSolarCharm ? "Solar Charm: " + pc.name() : pc.name() + " (Evocation/MA)";
                purchases.add(new ExperiencePurchase(desc, cost, canUseSolarXp));
            }
        }

        // 9. Spells
        List<String> snapSpellIds = new ArrayList<>();
        if (snapshot.spells != null) {
            for (CharacterSaveState.SpellData sd : snapshot.spells) {
                snapSpellIds.add(sd.id);
            }
        }

        boolean isOccultFavored =
                current.getCasteAbility(Ability.OCCULT).get()
                        || current.getFavoredAbility(Ability.OCCULT).get();
        int spellCost = isOccultFavored ? 8 : 10;

        for (Spell spell : current.getSpells()) {
            if (!snapSpellIds.contains(spell.getId())) {
                // Determine if this spell is the free TCS spell
                boolean wasTCSFreeAtCreation =
                        snapshot.spells != null
                                && snapshot.spells.stream()
                                        .anyMatch(
                                                s ->
                                                        s.name.equals(
                                                                SystemData
                                                                        .TERRESTRIAL_CIRCLE_SORCERY));

                // If it wasn't bought at creation, maybe it's the free one now
                boolean isTCS = spell.getName().equals(SystemData.TERRESTRIAL_CIRCLE_SORCERY);
                boolean isFirstSpellAndTCS =
                        isTCS && current.getSpells().get(0).getId().equals(spell.getId());

                if (isFirstSpellAndTCS && !wasTCSFreeAtCreation) {
                    continue; // Skip calculating cost for the free TCS initiation spell
                }

                purchases.add(new ExperiencePurchase("Spell: " + spell.getName(), spellCost, true));
            }
        }

        // Calculate final spend pools
        int tempSolarPool = status.totalSolarXpAwarded;

        for (ExperiencePurchase purchase : purchases) {
            status.log.add(purchase);

            if (!purchase.canUseSolarXp()) {
                // Must use Regular XP
                status.regularXpSpent += purchase.getCost();
            } else {
                // Can use Solar XP. Prefer Solar XP if available.
                int cost = purchase.getCost();
                if (tempSolarPool >= cost) {
                    tempSolarPool -= cost;
                    status.solarXpSpent += cost;
                } else if (tempSolarPool > 0) {
                    // Exhaust remaining solar XP and use Regular for the rest
                    int remainder = cost - tempSolarPool;
                    status.solarXpSpent += tempSolarPool;
                    tempSolarPool = 0;
                    status.regularXpSpent += remainder;
                } else {
                    // No solar XP left, use regular
                    status.regularXpSpent += cost;
                }
            }
        }

        return status;
    }
}
