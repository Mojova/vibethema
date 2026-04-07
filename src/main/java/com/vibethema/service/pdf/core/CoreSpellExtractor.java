package com.vibethema.service.pdf.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vibethema.service.CharmDataService;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CoreSpellExtractor {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Path overrideOutputPath;

    public void setOverrideOutputPath(Path path) {
        this.overrideOutputPath = path;
    }

    public void extractAndSave(String text, String suffix) throws IOException {
        Map<String, List<Map<String, Object>>> spellsByCircle = new HashMap<>();
        spellsByCircle.put("TERRESTRIAL", new ArrayList<>());
        spellsByCircle.put("CELESTIAL", new ArrayList<>());
        spellsByCircle.put("SOLAR", new ArrayList<>());

        String currentCircle = "TERRESTRIAL";
        String[] lines = text.split("\r?\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            if (line.contains("Terrestrial Circle Spells")) {
                currentCircle = "TERRESTRIAL";
                continue;
            } else if (line.contains("Celestial Circle Spells")) {
                currentCircle = "CELESTIAL";
                continue;
            } else if (line.contains("Solar Circle Spells")) {
                currentCircle = "SOLAR";
                continue;
            }

            if (i + 1 < lines.length && lines[i + 1].trim().startsWith("Cost:")) {
                String name = line;
                String costLine = lines[i + 1].trim();
                String cost = costLine.replace("Cost:", "").trim();

                if (!cost.toLowerCase().contains("sm") && !cost.toLowerCase().contains("ritual")) {
                    continue;
                }

                String keywords = "";
                String duration = "";
                int descStartIdx = i + 2;

                for (int j = descStartIdx; j < Math.min(lines.length, descStartIdx + 3); j++) {
                    String l = lines[j].trim();
                    if (l.startsWith("Keywords:")) {
                        keywords = l.replace("Keywords:", "").trim();
                        descStartIdx = j + 1;
                    } else if (l.startsWith("Duration:")) {
                        duration = l.replace("Duration:", "").trim();
                        descStartIdx = j + 1;
                    }
                }

                StringBuilder descRaw = new StringBuilder();
                int k = descStartIdx;
                while (k < lines.length) {
                    String l = lines[k].trim();
                    if (k + 1 < lines.length && lines[k + 1].trim().startsWith("Cost:")) break;
                    if (l.contains("Circle Spells")) break;
                    if (l.startsWith("Sorcerous Workings")) break;
                    descRaw.append(lines[k]).append("\n");
                    k++;
                }

                i = k - 1;

                String description = cleanDescription(descRaw.toString());
                String spellId =
                        UUID.nameUUIDFromBytes((name.trim() + "|" + currentCircle).getBytes())
                                .toString();

                Map<String, Object> spellMap = new LinkedHashMap<>();
                spellMap.put("id", spellId);
                spellMap.put("name", name);
                spellMap.put("circle", currentCircle);
                spellMap.put("cost", cost);

                List<String> kwList = new ArrayList<>();
                if (!keywords.isEmpty() && !keywords.equalsIgnoreCase("None")) {
                    for (String kw : keywords.split(",")) {
                        String t = kw.trim();
                        if (!t.isEmpty()) kwList.add(t);
                    }
                }
                spellMap.put("keywords", kwList);
                spellMap.put("duration", duration);
                spellMap.put("description", description);

                spellsByCircle.get(currentCircle).add(spellMap);
            }
        }

        saveSpells(spellsByCircle, suffix);
    }

    private void saveSpells(Map<String, List<Map<String, Object>>> spellsByCircle, String suffix)
            throws IOException {
        Path outDir =
                overrideOutputPath != null
                        ? overrideOutputPath
                        : CharmDataService.getUserSpellsPath();
        if (!Files.exists(outDir)) Files.createDirectories(outDir);

        for (Map.Entry<String, List<Map<String, Object>>> entry : spellsByCircle.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            String filename = entry.getKey().toLowerCase() + suffix + ".json";
            Path filePath = outDir.resolve(filename);
            try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                Map<String, Object> wrapper = new LinkedHashMap<>();
                wrapper.put("version", "0.1.0");
                wrapper.put("circle", entry.getKey());
                wrapper.put("spells", entry.getValue());
                gson.toJson(wrapper, writer);
            }
        }
    }

    private String cleanDescription(String text) {
        // Simple cleaning for spells (shared with charms but localized here for now)
        text = text.replaceAll("(?i)\\bC H A P T E R\\s+\\d+\\b", "");
        text = text.replaceAll("(?i)\\bE X 3\\b", "");
        text = text.replaceAll("[\\x00-\\x08\\x0b\\x0c\\x0e-\\x1f\\x7f]", "");
        return text.trim();
    }
}
