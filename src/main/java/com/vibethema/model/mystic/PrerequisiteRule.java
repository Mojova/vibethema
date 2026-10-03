package com.vibethema.model.mystic;

import com.vibethema.model.CharacterData;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Structured prerequisite expression used by imported and custom Charms. */
public class PrerequisiteRule {
    private String type;
    private String charmId;
    private List<PrerequisiteRule> requirements = new ArrayList<>();
    private List<PrerequisiteRule> options = new ArrayList<>();
    private List<String> charmIds = new ArrayList<>();
    private List<String> abilities = new ArrayList<>();
    private int count;
    private String predicate;

    public static PrerequisiteRule allOf(List<PrerequisiteRule> requirements) {
        PrerequisiteRule rule = new PrerequisiteRule();
        rule.type = "allOf";
        rule.requirements = requirements == null ? new ArrayList<>() : requirements;
        return rule;
    }

    public static PrerequisiteRule anyOf(List<PrerequisiteRule> options) {
        PrerequisiteRule rule = new PrerequisiteRule();
        rule.type = "anyOf";
        rule.options = options == null ? new ArrayList<>() : options;
        return rule;
    }

    public static PrerequisiteRule charm(String id) {
        PrerequisiteRule rule = new PrerequisiteRule();
        rule.type = "charm";
        rule.charmId = id;
        return rule;
    }

    public static PrerequisiteRule countOf(List<String> ids, int count) {
        PrerequisiteRule rule = new PrerequisiteRule();
        rule.type = "countOf";
        rule.charmIds = ids == null ? new ArrayList<>() : ids;
        rule.count = count;
        return rule;
    }

    public static PrerequisiteRule countFromAbilities(List<String> abilities, int count) {
        PrerequisiteRule rule = new PrerequisiteRule();
        rule.type = "countFromAbilities";
        rule.abilities = abilities == null ? new ArrayList<>() : abilities;
        rule.count = count;
        return rule;
    }

    public static PrerequisiteRule countFromSingleAbility(int count) {
        PrerequisiteRule rule = new PrerequisiteRule();
        rule.type = "countFromSingleAbility";
        rule.count = count;
        return rule;
    }

    public static PrerequisiteRule predicate(String predicate, int count) {
        PrerequisiteRule rule = new PrerequisiteRule();
        rule.type = "countMatchingPredicate";
        rule.predicate = predicate;
        rule.count = count;
        return rule;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCharmId() { return charmId; }
    public void setCharmId(String charmId) { this.charmId = charmId; }
    public List<PrerequisiteRule> getRequirements() { return requirements; }
    public void setRequirements(List<PrerequisiteRule> requirements) { this.requirements = requirements; }
    public List<PrerequisiteRule> getOptions() { return options; }
    public void setOptions(List<PrerequisiteRule> options) { this.options = options; }
    public List<String> getCharmIds() { return charmIds; }
    public void setCharmIds(List<String> charmIds) { this.charmIds = charmIds; }
    public List<String> getAbilities() { return abilities; }
    public void setAbilities(List<String> abilities) { this.abilities = abilities; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public String getPredicate() { return predicate; }
    public void setPredicate(String predicate) { this.predicate = predicate; }

    public boolean isSatisfied(CharacterData data) {
        if (data == null) return false;
        return switch (type == null ? "allOf" : type) {
            case "charm" -> data.hasCharm(charmId);
            case "allOf" -> requirements == null || requirements.stream().allMatch(r -> r.isSatisfied(data));
            case "anyOf" -> options != null && options.stream().anyMatch(r -> r.isSatisfied(data));
            case "countOf" -> countDistinctOwned(data, charmIds) >= count;
            case "countFromAbilities" -> countDistinctOwnedFromAbilities(data, abilities) >= count;
            case "countFromSingleAbility" -> maxDistinctOwnedInOneAbility(data) >= count;
            // Principle matching is deliberately deferred to the purchase UI/storyteller.
            case "countMatchingPredicate" -> false;
            default -> false;
        };
    }

    /** Returns whether a satisfied prerequisite path can receive the Caste/Favored discount. */
    public boolean qualifiesForCasteFavoredDiscount(CharacterData data) {
        if (data == null) return false;
        return switch (type == null ? "allOf" : type) {
            case "charm" -> isDiscountableCharm(data, charmId);
            case "allOf" -> requirements == null || requirements.stream()
                    .allMatch(r -> r.qualifiesForCasteFavoredDiscount(data));
            case "anyOf" -> options != null && options.stream()
                    .anyMatch(r -> r.isSatisfied(data) && r.qualifiesForCasteFavoredDiscount(data));
            case "countOf" -> countOfDiscountable(data, charmIds) >= count;
            case "countFromAbilities" -> countFromAbilitiesDiscountable(data, abilities) >= count;
            case "countFromSingleAbility" -> singleAbilityDiscountable(data) >= count;
            case "countMatchingPredicate" -> false;
            default -> false;
        };
    }

    public void collectCharmIds(Set<String> ids) {
        if (ids == null) return;
        if ("charm".equals(type) && charmId != null) ids.add(charmId);
        if (charmIds != null) ids.addAll(charmIds);
        if (requirements != null) requirements.forEach(r -> r.collectCharmIds(ids));
        if (options != null) options.forEach(r -> r.collectCharmIds(ids));
    }

    private static int countDistinctOwned(CharacterData data, List<String> ids) {
        if (ids == null) return 0;
        int count = 0;
        for (String id : new HashSet<>(ids)) if (data.hasCharm(id)) count++;
        return count;
    }

    private static int countDistinctOwnedFromAbilities(CharacterData data, List<String> abilities) {
        if (abilities == null) return 0;
        Set<String> allowed = new HashSet<>();
        for (String ability : abilities) allowed.add(ability.toLowerCase());
        Set<String> owned = new HashSet<>();
        data.getUnlockedCharms().forEach(c -> {
            if (c.ability() != null && allowed.contains(c.ability().toLowerCase())) owned.add(c.id());
        });
        return owned.size();
    }

    private static int maxDistinctOwnedInOneAbility(CharacterData data) {
        java.util.Map<String, Set<String>> byAbility = new java.util.HashMap<>();
        data.getUnlockedCharms().forEach(c -> {
            if (c.ability() != null) byAbility.computeIfAbsent(c.ability(), k -> new HashSet<>()).add(c.id());
        });
        return byAbility.values().stream().mapToInt(Set::size).max().orElse(0);
    }

    private static int countOfDiscountable(CharacterData data, List<String> ids) {
        if (ids == null) return 0;
        int count = 0;
        for (String id : new HashSet<>(ids)) {
            if (data.hasCharm(id) && isDiscountableCharm(data, id)) count++;
        }
        return count;
    }

    private static int countFromAbilitiesDiscountable(CharacterData data, List<String> abilities) {
        if (abilities == null) return 0;
        Set<String> allowed = new HashSet<>();
        for (String ability : abilities) allowed.add(ability.toLowerCase());
        Set<String> owned = new HashSet<>();
        data.getUnlockedCharms().forEach(c -> {
            if (c.ability() != null && allowed.contains(c.ability().toLowerCase())
                    && isDiscountableAbility(data, c.ability())) owned.add(c.id());
        });
        return owned.size();
    }

    private static int singleAbilityDiscountable(CharacterData data) {
        java.util.Map<String, Set<String>> byAbility = new java.util.HashMap<>();
        data.getUnlockedCharms().forEach(c -> {
            if (c.ability() != null && isDiscountableAbility(data, c.ability()))
                byAbility.computeIfAbsent(c.ability(), k -> new HashSet<>()).add(c.id());
        });
        return byAbility.values().stream().mapToInt(Set::size).max().orElse(0);
    }

    private static boolean isDiscountableCharm(CharacterData data, String id) {
        return data.getUnlockedCharms().stream()
                .filter(c -> c.id().equals(id))
                .findFirst()
                .map(c -> isDiscountableAbility(data, c.ability()))
                .orElse(false);
    }

    private static boolean isDiscountableAbility(CharacterData data, String abilityName) {
        com.vibethema.model.traits.Ability ability = com.vibethema.model.traits.Ability.fromString(abilityName);
        if (ability == null && data.isMartialArtsStyle(abilityName)) {
            ability = com.vibethema.model.traits.Ability.MARTIAL_ARTS;
        }
        return ability != null
                && (data.getCasteAbility(ability).get() || data.getFavoredAbility(ability).get());
    }
}
