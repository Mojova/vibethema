package com.vibethema.service.pdf.base;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vibethema.service.CharmDataService;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public abstract class BaseCharmExtractor {
    protected static final List<String> ABILITIES = Arrays.asList(
            "Archery", "Athletics", "Awareness", "Brawl", "Bureaucracy",
            "Craft", "Dodge", "Integrity", "Investigation", "Larceny",
            "Linguistics", "Lore", "Martial Arts", "Medicine", "Melee",
            "Occult", "Performance", "Presence", "Resistance", "Ride",
            "Sail", "Socialize", "Stealth", "Survival", "Thrown", "War"
    );

    protected final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    protected Path overrideOutputPath;

    public void setOverrideOutputPath(Path path) {
        this.overrideOutputPath = path;
    }

    public abstract void extractAndSave(String text, String suffix) throws IOException;

    protected String cleanDescription(String text) {
        // Remove page junk (headers, footers, page numbers)
        text = text.replaceAll("(?i)\\bC H A P T E R\\s+\\d+\\b", "");
        text = text.replaceAll("(?i)\\bE X 3\\b", "");
        text = text.replaceAll("(?i)\\bC H A R M S\\b", "");
        text = text.replaceAll("(?i)\\bE X A L T E D\\s+T H I R D\\s+E D I T I O N\\b", "");
        
        // Remove standalone page numbers
        text = text.replaceAll("\\n\\s*\\d{1,3}\\s*\\n", "\n");
        
        text = text.replaceAll("Syntax Warning:.*", "");
        text = text.replaceAll("[\\x00-\\x08\\x0b\\x0c\\x0e-\\x1f\\x7f]", "");

        String[] lines = text.split("\n");
        List<String> cleanedLines = new ArrayList<>();
        StringBuilder currentPara = new StringBuilder();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                // If the previous line ended with a hyphen, we assume the paragraph continues
                if (currentPara.length() > 0 && currentPara.charAt(currentPara.length() - 1) == '-') {
                    continue;
                }
                if (currentPara.length() > 0) {
                    cleanedLines.add(currentPara.toString().trim());
                    currentPara.setLength(0);
                }
                continue;
            }

            if (isSidebarLine(line)) continue;

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

                boolean isBullet = line.startsWith("•") || line.startsWith("- ") || line.startsWith("* ");
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

        return String.join("\n\n", cleanedLines);
    }

    protected boolean isSidebarLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return false;
        
        // Handle standalone page numbers and short page junk (e.g. "274", "EX3")
        if (trimmed.matches("\\d{1,3}") || (trimmed.toUpperCase().equals(trimmed) && trimmed.length() <= 3)) {
            return true;
        }

        String upper = trimmed.toUpperCase();
        if (upper.startsWith("ON ") && trimmed.length() < 100) return true;
        if (upper.startsWith("C H A P T E R") || upper.contains("SOLAR CHARMS")) return true;
        
        // Common 3e formatting artifacts
        if (upper.equals("EX3") || upper.equals("CHARMS") || upper.startsWith("CHAPTER")) return true;
        
        // Short all-caps lines are often headers/sidebars
        if (upper.equals(trimmed) && trimmed.length() > 3 && trimmed.length() < 70) {
            if (upper.contains("CHAPTER") || upper.contains("SOLAR") || upper.contains("EX3")) return true;
        }
        return false;
    }

    protected void resolvePrerequisites(Collection<Map<String, Object>> charms) {
        Set<String> allExtractedIds = new HashSet<>();
        charms.forEach(c -> allExtractedIds.add((String) c.get("id")));

        for (Map<String, Object> charm : charms) {
            String ability = (String) charm.get("ability");
            String currentCharmId = (String) charm.get("id");
            
            @SuppressWarnings("unchecked")
            List<Object> rawPrereqs = (List<Object>) charm.get("prerequisites");
            List<Map<String, Object>> resolvedGroups = new ArrayList<>();
            boolean problematic = false;

            if (rawPrereqs != null && !rawPrereqs.isEmpty()) {
                // If it's a simple list of strings, convert to one mandatory group
                if (rawPrereqs.get(0) instanceof String) {
                    List<String> ids = new ArrayList<>();
                    for (Object p : rawPrereqs) {
                        String pName = (String) p;
                        String cleanedName = pName.replaceAll("\\s+", " ").trim();
                        if (cleanedName.isEmpty()) continue;
                        
                        String pId = UUID.nameUUIDFromBytes((cleanedName + "|" + ability.trim()).getBytes()).toString();
                        ids.add(pId);
                        if (!allExtractedIds.contains(pId)) problematic = true;
                    }
                    if (!ids.isEmpty()) {
                        Map<String, Object> group = new LinkedHashMap<>();
                        group.put("charmIds", ids);
                        group.put("minCount", 0); // 0 means ALL
                        resolvedGroups.add(group);
                    }
                } else {
                    // It's already in Group format (Maps)
                    for (Object gObj : rawPrereqs) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> gMap = (Map<String, Object>) gObj;
                        @SuppressWarnings("unchecked")
                        List<String> names = (List<String>) gMap.get("names");
                        List<String> ids = new ArrayList<>();
                        
                        if (names != null) {
                            for (String name : names) {
                                if (name.startsWith("__OTHERS")) {
                                    Set<String> exclusions = new HashSet<>();
                                    exclusions.add(currentCharmId);
                                    int extraMinCount = 0;
                                    boolean nonExcellency = name.contains("|non-Excellency");

                                    if (name.startsWith("__OTHERS_EXCEPT:")) {
                                        String targetName = name.substring("__OTHERS_EXCEPT:".length()).split("\\|")[0].replace("__", "");
                                        TransitiveMetadata meta = calculateTransitiveMetadata(targetName, ability, charms);
                                        exclusions.addAll(meta.mandatoryExcludedIds);
                                        extraMinCount = meta.additionalMinCount;
                                    }

                                    // Add all other charm IDs from this ability that are not excluded
                                    for (Map<String, Object> other : charms) {
                                        String otherAbility = (String) other.get("ability");
                                        if (!ability.equals(otherAbility)) continue;

                                        String otherId = (String) other.get("id");
                                        String otherName = (String) other.get("name");
                                        if (!exclusions.contains(otherId)) {
                                            if (nonExcellency && (otherName.toLowerCase().contains("excellency") || 
                                                                 otherName.toLowerCase().contains("ex-cellency"))) {
                                                continue;
                                            }
                                            ids.add(otherId);
                                        }
                                    }
                                    
                                    // Update minCount for this group if we added extra counts from AE's prereqs
                                    if (extraMinCount > 0) {
                                        int currentMinCount = (int) gMap.getOrDefault("minCount", 0);
                                        gMap.put("minCount", currentMinCount + extraMinCount);
                                    }
                                } else {
                                    String cleanedName = name.replaceAll("\\s+", " ").trim();
                                    String pId = UUID.nameUUIDFromBytes((cleanedName + "|" + ability.trim()).getBytes()).toString();
                                    ids.add(pId);
                                    if (!allExtractedIds.contains(pId)) problematic = true;
                                }
                            }
                        }
                        
                        Map<String, Object> group = new LinkedHashMap<>();
                        if (gMap.containsKey("label")) group.put("label", gMap.get("label"));
                        group.put("charmIds", ids);
                        group.put("minCount", gMap.getOrDefault("minCount", 0));
                        resolvedGroups.add(group);
                    }
                }
            }

            charm.remove("prerequisites"); // No backwards comp
            charm.put("prerequisiteGroups", resolvedGroups);
            charm.put("potentiallyProblematicImport", problematic);
        }
    }

    protected void saveCharms(Map<String, List<Map<String, Object>>> charmsByAbility, String suffix, boolean isMartialArts) throws IOException {
        Path outDir = overrideOutputPath != null ? overrideOutputPath : (isMartialArts ? CharmDataService.getUserMartialArtsPath() : CharmDataService.getUserCharmsPath());
        if (!Files.exists(outDir)) Files.createDirectories(outDir);

        for (Map.Entry<String, List<Map<String, Object>>> entry : charmsByAbility.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            String filename = entry.getKey().toLowerCase().replace(" ", "-") + suffix + ".json";
            Path filePath = outDir.resolve(filename);
            try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                Map<String, Object> wrapper = new LinkedHashMap<>();
                wrapper.put("$schema", "./charm-schema.json");
                wrapper.put("version", "0.1.0");
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
    private TransitiveMetadata calculateTransitiveMetadata(String targetName, String ability, Collection<Map<String, Object>> charms) {
        TransitiveMetadata meta = new TransitiveMetadata();
        String rootId = UUID.nameUUIDFromBytes((targetName + "|" + ability.trim()).getBytes()).toString();
        
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(rootId);
        meta.mandatoryExcludedIds.add(rootId);

        while (!queue.isEmpty()) {
            String currId = queue.poll();
            if (visited.contains(currId)) continue;
            visited.add(currId);

            Map<String, Object> charm = charms.stream().filter(c -> c.get("id").equals(currId)).findFirst().orElse(null);
            if (charm == null) continue;

            List<Object> rawPrereqs = (List<Object>) charm.get("prerequisites");
            List<Map<String, Object>> resolvedGroups = (List<Map<String, Object>>) charm.get("prerequisiteGroups");
            
            if ((rawPrereqs == null || rawPrereqs.isEmpty()) && (resolvedGroups == null || resolvedGroups.isEmpty())) continue;

            if (rawPrereqs != null && !rawPrereqs.isEmpty()) {
                if (rawPrereqs.get(0) instanceof String) {
                    // Mandatory simple list
                    for (Object p : rawPrereqs) {
                        String pId = UUID.nameUUIDFromBytes(((String) p + "|" + ability.trim()).getBytes()).toString();
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
                            String pId = UUID.nameUUIDFromBytes((pName.trim() + "|" + ability.trim()).getBytes()).toString();
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
