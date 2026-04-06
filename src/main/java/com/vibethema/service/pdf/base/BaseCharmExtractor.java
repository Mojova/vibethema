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
                if (lastChar == '-') {
                    currentPara.setLength(currentPara.length() - 1);
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
        String upper = line.toUpperCase().trim();
        // Common 3e formatting artifacts
        if (upper.equals("EX3") || upper.equals("CHARMS") || upper.startsWith("C H A P T E R")) return true;
        
        // Short all-caps lines are often headers/sidebars
        if (upper.equals(line.trim()) && line.length() > 3 && line.length() < 70) {
            if (upper.contains("CHAPTER") || upper.contains("SOLAR") || upper.contains("EX3")) return true;
        }
        return false;
    }

    protected void resolvePrerequisites(Collection<Map<String, Object>> charms) {
        Set<String> allExtractedIds = new HashSet<>();
        charms.forEach(c -> allExtractedIds.add((String) c.get("id")));

        for (Map<String, Object> charm : charms) {
            String ability = (String) charm.get("ability");
            @SuppressWarnings("unchecked")
            List<String> prereqNames = (List<String>) charm.get("prerequisites");
            List<String> prereqIds = new ArrayList<>();
            boolean problematic = false;

            if (prereqNames != null) {
                for (String pName : prereqNames) {
                    String pId = UUID.nameUUIDFromBytes((pName.trim() + "|" + ability.trim()).getBytes()).toString();
                    prereqIds.add(pId);
                    if (!allExtractedIds.contains(pId)) problematic = true;
                }
            }
            charm.put("prerequisites", prereqIds);
            charm.put("potentiallyProblematicImport", problematic);
        }
    }

    protected void saveCharms(Map<String, List<Map<String, Object>>> charmsByAbility, String suffix, boolean isMartialArts) throws IOException {
        Path outDir = isMartialArts ? CharmDataService.getUserMartialArtsPath() : CharmDataService.getUserCharmsPath();
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
}
