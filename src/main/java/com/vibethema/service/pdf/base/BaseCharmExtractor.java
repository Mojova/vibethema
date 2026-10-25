package com.vibethema.service.pdf.base;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vibethema.service.CharmDataService;
import com.vibethema.service.DefinitionId;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseCharmExtractor {
    protected static final List<String> ABILITIES =
            Arrays.asList(
                    "Archery",
                    "Athletics",
                    "Awareness",
                    "Brawl",
                    "Bureaucracy",
                    "Craft",
                    "Dodge",
                    "Integrity",
                    "Investigation",
                    "Larceny",
                    "Linguistics",
                    "Lore",
                    "Martial Arts",
                    "Medicine",
                    "Melee",
                    "Occult",
                    "Performance",
                    "Presence",
                    "Resistance",
                    "Ride",
                    "Sail",
                    "Socialize",
                    "Stealth",
                    "Survival",
                    "Thrown",
                    "War");

    protected final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    protected Path overrideOutputPath;
    protected String sourceId = "exalted-3e-core";

    public void setOverrideOutputPath(Path path) {
        this.overrideOutputPath = path;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public abstract void extractAndSave(String text, String suffix) throws IOException;

    protected String cleanDescription(String text) {
        // Remove page junk (headers, footers, page numbers)
        text = text.replaceAll("(?i)C\\s*H\\s*A\\s*P\\s*T\\s*E\\s*R\\s*S?\\s+\\d+", "");
        text = text.replaceAll("(?i)E\\s*X\\s*3", "");
        text = text.replaceAll("(?i)S\\s*O\\s*L\\s*A\\s*R\\s*C\\s*H\\s*A\\s*R\\s*M\\s*S", "");
        text =
                text.replaceAll(
                        "(?i)E\\s*X\\s*A\\s*L\\s*T\\s*E\\s*D\\s+T\\s*H\\s*I\\s*R\\s*D\\s+E\\s*D\\s*I\\s*T\\s*I\\s*O\\s*N",
                        "");

        // Remove standalone page numbers (including ones with spaces/dashes)
        text = text.replaceAll("\\n\\s*\\d{1,3}\\s*\\n", "\n");
        text = text.replaceAll("\\u00AD\\s*\\n", ""); // Soft hyphen at line end

        String[] lines = text.split("\n");
        List<String> cleanedLines = new ArrayList<>();
        StringBuilder currentPara = new StringBuilder();

        boolean skippingSidebar = false;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                // If the previous line ended with a hyphen, we assume the paragraph continues
                if (currentPara.length() > 0
                        && currentPara.charAt(currentPara.length() - 1) == '-') {
                    continue;
                }
                if (currentPara.length() > 0) {
                    cleanedLines.add(currentPara.toString().trim());
                    currentPara.setLength(0);
                }
                continue;
            }

            if (isSidebarLine(line)) {
                skippingSidebar = true;
                continue;
            }

            if (skippingSidebar) {
                // STOP skipping ONLY if we are SURE it's a continuation.
                // Case 1: line starts with lowercase letter.
                if (Character.isLowerCase(line.charAt(0))) {
                    skippingSidebar = false;
                }
                // Case 2: previous line ended in a hyphen and current line completes it?
                // This is risky, so we only do it if the line is not ALL CAPS.
                else if (currentPara.length() > 0
                        && currentPara.charAt(currentPara.length() - 1) == '-'
                        && !line.equals(line.toUpperCase())) {
                    skippingSidebar = false;
                } else {
                    continue;
                }
            }

            if (currentPara.length() > 0) {
                String prevContent = currentPara.toString().trim();
                char lastChar = prevContent.charAt(prevContent.length() - 1);

                // Handle hyphenated words split across lines
                // Heuristic: only remove hyphen if the next part starts with a lowercase letter
                if (lastChar == '-') {
                    if (Character.isLowerCase(line.charAt(0))) {
                        currentPara.setLength(currentPara.length() - 1);
                    }
                    currentPara.append(line);
                    continue;
                }

                boolean isBullet =
                        line.startsWith("•") || line.startsWith("- ") || line.startsWith("* ");
                boolean isContinuation = Character.isLowerCase(line.charAt(0)) || lastChar == ',';

                if (isBullet || (!isContinuation && ".!? :;".indexOf(lastChar) != -1)) {
                    cleanedLines.add(currentPara.toString().trim());
                    currentPara.setLength(0);
                    currentPara.append(line);
                } else {
                    currentPara.append(" ").append(line);
                }
            } else {
                currentPara.append(line);
            }
        }

        if (currentPara.length() > 0) {
            cleanedLines.add(currentPara.toString().trim());
        }

        // Final pass on block-level issues
        List<String> finalBlocks = new ArrayList<>();
        for (int i = 0; i < cleanedLines.size(); i++) {
            String block = cleanedLines.get(i);

            // Join fragmented page references/mid-sentence breaks that spanned multiple newlines
            // Example: "This\n\nattack" or "(p.\n\n240)"
            if (i < cleanedLines.size() - 1) {
                String nextBlock = cleanedLines.get(i + 1);
                boolean isPageRefFrag =
                        block.matches(".*[ (]p\\.?$") || block.matches(".*[ (]see p\\.?$");
                boolean startsWithNumber = nextBlock.matches("\\d+.*");

                if (isPageRefFrag && startsWithNumber) {
                    cleanedLines.set(i + 1, block + " " + nextBlock);
                    continue;
                }

                // If this block doesn't end in punctuation and the next block continues the
                // sentence
                if (!block.matches(".*[.!?:\";\\-»]$")
                        && Character.isLowerCase(nextBlock.charAt(0))) {
                    cleanedLines.set(i + 1, block + " " + nextBlock);
                    continue;
                }
            }
            finalBlocks.add(block);
        }

        // Trailing header detection: remove short, title-case/all-caps blocks at very end
        if (!finalBlocks.isEmpty()) {
            String last = finalBlocks.get(finalBlocks.size() - 1);
            if (last.length() < 45
                    && !last.endsWith(".")
                    && !last.endsWith("\"")
                    && !last.endsWith("»")) {
                // Check if it's likely a header (no lowercase OR Title-Case)
                long lowerCount = last.chars().filter(Character::isLowerCase).count();

                boolean isTitleCase = false;
                String[] words = last.split("\\s+");
                if (words.length > 0) {
                    int capWords = 0;
                    int significantWords = 0;
                    for (String w : words) {
                        if (w.length() > 2) {
                            significantWords++;
                            if (Character.isUpperCase(w.charAt(0))) capWords++;
                        }
                    }
                    if (significantWords > 0 && (double) capWords / significantWords >= 0.5) {
                        isTitleCase = true;
                    }
                }

                if (last.startsWith("•")) {
                    // Bullet points are never headers
                    isTitleCase = false;
                    lowerCount = -1; // Force keep
                }

                // An all-caps header must have at least one uppercase letter
                boolean isAllCaps = (lowerCount == 0 && last.matches(".*[A-Z].*"));

                if (isAllCaps || isTitleCase) {
                    finalBlocks.remove(finalBlocks.size() - 1);
                } else if (last.matches(".*[a-zA-Z0-9]$")) {
                    finalBlocks.set(finalBlocks.size() - 1, last + ".");
                }
            } else if (last.matches(".*[a-zA-Z0-9]$")) {
                // Add period to long blocks too if missing
                finalBlocks.set(finalBlocks.size() - 1, last + ".");
            }
        }

        return String.join("\n\n", finalBlocks);
    }

    protected boolean isSidebarLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return false;

        // Handle standalone page numbers and short page junk (e.g. "274", "EX3")
        if (trimmed.matches("\\d{1,3}")
                || (trimmed.toUpperCase().equals(trimmed)
                        && (trimmed.length() <= 3 || trimmed.matches("E\\s*X\\s*3")))) {
            return true;
        }

        String upper = trimmed.toUpperCase();
        // Catch sidebar headers like "ON HUNDRED SHADOW WAYS" or "ON SURPRISE ANTICIPATION METHOD"
        if (upper.startsWith("ON ") && upper.equals(trimmed) && trimmed.length() < 100) return true;

        // Catch all-caps headers
        if (upper.equals(trimmed) && trimmed.length() > 5 && trimmed.length() < 100) {
            if (upper.contains("CHAPTER")
                    || upper.contains("SOLAR")
                    || upper.contains("CHARMS")
                    || upper.contains("MERIT")
                    || upper.contains("FLAW")
                    || upper.contains("SUPERNATURAL")
                    || upper.contains("MASTERY")) return true;
        }

        // Common 3e formatting artifacts with spaced letters
        if (upper.matches("^\\s*C\\s*H\\s*A\\s*P\\s*T\\s*E\\s*R.*\\d+\\s*$")
                || upper.matches("^\\s*E\\s*X\\s*3\\s*$")
                || upper.matches("^\\s*S\\s*O\\s*L\\s*A\\s*R\\s*C\\s*H\\s*A\\s*R\\s*M\\s*S\\s*$"))
            return true;

        return false;
    }

    protected void resolvePrerequisites(Collection<Map<String, Object>> charms) {
        Set<String> allExtractedIds = new HashSet<>();
        Map<String, String> idsByCompactAbilityAndName = new HashMap<>();
        Map<String, String> idsByCompactName = new HashMap<>();
        Map<String, String> namesByCompactAbilityAndName = new HashMap<>();
        charms.forEach(
                c -> {
                    allExtractedIds.add((String) c.get("id"));
                    String ability = (String) c.get("ability");
                    String name = (String) c.get("name");
                    String charmId = (String) c.get("id");
                    if (ability != null && name != null) {
                        String key = compactCharmKey(ability, name);
                        String existingId = idsByCompactAbilityAndName.putIfAbsent(key, charmId);
                        if (existingId != null) {
                            idsByCompactAbilityAndName.put(key, "");
                            namesByCompactAbilityAndName.put(key, "");
                        } else {
                            namesByCompactAbilityAndName.put(key, name);
                        }
                        String nameKey = compactCharmName(name);
                        String existingGlobalId = idsByCompactName.putIfAbsent(nameKey, charmId);
                        if (existingGlobalId != null && !existingGlobalId.equals(charmId)) {
                            idsByCompactName.put(nameKey, "");
                        }
                    }
                });

        for (Map<String, Object> charm : charms) {
            String ability = (String) charm.get("ability");
            normalizeRawPrerequisiteSpacing(charm, ability, namesByCompactAbilityAndName);

            @SuppressWarnings("unchecked")
            List<Object> rawPrereqs = (List<Object>) charm.get("prerequisites");
            List<String> warnings = new ArrayList<>();
            Map<String, Object> resolved =
                    resolveGroups(
                            rawPrereqs,
                            ability,
                            allExtractedIds,
                            idsByCompactAbilityAndName,
                            idsByCompactName,
                            charms,
                            (String) charm.get("id"),
                            warnings);
            @SuppressWarnings("unchecked")
            Map<String, Object> bridge = (Map<String, Object>) charm.get("bridgePrerequisite");
            String prerequisiteText = (String) charm.get("prerequisiteText");
            @SuppressWarnings("unchecked")
            List<String> keywords = (List<String>) charm.getOrDefault("keywords", List.of());
            if (keywords.stream().anyMatch(k -> k.equalsIgnoreCase("Bridge"))) {
                Map<String, Object> rawBridge = extractBridgeFromRawData((String) charm.get("rawData"));
                if (rawBridge != null) {
                    bridge = rawBridge;
                    warnings.clear();
                }
            }
            if (bridge == null && prerequisiteText != null
                    && keywords.stream().anyMatch(k -> k.equalsIgnoreCase("Bridge"))) {
                Matcher matcher = Pattern.compile("(?i)^(.+?)\\s+or\\s+any\\s+(\\d+)\\s+(.+)$")
                        .matcher(prerequisiteText.replaceAll("[-\\u00AD]\\s*(?=[a-z])", ""));
                if (matcher.find()) {
                    bridge = new LinkedHashMap<>();
                    bridge.put("normal", matcher.group(1).trim());
                    bridge.put("alternate", "any " + matcher.group(2) + " " + matcher.group(3).trim());
                }
            }
            if (bridge == null) bridge = detectBridgePrerequisite(rawPrereqs);
            if (bridge != null) {
                warnings.clear();
                Map<String, Object> normal =
                        ruleFromText(
                                (String) bridge.get("normal"),
                                ability,
                                allExtractedIds,
                                idsByCompactAbilityAndName,
                                idsByCompactName,
                                warnings);
                Map<String, Object> alternate =
                        ruleFromText(
                                (String) bridge.get("alternate"),
                                ability,
                                allExtractedIds,
                                idsByCompactAbilityAndName,
                                idsByCompactName,
                                warnings);
                Map<String, Object> any = new LinkedHashMap<>();
                any.put("type", "anyOf");
                any.put("options", List.of(normal, alternate));
                resolved = any;
            }
            charm.remove("prerequisites"); // No backwards comp
            charm.remove("bridgePrerequisite");
            charm.remove("prerequisiteText");
            charm.put("prerequisites", resolved);
            if (!warnings.isEmpty()) charm.put("importWarnings", warnings);
        }

        expandCharmExclusions(charms);
    }

    private void expandCharmExclusions(Collection<Map<String, Object>> charms) {
        Map<String, Map<String, Object>> byId = new HashMap<>();
        for (Map<String, Object> charm : charms) byId.put((String) charm.get("id"), charm);

        for (Map<String, Object> charm : charms) {
            @SuppressWarnings("unchecked")
            Map<String, Object> prerequisites =
                    (Map<String, Object>) charm.get("prerequisites");
            expandRuleExclusions(prerequisites, byId);
        }
    }

    private void expandRuleExclusions(
            Map<String, Object> rule, Map<String, Map<String, Object>> charmsById) {
        if (rule == null) return;
        if ("countFromAbilities".equals(rule.get("type"))
                && Boolean.TRUE.equals(rule.get("excludeExcellencies"))) {
            @SuppressWarnings("unchecked")
            List<String> excludedIds =
                    (List<String>) rule.getOrDefault("excludedCharmIds", List.of());
            List<List<String>> paths = List.of(List.of());
            for (String excludedId : excludedIds) {
                Map<String, Object> excludedCharm = charmsById.get(excludedId);
                if (excludedCharm == null) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> prerequisiteRule =
                        (Map<String, Object>) excludedCharm.get("prerequisites");
                paths = combinePaths(paths, prerequisitePaths(prerequisiteRule, charmsById, new HashSet<>()));
            }
            rule.put("excludedPrerequisitePaths", paths);
        }

        for (String childKey : List.of("requirements", "options")) {
            Object children = rule.get(childKey);
            if (children instanceof List<?> list) {
                for (Object child : list) {
                    if (child instanceof Map<?, ?> childMap) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> typedChild = (Map<String, Object>) childMap;
                        expandRuleExclusions(typedChild, charmsById);
                    }
                }
            }
        }
    }

    private List<List<String>> prerequisitePaths(
            Map<String, Object> rule,
            Map<String, Map<String, Object>> charmsById,
            Set<String> visiting) {
        if (rule == null) return List.of(List.of());
        String type = (String) rule.get("type");
        if ("charm".equals(type) && rule.get("charmId") instanceof String charmId) {
            return pathsForCharm(charmId, charmsById, visiting);
        }
        if ("allOf".equals(type)) {
            List<List<String>> paths = List.of(List.of());
            for (Map<String, Object> child : childRules(rule, "requirements")) {
                paths = combinePaths(paths, prerequisitePaths(child, charmsById, visiting));
            }
            return paths;
        }
        if ("anyOf".equals(type)) {
            List<List<String>> paths = new ArrayList<>();
            for (Map<String, Object> child : childRules(rule, "options")) {
                paths.addAll(prerequisitePaths(child, charmsById, visiting));
            }
            return uniquePaths(paths);
        }
        if ("countOf".equals(type) && rule.get("charmIds") instanceof List<?> ids) {
            return chooseCharmPaths(stringIds(ids), number(rule.get("count")), charmsById, visiting);
        }
        if ("countFromAbilities".equals(type)) {
            Set<String> abilities = stringSet(rule.get("abilities"));
            Set<String> excluded = stringSet(rule.get("excludedCharmIds"));
            List<String> candidates = charmsById.values().stream()
                    .filter(charm -> abilities.contains(
                            String.valueOf(charm.get("ability")).toLowerCase(Locale.ROOT)))
                    .filter(charm -> !excluded.contains(charm.get("id")))
                    .filter(charm -> !Boolean.TRUE.equals(rule.get("excludeExcellencies"))
                            || !String.valueOf(charm.get("name")).toLowerCase(Locale.ROOT).contains("excellency"))
                    .map(charm -> (String) charm.get("id"))
                    .sorted()
                    .toList();
            return chooseCharmPaths(candidates, number(rule.get("count")), charmsById, visiting);
        }
        if ("countFromSingleAbility".equals(type)) {
            Map<String, List<String>> byAbility = new TreeMap<>();
            for (Map<String, Object> charm : charmsById.values()) {
                byAbility.computeIfAbsent(
                                String.valueOf(charm.get("ability")), key -> new ArrayList<>())
                        .add((String) charm.get("id"));
            }
            List<List<String>> paths = new ArrayList<>();
            for (List<String> candidates : byAbility.values()) {
                paths.addAll(chooseCharmPaths(
                        candidates, number(rule.get("count")), charmsById, visiting));
            }
            return uniquePaths(paths);
        }
        return List.of(List.of());
    }

    private List<List<String>> pathsForCharm(
            String charmId,
            Map<String, Map<String, Object>> charmsById,
            Set<String> visiting) {
        if (!visiting.add(charmId)) return List.of(List.of(charmId));
        Map<String, Object> charm = charmsById.get(charmId);
        if (charm == null) return List.of(List.of(charmId));
        @SuppressWarnings("unchecked")
        Map<String, Object> prerequisites = (Map<String, Object>) charm.get("prerequisites");
        List<List<String>> paths = new ArrayList<>();
        for (List<String> path : prerequisitePaths(prerequisites, charmsById, visiting)) {
            LinkedHashSet<String> completePath = new LinkedHashSet<>();
            completePath.add(charmId);
            completePath.addAll(path);
            paths.add(new ArrayList<>(completePath));
        }
        visiting.remove(charmId);
        return uniquePaths(paths);
    }

    private List<List<String>> chooseCharmPaths(
            List<String> candidates,
            int required,
            Map<String, Map<String, Object>> charmsById,
            Set<String> visiting) {
        List<List<String>> combinations = new ArrayList<>();
        chooseCombinations(candidates, required, 0, new ArrayList<>(), combinations);
        List<List<String>> paths = new ArrayList<>();
        for (List<String> combination : combinations) {
            List<List<String>> combinationPaths = List.of(List.of());
            for (String charmId : combination) {
                combinationPaths = combinePaths(
                        combinationPaths, pathsForCharm(charmId, charmsById, visiting));
            }
            paths.addAll(combinationPaths);
        }
        return paths.isEmpty() ? List.of(List.of()) : uniquePaths(paths);
    }

    private void chooseCombinations(
            List<String> candidates,
            int required,
            int start,
            List<String> selected,
            List<List<String>> result) {
        if (selected.size() == required) {
            result.add(new ArrayList<>(selected));
            return;
        }
        for (int index = start; index <= candidates.size() - (required - selected.size()); index++) {
            selected.add(candidates.get(index));
            chooseCombinations(candidates, required, index + 1, selected, result);
            selected.remove(selected.size() - 1);
        }
    }

    private List<List<String>> combinePaths(List<List<String>> left, List<List<String>> right) {
        List<List<String>> combined = new ArrayList<>();
        for (List<String> leftPath : left) {
            for (List<String> rightPath : right) {
                LinkedHashSet<String> merged = new LinkedHashSet<>(leftPath);
                merged.addAll(rightPath);
                combined.add(new ArrayList<>(merged));
            }
        }
        return uniquePaths(combined);
    }

    private List<List<String>> uniquePaths(List<List<String>> paths) {
        LinkedHashMap<String, List<String>> unique = new LinkedHashMap<>();
        for (List<String> path : paths) {
            List<String> normalized = new ArrayList<>(new LinkedHashSet<>(path));
            List<String> keyParts = new ArrayList<>(normalized);
            Collections.sort(keyParts);
            unique.putIfAbsent(String.join("|", keyParts), normalized);
        }
        return new ArrayList<>(unique.values());
    }

    private List<Map<String, Object>> childRules(Map<String, Object> rule, String key) {
        Object children = rule.get(key);
        if (!(children instanceof List<?> list)) return List.of();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object child : list) {
            if (child instanceof Map<?, ?> childMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typed = (Map<String, Object>) childMap;
                result.add(typed);
            }
        }
        return result;
    }

    private List<String> stringIds(List<?> values) {
        return values.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }

    private Set<String> stringSet(Object value) {
        if (!(value instanceof List<?> list)) return Set.of();
        return stringIds(list).stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
    }

    private int number(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }

    private Map<String, Object> resolveGroups(
            List<Object> raw,
            String ability,
            Set<String> allIds,
            Map<String, String> idsByCompactAbilityAndName,
            Map<String, String> idsByCompactName,
            Collection<Map<String, Object>> allCharms,
            String currentCharmId,
            List<String> warnings) {
        List<Map<String, Object>> rules = new ArrayList<>();
        if (raw != null) {
            for (Object value : raw) {
                if (value instanceof String) {
                    rules.add(
                            ruleFromCharmName(
                                    (String) value,
                                    ability,
                                    allIds,
                                    idsByCompactAbilityAndName,
                                    idsByCompactName,
                                    warnings));
                    continue;
                }
                @SuppressWarnings("unchecked") Map<String, Object> group = (Map<String, Object>) value;
                @SuppressWarnings("unchecked") Map<String, Object> explicitRule =
                        (Map<String, Object>) group.get("rule");
                if (explicitRule != null) {
                    rules.add(explicitRule);
                    continue;
                }
                @SuppressWarnings("unchecked") List<String> names = (List<String>) group.get("names");
                int count = ((Number) group.getOrDefault("minCount", 0)).intValue();
                if (names == null || names.isEmpty()) continue;
                String marker = names.get(0);
                if (marker.startsWith("__ANY_ABILITIES__:")) {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("type", "countFromAbilities");
                    r.put("abilities", Arrays.asList(marker.substring("__ANY_ABILITIES__:".length()).split("\\|")));
                    r.put("count", count);
                    rules.add(r);
                } else if (marker.startsWith("__ANY_ABILITY_ESSENCE__:")) {
                    String[] markerParts =
                            marker.substring("__ANY_ABILITY_ESSENCE__:".length()).split(":", 2);
                    int minimumEssence = Integer.parseInt(markerParts[0]);
                    String eligibleAbility = markerParts[1];
                    List<String> eligibleCharmIds =
                            allCharms.stream()
                                    .filter(
                                            candidate ->
                                                    eligibleAbility.equalsIgnoreCase(
                                                            (String) candidate.get("ability")))
                                    .filter(
                                            candidate ->
                                                    ((Number) candidate.getOrDefault("minEssence", 0))
                                                                    .intValue()
                                                            >= minimumEssence)
                                    .map(candidate -> (String) candidate.get("id"))
                                    .filter(id -> !id.equals(currentCharmId))
                                    .toList();
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("type", "countOf");
                    r.put("charmIds", eligibleCharmIds);
                    r.put("count", count);
                    rules.add(r);
                } else if (marker.startsWith("__ANY_SINGLE_ABILITY__:")) {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("type", "countFromSingleAbility");
                    r.put("count", Integer.parseInt(marker.substring("__ANY_SINGLE_ABILITY__:".length())));
                    rules.add(r);
                } else if (marker.startsWith("__PREDICATE__:")) {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("type", "countMatchingPredicate");
                    r.put("predicate", marker.substring("__PREDICATE__:".length()));
                    r.put("count", count);
                    rules.add(r);
                } else {
                    List<String> ids = new ArrayList<>();
                    for (String name : names) {
                        String id = DefinitionId.forCharm(sourceId, name.replaceAll("\\s+", " ").trim(), ability);
                        id =
                                resolveCharmId(
                                        name,
                                        ability,
                                        id,
                                        allIds,
                                        idsByCompactAbilityAndName,
                                        idsByCompactName);
                        ids.add(id);
                        if (!allIds.contains(id)) warnings.add("Unresolved prerequisite: " + name);
                    }
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("type", count > 0 ? "countOf" : "allOf");
                    if (count > 0) r.put("count", count);
                    if (count > 0) r.put("charmIds", ids);
                    else r.put("requirements", ids.stream().map(id -> Map.of("type", "charm", "charmId", id)).toList());
                    rules.add(r);
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "allOf");
        result.put("requirements", rules);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> detectBridgePrerequisite(List<Object> raw) {
        if (raw == null || raw.size() != 1 || !(raw.get(0) instanceof Map)) return null;
        Map<String, Object> group = (Map<String, Object>) raw.get(0);
        List<String> names = (List<String>) group.get("names");
        if (names == null || names.size() != 1) return null;
        Matcher matcher = Pattern.compile("(?i)^(.+?)\\s+or\\s+any\\s+(\\d+)\\s+(.+)$").matcher(names.get(0).trim());
        if (!matcher.find()) return null;
        Map<String, Object> bridge = new LinkedHashMap<>();
        bridge.put("normal", matcher.group(1).trim());
        bridge.put("alternate", "any " + matcher.group(2) + " " + matcher.group(3).trim());
        return bridge;
    }

    private Map<String, Object> extractBridgeFromRawData(String rawData) {
        if (rawData == null) return null;
        // The PDF text extractor wraps prerequisite lines arbitrarily.  A paragraph-boundary
        // heuristic is therefore unreliable: several descriptions begin with an uppercase word
        // and some prerequisite alternatives themselves continue onto a second line.  Bridge
        // alternatives in the source end with either "Charms" or "Principle", so capture the
        // first such terminator after the label and discard the description that follows it.
        Matcher header = Pattern.compile(
                        "(?is)Prerequisite Charms:\\s*(.*?(?:Charms\\s+that\\s+reflect.*?Principle|Charms|Principle))\\b")
                .matcher(rawData);
        if (!header.find()) return null;
        String text = header.group(1)
                .replaceAll("[-\\u00AD]\\s*(?=[a-z])", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (text.toLowerCase().startsWith("none ")) text = text.substring(5).trim();
        if (text.toLowerCase().startsWith("or any ")) text = "None " + text;
        Matcher bridge = Pattern.compile("(?i)^(.+?)\\s+or\\s+any\\s+(\\d+)\\s+(.+)$").matcher(text);
        if (!bridge.find()) return null;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("normal", bridge.group(1).trim());
        result.put("alternate", "any " + bridge.group(2) + " " + bridge.group(3).trim());
        return result;
    }

    private Map<String, Object> ruleFromText(
            String text,
            String ability,
            Set<String> allIds,
            Map<String, String> idsByCompactAbilityAndName,
            Map<String, String> idsByCompactName,
            List<String> warnings) {
        if (text == null || text.isBlank() || text.equalsIgnoreCase("none")) return Map.of("type", "allOf", "requirements", List.of());
        Matcher casteFavoredAbility =
                Pattern.compile("(?i)^(.+?)\\s+as\\s+Caste\\s+or\\s+Favored$")
                        .matcher(text.trim());
        if (casteFavoredAbility.find()) {
            String abilityName = casteFavoredAbility.group(1).trim();
            if (ABILITIES.stream().anyMatch(a -> a.equalsIgnoreCase(abilityName))) {
                String canonicalAbility =
                        ABILITIES.stream()
                                .filter(a -> a.equalsIgnoreCase(abilityName))
                                .findFirst()
                                .orElseThrow();
                return Map.of(
                        "type", "countMatchingPredicate",
                        "predicate", "abilityIsCasteOrFavored:" + canonicalAbility,
                        "count", 1);
            }
        }
        if (text.contains(",")
                && !text.toLowerCase().matches("(?s).*\\bany\\s+\\d+\\b.*")) {
            List<Map<String, Object>> requirements = new ArrayList<>();
            for (String part : text.split(",")) {
                if (!part.isBlank())
                    requirements.add(
                            ruleFromCharmName(
                                    part.trim(),
                                    ability,
                                    allIds,
                                    idsByCompactAbilityAndName,
                                    idsByCompactName,
                                    warnings));
            }
            return Map.of("type", "allOf", "requirements", requirements);
        }
        Matcher any = Pattern.compile("(?i)any (\\d+) (.+)$").matcher(text.trim());
        if (any.find()) {
            int count = Integer.parseInt(any.group(1));
            String scope = any.group(2).trim().replaceFirst("(?i)\\s+charms?$", "").trim();
            if (scope.equalsIgnoreCase("one ability") || scope.equalsIgnoreCase("any one ability")) {
                return Map.of("type", "countFromSingleAbility", "count", count);
            }
            if (scope.isEmpty() || scope.equalsIgnoreCase("charms")) {
                return Map.of("type", "countFromSingleAbility", "count", count);
            }
            if (scope.toLowerCase().contains("that reflect")) {
                return Map.of("type", "countMatchingPredicate", "predicate", "reflectsSelectedPrinciple", "count", count);
            }
            if (scope.equalsIgnoreCase("social")) scope = "Bureaucracy|Linguistics|Performance|Presence|Socialize";
            if (scope.equalsIgnoreCase("martial arts")) scope = "Martial Arts";
            if (scope.equalsIgnoreCase("caste")) return Map.of("type", "countMatchingPredicate", "predicate", "casteCharms", "count", count);
            scope = scope.replaceAll("(?i)\\s+or\\s+", "|");
            List<String> abilities =
                    Arrays.stream(scope.split("\\s*,\\s*|\\|"))
                            .map(String::trim)
                            .filter(abilityName -> !abilityName.isEmpty())
                            .toList();
            return Map.of("type", "countFromAbilities", "abilities", abilities, "count", count);
        }
        return ruleFromCharmName(
                text.trim(),
                ability,
                allIds,
                idsByCompactAbilityAndName,
                idsByCompactName,
                warnings);
    }

    private Map<String, Object> ruleFromCharmName(
            String name,
            String ability,
            Set<String> allIds,
            Map<String, String> idsByCompactAbilityAndName,
            Map<String, String> idsByCompactName,
            List<String> warnings) {
        String id = DefinitionId.forCharm(sourceId, name.replaceAll("\\s+", " ").trim(), ability);
        id = resolveCharmId(name, ability, id, allIds, idsByCompactAbilityAndName, idsByCompactName);
        if (!allIds.contains(id)) warnings.add("Unresolved prerequisite: " + name);
        return Map.of("type", "charm", "charmId", id);
    }

    private String resolveCharmId(
            String name,
            String ability,
            String id,
            Set<String> allIds,
            Map<String, String> idsByCompactAbilityAndName,
            Map<String, String> idsByCompactName) {
        if (allIds.contains(id) || ability == null || name == null) return id;
        String compactMatch = idsByCompactAbilityAndName.get(compactCharmKey(ability, name));
        if (compactMatch != null && !compactMatch.isEmpty()) return compactMatch;
        String globalMatch = idsByCompactName.get(compactCharmName(name));
        return globalMatch == null || globalMatch.isEmpty() ? id : globalMatch;
    }

    private String compactCharmKey(String ability, String name) {
        return ability.trim().toLowerCase(Locale.ROOT) + "|" + compactCharmName(name);
    }

    private String compactCharmName(String name) {
        return name.replaceAll("[-\\u00AD\\u2010-\\u2014\\s]+", "").toLowerCase(Locale.ROOT);
    }

    private void normalizeRawPrerequisiteSpacing(
            Map<String, Object> charm,
            String ability,
            Map<String, String> namesByCompactAbilityAndName) {
        String rawData = (String) charm.get("rawData");
        if (rawData == null || ability == null) return;
        Matcher matcher = Pattern.compile("(?m)(Prerequisite Charms:\\s*)([^\\r\\n]*)").matcher(rawData);
        if (!matcher.find()) return;
        String canonicalName =
                namesByCompactAbilityAndName.get(compactCharmKey(ability, matcher.group(2)));
        if (canonicalName == null || canonicalName.isEmpty()) return;
        String normalized =
                rawData.substring(0, matcher.start(2))
                        + canonicalName
                        + rawData.substring(matcher.end(2));
        charm.put("rawData", normalized);
    }

    protected void saveCharms(
            Map<String, List<Map<String, Object>>> charmsByAbility,
            String suffix,
            boolean isMartialArts)
            throws IOException {
        Path outDir =
                overrideOutputPath != null
                        ? overrideOutputPath.resolve("charms")
                        : (isMartialArts
                                ? CharmDataService.getUserMartialArtsPath()
                                : CharmDataService.getUserCharmsPath());
        if (isMartialArts && overrideOutputPath != null) outDir = outDir.resolve("martial_arts");
        if (!Files.exists(outDir)) Files.createDirectories(outDir);

        for (Map.Entry<String, List<Map<String, Object>>> entry : charmsByAbility.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            String filename = entry.getKey().toLowerCase().replace(" ", "-") + suffix + ".json";
            Path filePath = outDir.resolve(filename);
            try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                Map<String, Object> wrapper = new LinkedHashMap<>();
                wrapper.put("$schema", "./charm-schema.json");
                wrapper.put("version", "0.2.0");
                wrapper.put("sourceId", sourceId);
                if (isMartialArts) {
                    wrapper.put(
                            "id", DefinitionId.forMartialArtsStyle(sourceId, entry.getKey()));
                }
                wrapper.put("ability", entry.getKey());
                wrapper.put("type", isMartialArts ? "martialArts" : "solarAbility");
                wrapper.put("exalt", "solar");
                wrapper.put("charms", entry.getValue());
                gson.toJson(wrapper, writer);
            }
        }
    }

    private static class TransitiveMetadata {
        Set<String> mandatoryExcludedIds = new HashSet<>();
        int additionalMinCount = 0;
    }

    @SuppressWarnings("unchecked")
    private TransitiveMetadata calculateTransitiveMetadata(
            String targetName, String ability, Collection<Map<String, Object>> charms) {
        TransitiveMetadata meta = new TransitiveMetadata();
        String rootId = DefinitionId.forCharm(sourceId, targetName, ability);

        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(rootId);
        meta.mandatoryExcludedIds.add(rootId);

        while (!queue.isEmpty()) {
            String currId = queue.poll();
            if (visited.contains(currId)) continue;
            visited.add(currId);

            Map<String, Object> charm =
                    charms.stream()
                            .filter(c -> c.get("id").equals(currId))
                            .findFirst()
                            .orElse(null);
            if (charm == null) continue;

            List<Object> rawPrereqs = (List<Object>) charm.get("prerequisites");
            List<Map<String, Object>> resolvedGroups =
                    (List<Map<String, Object>>) charm.get("prerequisiteGroups");

            if ((rawPrereqs == null || rawPrereqs.isEmpty())
                    && (resolvedGroups == null || resolvedGroups.isEmpty())) continue;

            if (rawPrereqs != null && !rawPrereqs.isEmpty()) {
                if (rawPrereqs.get(0) instanceof String) {
                    // Mandatory simple list
                    for (Object p : rawPrereqs) {
                        String pId = DefinitionId.forCharm(sourceId, (String) p, ability);
                        meta.mandatoryExcludedIds.add(pId);
                        queue.add(pId);
                    }
                } else {
                    for (Object gObj : rawPrereqs) {
                        Map<String, Object> gMap = (Map<String, Object>) gObj;
                        List<String> names = (List<String>) gMap.get("names");
                        if (names == null) continue;

                        int minCount = (int) gMap.getOrDefault("minCount", 0);
                        boolean isMandatory = (minCount == 0 || minCount >= names.size());

                        for (String pName : names) {
                            String pId = DefinitionId.forCharm(sourceId, pName, ability);
                            if (isMandatory) {
                                meta.mandatoryExcludedIds.add(pId);
                                queue.add(pId);
                            } else {
                                queue.add(pId); // Recurse to find transitive mandatory prereqs
                            }
                        }
                        if (!isMandatory) {
                            meta.additionalMinCount += minCount;
                        }
                    }
                }
            } else if (resolvedGroups != null) {
                for (Map<String, Object> group : resolvedGroups) {
                    List<String> ids = (List<String>) group.get("charmIds");
                    if (ids == null) continue;

                    int minCount = (int) group.getOrDefault("minCount", 0);
                    boolean isMandatory = (minCount == 0 || minCount >= ids.size());

                    for (String pId : ids) {
                        if (isMandatory) {
                            meta.mandatoryExcludedIds.add(pId);
                            queue.add(pId);
                        } else {
                            queue.add(pId);
                        }
                    }
                    if (!isMandatory) {
                        meta.additionalMinCount += minCount;
                    }
                }
            }
        }
        return meta;
    }
}
