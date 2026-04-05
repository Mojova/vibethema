package com.vibethema.model.logic;

import com.vibethema.model.*;
import java.util.*;

/**
 * Logic engine for character creation rules (Bonus Points, starting pools).
 */
public class CreationRuleEngine {

    public static class CreationStatus {
        public int physicalDots, socialDots, mentalDots;
        public int abilityDots;
        public int charmCount;
        public int meritDots;
        public int specialtyCount;
        public int bonusPointsSpent;
        public int personalMotes, peripheralMotes;
        public List<String> healthLevels;

        // Validation
        public boolean overBonusPoints;
        public int casteAbilities, favoredAbilities;

        // Detailed pool usage
        public int attributesSpent; // dots above 1
        public int abilitiesSpent; // total dots
        public int meritsSpent;    // total merit dots
        public int specialtiesSpent; // count 
        public int charmsSpent;      // count of charms/spells
        
        public boolean allAttributePrioritiesSet;
        public boolean isReadyToFinalize;
    }

    public static CreationStatus calculateStatus(CharacterData data) {
        CreationStatus status = new CreationStatus();
        
        // 1. Attributes (8/6/4)
        status.physicalDots = getAttributeTotal(data, SystemData.PHYSICAL_ATTRIBUTES);
        status.socialDots = getAttributeTotal(data, SystemData.SOCIAL_ATTRIBUTES);
        status.mentalDots = getAttributeTotal(data, SystemData.MENTAL_ATTRIBUTES);
        
        AttributePriority physPri = data.getAttributePriority(Attribute.Category.PHYSICAL).get();
        AttributePriority socPri = data.getAttributePriority(Attribute.Category.SOCIAL).get();
        AttributePriority mentPri = data.getAttributePriority(Attribute.Category.MENTAL).get();

        boolean allPrioritiesSet = (physPri != null && socPri != null && mentPri != null) &&
                                  (physPri != socPri && physPri != mentPri && socPri != mentPri);

        if (allPrioritiesSet) {
            status.bonusPointsSpent += Math.max(0, status.physicalDots - physPri.getFreeDots()) * (physPri == AttributePriority.TERTIARY ? 3 : 4);
            status.bonusPointsSpent += Math.max(0, status.socialDots - socPri.getFreeDots()) * (socPri == AttributePriority.TERTIARY ? 3 : 4);
            status.bonusPointsSpent += Math.max(0, status.mentalDots - mentPri.getFreeDots()) * (mentPri == AttributePriority.TERTIARY ? 3 : 4);
        } else {
            // Fallback to auto-fit for BP calculation ONLY, but validation will prevent finalization
            int[][] permutations = {{8, 6, 4}, {8, 4, 6}, {6, 8, 4}, {6, 4, 8}, {4, 8, 6}, {4, 6, 8}};
            int minAttrBP = Integer.MAX_VALUE;
            for (int[] p : permutations) {
                int cost = 0;
                cost += Math.max(0, status.physicalDots - p[0]) * (p[0] == 4 ? 3 : 4);
                cost += Math.max(0, status.socialDots - p[1]) * (p[1] == 4 ? 3 : 4);
                cost += Math.max(0, status.mentalDots - p[2]) * (p[2] == 4 ? 3 : 4);
                if (cost < minAttrBP) minAttrBP = cost;
            }
            status.bonusPointsSpent += minAttrBP;
        }
        status.allAttributePrioritiesSet = allPrioritiesSet;

        // 2. Abilities (28 dots, caps, BP)
        int mustBp = 0;
        List<Integer> poolDots = new ArrayList<>();
        for (Ability abil : SystemData.ABILITIES) {
            if (Ability.CRAFT == abil || Ability.MARTIAL_ARTS == abil) continue;
            int dots = data.getAbility(abil).get();
            boolean isFavored = data.getCasteAbility(abil).get() || data.getFavoredAbility(abil).get();
            int costPerDot = isFavored ? 1 : 2;
            for (int i = 1; i <= dots; i++) {
                if (i > 3) mustBp += costPerDot;
                else poolDots.add(costPerDot);
            }
        }
        for (CraftAbility ca : data.getCrafts()) {
            int dots = ca.getRating();
            int costPerDot = (ca.isCaste() || ca.isFavored()) ? 1 : 2;
            for (int i = 1; i <= dots; i++) {
                if (i > 3) mustBp += costPerDot;
                else poolDots.add(costPerDot);
            }
        }
        for (MartialArtsStyle mas : data.getMartialArtsStyles()) {
            int dots = mas.getRating();
            int costPerDot = (mas.isCaste() || mas.isFavored()) ? 1 : 2;
            for (int i = 1; i <= dots; i++) {
                if (i > 3) mustBp += costPerDot;
                else poolDots.add(costPerDot);
            }
        }
        Collections.sort(poolDots, Collections.reverseOrder());
        int freeLeft = 28;
        int optPoolBp = 0;
        for (int cost : poolDots) {
            if (freeLeft > 0) freeLeft--;
            else optPoolBp += cost;
        }
        status.abilityDots = 28 - freeLeft; 
        status.bonusPointsSpent += mustBp + optPoolBp;

        // 3. Merits & Specialties
        status.meritDots = data.getMerits().stream().mapToInt(Merit::getRating).sum();
        status.bonusPointsSpent += Math.max(0, status.meritDots - 10);
        
        status.specialtyCount = data.getSpecialties().size();
        status.bonusPointsSpent += Math.max(0, status.specialtyCount - 4);

        // 4. Willpower
        if (data.willpowerProperty().get() > 5) {
            status.bonusPointsSpent += (data.willpowerProperty().get() - 5) * 2;
        }

        // 5. Charms & Spells
        List<Integer> charmCosts = new ArrayList<>();
        for (PurchasedCharm pc : data.getUnlockedCharms()) {
            String ability = pc.ability();
            boolean isFavored = false;
            Ability enumAbil = Ability.fromString(ability);
            if (enumAbil != null) {
                isFavored = data.getCasteAbility(enumAbil).get() || data.getFavoredAbility(enumAbil).get();
            } else {
                // Check if it's a MA/Craft style
                for (MartialArtsStyle mas : data.getMartialArtsStyles()) {
                    if (mas.getStyleName().equals(ability)) {
                        isFavored = mas.isCaste() || mas.isFavored();
                        break;
                    }
                }
                for (CraftAbility ca : data.getCrafts()) {
                    if (ca.getExpertise().equals(ability)) {
                        isFavored = ca.isCaste() || ca.isFavored();
                        break;
                    }
                }
            }
            charmCosts.add(isFavored ? 4 : 5);
        }
        
        boolean hasTCS = data.hasCharmByName(SystemData.TERRESTRIAL_CIRCLE_SORCERY);
        int billableSpells = Math.max(0, data.getSpells().size() - (hasTCS ? 1 : 0));
        if (billableSpells > 0) {
            boolean isOccultFavored = data.getCasteAbility(Ability.OCCULT).get() || data.getFavoredAbility(Ability.OCCULT).get();
            int spellCost = isOccultFavored ? 4 : 5;
            for (int i = 0; i < billableSpells; i++) charmCosts.add(spellCost);
        }
        
        Collections.sort(charmCosts, Collections.reverseOrder());
        int freeCharmsLeft = 15;
        for (int cost : charmCosts) {
            if (freeCharmsLeft > 0) freeCharmsLeft--;
            else status.bonusPointsSpent += cost;
        }
        status.charmCount = (15 - freeCharmsLeft) + Math.max(0, charmCosts.size() - 15);

        // 6. Motes & Health
        int essence = data.essenceProperty().get();
        status.personalMotes = (essence * 3) + 10;
        status.peripheralMotes = (essence * 7) + 26;
        
        status.healthLevels = data.getHealthLevels();

        status.overBonusPoints = status.bonusPointsSpent > 15;
        status.casteAbilities = data.casteAbilityCountProperty().get();
        status.favoredAbilities = data.favoredAbilityCountProperty().get();

        // 7. Calculate "Spent" counts for each pool
        status.attributesSpent = status.physicalDots + status.socialDots + status.mentalDots; // dots above 1
        status.abilitiesSpent = data.getAbilities().values().stream().mapToInt(javafx.beans.property.IntegerProperty::get).sum();
        for (CraftAbility ca : data.getCrafts()) status.abilitiesSpent += ca.getRating();
        for (MartialArtsStyle mas : data.getMartialArtsStyles()) status.abilitiesSpent += mas.getRating();
        
        status.meritsSpent = status.meritDots;
        status.specialtiesSpent = status.specialtyCount;
        status.charmsSpent = data.getUnlockedCharms().size() + billableSpells;

        // 8. Finalization Eligibility
        // Requirements: 
        // - At least 18 attribute dots above 1 (8/6/4)
        // - At least 28 ability dots
        // - At least 10 merit dots
        // - At least 4 specialties
        // - At least 15 charms/spells
        // - Exactly 15 bonus points spent (strict interpretation of "all points spent")
        // - Caste/Favored counts met (5/5)
        
        status.isReadyToFinalize = 
            status.attributesSpent >= 18 &&
            status.abilitiesSpent >= 28 &&
            status.meritsSpent >= 10 &&
            status.specialtiesSpent >= 4 &&
            status.charmsSpent >= 15 &&
            status.bonusPointsSpent == 15 &&
            status.casteAbilities == 5 &&
            status.favoredAbilities == 5 &&
            status.allAttributePrioritiesSet;

        return status;
    }

    private static int getAttributeTotal(CharacterData data, List<Attribute> attrCategory) {
        return attrCategory.stream().mapToInt(attr -> data.getAttribute(attr).get() - 1).sum();
    }
}
