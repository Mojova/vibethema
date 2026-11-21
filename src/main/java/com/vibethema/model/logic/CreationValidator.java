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

    /** Optional ability selects the appropriate Charm tree before highlighting its node. */
    public record Location(String tab, String elementId, String ability) {
        public Location(String tab, String elementId) {
            this(tab, elementId, null);
        }
    }

    public record Issue(String code, String message, Location location) {
        public Issue(String code, String message) {
            this(code, message, defaultLocation(code));
        }
    }

    private static Location defaultLocation(String code) {
        return switch (code) {
            case "merit_pool", "martial_artist" -> new Location("Merits", "merits_add");
            case "intimacies", "defining_intimacy", "major_intimacy" ->
                    new Location("Intimacies", "intimacies_add");
            case "caste" -> new Location("Stats", "info_caste");
            case "supernal" -> new Location("Stats", "info_supernal");
            case "limit_trigger" -> new Location("Stats", "stats_limit_trigger");
            case "essence" -> new Location("Stats", "stats_essence");
            case "attribute_priorities" -> new Location("Stats", "priority_physical");
            case "specialty_pool" -> new Location("Stats", "stats_specialties");
            case "sorcery" -> new Location("Solar Charms", "charms_filter", "Occult");
            case "spell_circle" -> new Location("Sorcery", "sorcery_content");
            case "charm_pool", "charm_definition" -> new Location("Solar Charms", "charms_filter");
            case "ability_pool", "caste_abilities", "favored_abilities" ->
                    new Location("Stats", "stats_abilities");
            default -> new Location("Stats", "stats_content");
        };
    }

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
        return validate(data, charmLookup, null);
    }

    /** Catalog-aware validation used by both the checklist and finalization guard. */
    public static Result validate(
            CharacterData data,
            Function<String, Charm> charmLookup,
            Function<String, MeritReference> meritLookup) {
        List<Issue> issues = new ArrayList<>(validate(data).issues());
        if (meritLookup != null) {
            issues.removeIf(i -> i.code().startsWith("merit_"));
            validateMerits(data, meritLookup, issues);
        }
        Set<String> checked = new HashSet<>();
        for (PurchasedCharm purchase : data.getUnlockedCharms()) {
            if (!checked.add(purchase.id())) continue;
            Charm charm = charmLookup.apply(purchase.id());
            if (charm == null) {
                issues.add(
                        new Issue(
                                "charm_definition",
                                "Restore the definition for " + purchase.name() + " or refund it.",
                                new Location("Solar Charms", "charms_filter")));
            } else {
                if (!CharmPurchaseRules.meetsRequirements(charm, data)) {
                    issues.add(
                            new Issue(
                                    "charm_requirements",
                                    "Meet the requirements for "
                                            + purchase.name()
                                            + " or refund it.",
                                    charmLocation(charm, data)));
                }
                int count = data.getCharmCount(purchase.id());
                int limit = CharmPurchaseRules.purchaseLimit(charm, data);
                if (count > limit) {
                    issues.add(
                            new Issue(
                                    "charm_limit",
                                    "Reduce " + purchase.name() + " purchases to " + limit + ".",
                                    charmLocation(charm, data)));
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
                                            + " Attributes.",
                                    new Location(
                                            "Stats",
                                            "priority_"
                                                    + category.name().toLowerCase(Locale.ROOT))));
                }
            }
        }
        for (Attribute attribute : SystemData.ATTRIBUTES) {
            checkRating(
                    issues,
                    attribute.getDisplayName(),
                    data.getAttribute(attribute).get(),
                    1,
                    5,
                    new Location(
                            "Stats", "attribute_" + attribute.name().toLowerCase(Locale.ROOT)));
        }
        // Only dots at ratings 1-3 may use the free Ability pool.
        requirePool(issues, "ability_pool", "free Ability dots", status.abilityDots, 28);
        validateMerits(data, null, issues);
        if (data.getMartialArtsStyles().stream().anyMatch(style -> style.getRating() > 0)
                && !MeritRules.hasMartialArtist(data)) {
            issues.add(
                    new Issue(
                            "martial_artist",
                            "Purchase the four-dot Martial Artist Merit or remove your Martial Arts"
                                    + " ratings."));
        }
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
                                        + " is not available to the selected Caste.",
                                abilityLocation(ability)));
            }
            if (data.getCasteAbility(ability).get() && data.getFavoredAbility(ability).get()) {
                issues.add(
                        new Issue(
                                "ability_selection",
                                ability.getDisplayName() + " cannot be both Caste and Favored.",
                                abilityLocation(ability)));
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
                                        + ".",
                                abilityLocation(ability)));
            }
            if (ability != Ability.CRAFT)
                checkRating(
                        issues,
                        ability.getDisplayName(),
                        data.getAbility(ability).get(),
                        0,
                        5,
                        abilityLocation(ability));
        }
        data.getCrafts()
                .forEach(craft ->
                        checkRating(
                                issues,
                                craft.getExpertise(),
                                craft.getRating(),
                                0,
                                5,
                                new Location("Stats", "craft_" + craft.getId())));
        data.getMartialArtsStyles()
                .forEach(style ->
                        checkRating(
                                issues,
                                style.getStyleName(),
                                style.getRating(),
                                0,
                                5,
                                new Location("Martial Arts", "martial_style_" + style.getId())));
        Ability supernal = Ability.fromString(data.supernalAbilityProperty().get());
        if (supernal == null || !data.getCasteAbility(supernal).get()) {
            issues.add(
                    new Issue("supernal", "Select a Supernal from your chosen Caste Abilities."));
        }
        if (data.essenceProperty().get() != 1)
            issues.add(new Issue("essence", "Starting Essence must be 1."));
        checkRating(
                issues,
                "Willpower",
                data.willpowerProperty().get(),
                5,
                10,
                new Location("Stats", "stats_willpower"));
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

    private static Location charmLocation(Charm charm, CharacterData data) {
        if (charm instanceof MartialArtsCharm && !MeritRules.hasMartialArtist(data))
            return defaultLocation("martial_artist");
        return new Location("Solar Charms", "charm_" + charm.getId(), charm.getAbility());
    }

    private static Location abilityLocation(Ability ability) {
        return new Location("Stats", "ability_" + ability.name().toLowerCase(Locale.ROOT));
    }

    private static void validateMerits(
            CharacterData data, Function<String, MeritReference> lookup, List<Issue> issues) {
        int validDots = 0;
        for (Merit merit : data.getMerits()) {
            Location location = new Location("Merits", "merit_" + merit.getId());
            boolean valid = true;
            if (blank(merit.getName())) {
                issues.add(
                        new Issue("merit_name", "Give this Merit a name or remove it.", location));
                valid = false;
            }
            boolean custom = blank(merit.getDefinitionId());
            MeritReference definition =
                    custom || lookup == null ? null : lookup.apply(merit.getDefinitionId());
            if (!custom && lookup != null && definition == null) {
                issues.add(
                        new Issue(
                                "merit_definition",
                                "Restore the definition for " + merit.getName() + " or remove it.",
                                location));
                valid = false;
            }
            if (definition != null
                    && definition.getRatings() != null
                    && !definition.getRatings().isEmpty()) {
                if (!definition.getRatings().contains(merit.getRating())) {
                    issues.add(
                            new Issue(
                                    "merit_rating",
                                    merit.getName()
                                            + " must use one of these ratings: "
                                            + definition.getRatings()
                                            + ".",
                                    location));
                    valid = false;
                }
            } else if (merit.getRating() < (custom ? 1 : 0) || merit.getRating() > 5) {
                issues.add(
                        new Issue(
                                "merit_rating",
                                merit.getName() + " must be rated " + (custom ? "1" : "0") + "-5.",
                                location));
                valid = false;
            }
            if (valid) validDots += merit.getRating();
        }
        requirePool(issues, "merit_pool", "valid Merit dots", validDots, 10);
    }

    private static void requirePool(
            List<Issue> issues, String code, String name, int spent, int required) {
        if (spent < required)
            issues.add(new Issue(code, "Choose " + (required - spent) + " more " + name + "."));
    }

    private static void checkRating(
            List<Issue> issues, String name, int rating, int min, int max, Location location) {
        if (rating < min || rating > max)
            issues.add(
                    new Issue(
                            "rating", name + " must be rated " + min + "-" + max + ".", location));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
