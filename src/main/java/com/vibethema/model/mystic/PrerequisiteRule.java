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
    private List<String> excludedCharmIds = new ArrayList<>();
    private List<List<String>> excludedPrerequisitePaths = new ArrayList<>();
    private boolean excludeExcellencies;
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

    public static PrerequisiteRule countFromAbilities(
            List<String> abilities, int count, List<String> excludedCharmIds,
            boolean excludeExcellencies) {
        PrerequisiteRule rule = countFromAbilities(abilities, count);
        rule.excludedCharmIds = excludedCharmIds == null ? new ArrayList<>() : excludedCharmIds;
        rule.excludeExcellencies = excludeExcellencies;
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
    public List<String> getExcludedCharmIds() { return excludedCharmIds; }
    public void setExcludedCharmIds(List<String> excludedCharmIds) { this.excludedCharmIds = excludedCharmIds; }
    public List<List<String>> getExcludedPrerequisitePaths() { return excludedPrerequisitePaths; }
    public void setExcludedPrerequisitePaths(List<List<String>> excludedPrerequisitePaths) {
        this.excludedPrerequisitePaths = excludedPrerequisitePaths;
    }
    public boolean isExcludeExcellencies() { return excludeExcellencies; }
    public void setExcludeExcellencies(boolean excludeExcellencies) { this.excludeExcellencies = excludeExcellencies; }
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
            case "countFromAbilities" -> countFromAbilities(data) >= count;
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
            case "countFromAbilities" -> countFromAbilitiesDiscountable(data) >= count;
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

    /** Returns how many owned Charms currently satisfy this count rule. */
    public int getSatisfiedCount(CharacterData data) {
        if (data == null) return 0;
        return switch (type == null ? "allOf" : type) {
            case "countOf" -> countDistinctOwned(data, charmIds);
            case "countFromAbilities" -> countFromAbilities(data);
            case "countFromSingleAbility" -> maxDistinctOwnedInOneAbility(data);
            default -> 0;
        };
    }

    private static int countDistinctOwned(CharacterData data, List<String> ids) {
        if (ids == null) return 0;
        int count = 0;
        for (String id : new HashSet<>(ids)) if (data.hasCharm(id)) count++;
        return count;
    }

    private int countFromAbilities(CharacterData data) {
        if (abilities == null) return 0;
        Set<String> allowed = new HashSet<>();
        for (String ability : abilities) allowed.add(ability.toLowerCase());
        Set<String> excluded = getExcludedIdsFor(data);
        Set<String> owned = new HashSet<>();
        data.getUnlockedCharms().forEach(c -> {
            if (c.ability() != null
                    && allowed.contains(c.ability().toLowerCase())
                    && !excluded.contains(c.id())
                    && !(excludeExcellencies && isExcellency(c.name()))) owned.add(c.id());
        });
        return owned.size();
    }

    private static boolean isExcellency(String name) {
        return name != null && name.toLowerCase().contains("excellency");
    }

    private Set<String> getExcludedIdsFor(CharacterData data) {
        Set<String> excluded = excludedCharmIds == null
                ? new HashSet<>()
                : new HashSet<>(excludedCharmIds);
        if (excludedPrerequisitePaths == null || excludedPrerequisitePaths.isEmpty()) return excluded;

        List<String> selectedPath = null;
        int bestOwnedCount = -1;
        for (List<String> path : excludedPrerequisitePaths) {
            if (path == null) continue;
            int ownedCount = (int) path.stream().filter(data::hasCharm).distinct().count();
            if (ownedCount > bestOwnedCount
                    || (ownedCount == bestOwnedCount
                            && (selectedPath == null || path.size() < selectedPath.size()))) {
                selectedPath = path;
                bestOwnedCount = ownedCount;
            }
        }
        if (selectedPath != null) {
            for (String id : selectedPath) if (data.hasCharm(id)) excluded.add(id);
        }
        return excluded;
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

    private int countFromAbilitiesDiscountable(CharacterData data) {
        if (abilities == null) return 0;
        Set<String> allowed = new HashSet<>();
        for (String ability : abilities) allowed.add(ability.toLowerCase());
        Set<String> excluded = getExcludedIdsFor(data);
        Set<String> owned = new HashSet<>();
        data.getUnlockedCharms().forEach(c -> {
            if (c.ability() != null && allowed.contains(c.ability().toLowerCase())
                    && !excluded.contains(c.id())
                    && !(excludeExcellencies && isExcellency(c.name()))
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
