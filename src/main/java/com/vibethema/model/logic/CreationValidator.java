package com.vibethema.model.logic;

import com.vibethema.model.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.social.Intimacy;
import com.vibethema.model.traits.*;
import java.util.*;
import java.util.function.Function;

/** Shared, side-effect-free validation of the current Solar creation rules. */
public final class CreationValidator {
    private CreationValidator() {}

    public record Issue(String code, String message) {}

    public record Result(List<Issue> issues) {
        public Result {
            issues = List.copyOf(issues);
        }

        public boolean isReady() {
            return issues.isEmpty();
        }

        public String describe() {
            return issues.stream()
                    .map(Issue::message)
                    .collect(java.util.stream.Collectors.joining("\n"));
        }
    }

    /** Validates character-owned state, without requiring access to an imported catalog. */
    public static Result validate(CharacterData data) {
        return validate(data, CreationRuleEngine.calculateStatus(data));
    }

    /** Also rechecks every purchase against the current catalog, including missing definitions. */
    public static Result validate(CharacterData data, Function<String, Charm> charmLookup) {
        List<Issue> issues = new ArrayList<>(validate(data).issues());
        Set<String> checked = new HashSet<>();
        for (PurchasedCharm purchase : data.getUnlockedCharms()) {
            if (!checked.add(purchase.id())) continue;
            Charm charm = charmLookup.apply(purchase.id());
            if (charm == null) {
                issues.add(
                        new Issue(
                                "charm_definition",
                                "Restore the definition for "
                                        + purchase.name()
                                        + " or refund it."));
            } else {
                if (!charm.isEligible(data)) {
                    issues.add(
                            new Issue(
                                    "charm_requirements",
                                    "Meet the requirements for "
                                            + purchase.name()
                                            + " or refund it."));
                }
                int count = data.getCharmCount(purchase.id());
                boolean stackable =
                        charm.getKeywords() != null && charm.getKeywords().contains("Stackable");
                int limit =
                        !stackable
                                ? 1
                                : "Ox-Body Technique".equals(charm.getName())
                                        ? data.getAbility(Ability.RESISTANCE).get()
                                        : 10;
                if (count > limit) {
                    issues.add(
                            new Issue(
                                    "charm_limit",
                                    "Reduce " + purchase.name() + " purchases to " + limit + "."));
                }
            }
        }
        return new Result(issues);
    }

    // Package-private so the legacy CreationStatus readiness flag uses these same rules.
    static Result validate(CharacterData data, CreationRuleEngine.CreationStatus status) {
        List<Issue> issues = new ArrayList<>();
        if (data.getMode() != CharacterMode.CREATION) {
            issues.add(new Issue("mode", "Only characters in creation mode can be finalized."));
        }
        if (data.exaltTypeProperty().get() != ExaltType.SOLAR) {
            issues.add(
                    new Issue(
                            "exalt_type",
                            "Creation validation currently supports Solar characters only."));
        }
        if (!status.allAttributePrioritiesSet) {
            issues.add(
                    new Issue(
                            "attribute_priorities",
                            "Assign a different priority to each Attribute category."));
        } else {
            for (Attribute.Category category : Attribute.Category.values()) {
                int spent =
                        switch (category) {
                            case PHYSICAL -> status.physicalDots;
                            case SOCIAL -> status.socialDots;
                            case MENTAL -> status.mentalDots;
                        };
                int required = data.getAttributePriority(category).get().getFreeDots();
                if (spent < required) {
                    issues.add(
                            new Issue(
                                    "attribute_pool",
                                    "Spend "
                                            + (required - spent)
                                            + " more free dots in "
                                            + category.name().toLowerCase(Locale.ROOT)
                                            + " Attributes."));
                }
            }
        }
        for (Attribute attribute : SystemData.ATTRIBUTES) {
            checkRating(
                    issues, attribute.getDisplayName(), data.getAttribute(attribute).get(), 1, 5);
        }
        // Only dots at ratings 1-3 may use the free Ability pool.
        requirePool(issues, "ability_pool", "free Ability dots", status.abilityDots, 28);
        requirePool(issues, "merit_pool", "Merit dots", status.meritsSpent, 10);
        requirePool(issues, "specialty_pool", "Specialties", status.specialtiesSpent, 4);
        requirePool(issues, "charm_pool", "Charms or spells", status.charmsSpent, 15);
        if (status.bonusPointsSpent != 15) {
            issues.add(
                    new Issue(
                            "bonus_points",
                            status.bonusPointsSpent < 15
                                    ? "Spend "
                                            + (15 - status.bonusPointsSpent)
                                            + " remaining bonus points."
                                    : "Remove "
                                            + (status.bonusPointsSpent - 15)
                                            + " bonus points of purchases."));
        }
        Caste caste = data.casteProperty().get();
        if (caste == null || caste == Caste.NONE) {
            issues.add(new Issue("caste", "Select a Caste."));
        }
        if (status.casteAbilities != 5)
            issues.add(new Issue("caste_abilities", "Select exactly five Caste Abilities."));
        if (status.favoredAbilities != 5)
            issues.add(new Issue("favored_abilities", "Select exactly five Favored Abilities."));
        for (Ability ability : SystemData.ABILITIES) {
            if (ability == Ability.MARTIAL_ARTS) continue; // Shares its selection with Brawl.
            if (data.getCasteAbility(ability).get()
                    && (caste == null
                            || !SystemData.CASTE_OPTIONS
                                    .getOrDefault(caste, EnumSet.noneOf(Ability.class))
                                    .contains(ability))) {
                issues.add(
                        new Issue(
                                "caste_ability",
                                ability.getDisplayName()
                                        + " is not available to the selected Caste."));
            }
            if (data.getCasteAbility(ability).get() && data.getFavoredAbility(ability).get()) {
                issues.add(
                        new Issue(
                                "ability_selection",
                                ability.getDisplayName() + " cannot be both Caste and Favored."));
            }
            int rating =
                    ability == Ability.CRAFT
                            ? data.getCrafts().stream()
                                    .mapToInt(CraftAbility::getRating)
                                    .max()
                                    .orElse(0)
                            : data.getAbility(ability).get();
            if (ability == Ability.BRAWL)
                rating =
                        Math.max(
                                rating,
                                data.getMartialArtsStyles().stream()
                                        .mapToInt(MartialArtsStyle::getRating)
                                        .max()
                                        .orElse(0));
            if (data.getFavoredAbility(ability).get() && rating < 1) {
                issues.add(
                        new Issue(
                                "favored_rating",
                                "Assign at least one dot to Favored "
                                        + ability.getDisplayName()
                                        + "."));
            }
            if (ability != Ability.CRAFT)
                checkRating(issues, ability.getDisplayName(), data.getAbility(ability).get(), 0, 5);
        }
        data.getCrafts()
                .forEach(craft ->
                        checkRating(issues, craft.getExpertise(), craft.getRating(), 0, 5));
        data.getMartialArtsStyles()
                .forEach(style ->
                        checkRating(issues, style.getStyleName(), style.getRating(), 0, 5));
        Ability supernal = Ability.fromString(data.supernalAbilityProperty().get());
        if (supernal == null || !data.getCasteAbility(supernal).get()) {
            issues.add(
                    new Issue("supernal", "Select a Supernal from your chosen Caste Abilities."));
        }
        if (data.essenceProperty().get() != 1)
            issues.add(new Issue("essence", "Starting Essence must be 1."));
        checkRating(issues, "Willpower", data.willpowerProperty().get(), 5, 10);
        if (blank(data.limitTriggerProperty().get()))
            issues.add(new Issue("limit_trigger", "Choose a Limit Trigger."));
        List<Intimacy> intimacies =
                data.getIntimacies().stream().filter(i -> !blank(i.getName())).toList();
        requirePool(issues, "intimacies", "named Intimacies", intimacies.size(), 4);
        if (intimacies.stream().noneMatch(i -> i.getIntensity() == Intimacy.Intensity.DEFINING))
            issues.add(new Issue("defining_intimacy", "Choose at least one Defining Intimacy."));
        if (intimacies.stream().noneMatch(i -> i.getIntensity() == Intimacy.Intensity.MAJOR))
            issues.add(new Issue("major_intimacy", "Choose at least one Major Intimacy."));
        if (!data.getSpells().isEmpty()
                && !data.hasCharmByName(SystemData.TERRESTRIAL_CIRCLE_SORCERY))
            issues.add(
                    new Issue(
                            "sorcery",
                            "Purchase Terrestrial Circle Sorcery or remove your spells."));
        for (Spell spell : data.getSpells()) {
            if (Spell.Circle.CELESTIAL.name().equalsIgnoreCase(spell.getCircle())
                            && !data.hasCharmByName("Celestial Circle Sorcery")
                    || Spell.Circle.SOLAR.name().equalsIgnoreCase(spell.getCircle())
                            && !data.hasCharmByName("Solar Circle Sorcery")) {
                issues.add(
                        new Issue(
                                "spell_circle",
                                "Meet the sorcery circle requirement for "
                                        + spell.getName()
                                        + "."));
            }
        }
        return new Result(issues);
    }

    private static void requirePool(
            List<Issue> issues, String code, String name, int spent, int required) {
        if (spent < required)
            issues.add(new Issue(code, "Choose " + (required - spent) + " more " + name + "."));
    }

    private static void checkRating(List<Issue> issues, String name, int rating, int min, int max) {
        if (rating < min || rating > max)
            issues.add(new Issue("rating", name + " must be rated " + min + "-" + max + "."));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
