package com.vibethema.service.pdf.core;

import com.vibethema.service.pdf.base.BaseCharmExtractor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoreMeritExtractor extends BaseCharmExtractor {

    @Override
    public void extractAndSave(String text, String suffix) throws IOException {
        // Pre-process: Strip Core Book page-junk blocks
        text =
                text.replaceAll(
                                "(?m)^(?:[A-Z\\s]{5,}|MERITS|TRAITS)\\s*\\n"
                                        + "E\\s*X\\s*3\\s*\\n"
                                        + "\\d{3}\\s*\\n",
                                "\n")
                        .replaceAll(
                                "(?m)^\\n\\d{3}\\s*\\nE\\s*X\\s*3\\s*\\n(?:[A-Z\\s]{5,})", "\n");

        List<Map<String, Object>> merits = new ArrayList<>();

        // Improved Regex for split: Name (Dots)—Category
        // Handles: Allies (•, •••, or •••••)—Story
        // Handles: Hideous (0 dots)—Innate
        // Handles: Artifact (•• to •••••)—Story
        // Now handles slashes: Claws/Fangs/Hooves/Horns (• or ••••)—Innate
        Pattern meritHeaderPattern =
                Pattern.compile(
                        "(?m)^\\s*([A-Z][\\w\\s&/'-]+) \\(([^)]+)\\)—(Story|Innate|Purchased)$");

        // We use a matcher to find all starts, then split manually to ensure we don't miss any due
        // to whitespace issues
        Matcher headerMatcher = meritHeaderPattern.matcher(text);
        List<Integer> startIndices = new ArrayList<>();
        while (headerMatcher.find()) {
            startIndices.add(headerMatcher.start());
        }

        for (int i = 0; i < startIndices.size(); i++) {
            int start = startIndices.get(i);
            int end = (i < startIndices.size() - 1) ? startIndices.get(i + 1) : text.length();
            String part = text.substring(start, end).trim();

            String[] lines = part.split("\n");
            Matcher m = meritHeaderPattern.matcher(lines[0].trim());
            if (!m.find()) continue;

            String name = m.group(1).trim();
            String rawDots = m.group(2).trim();
            String category = m.group(3).trim();

            List<Integer> ratings = parseRatings(rawDots);
            String prerequisites = "";
            StringBuilder descRaw = new StringBuilder();
            int descStartIdx = 1;
            StringBuilder prereqSb = new StringBuilder();
            if (lines.length > 1 && lines[1].trim().startsWith("Prerequisite:")) {
                prereqSb.append(lines[1].replace("Prerequisite:", "").trim());
                descStartIdx = 2;

                // Multi-line prerequisite support
                while (descStartIdx < lines.length) {
                    String nextLine = lines[descStartIdx].trim();
                    if (nextLine.isEmpty()) break;

                    String currentPrereq = prereqSb.toString().trim();
                    boolean isContinuation =
                            currentPrereq.endsWith(",")
                                    || currentPrereq.endsWith("or")
                                    || currentPrereq.endsWith("and")
                                    || Character.isLowerCase(nextLine.charAt(0));

                    if (isContinuation) {
                        prereqSb.append(" ").append(nextLine);
                        descStartIdx++;
                    } else {
                        break;
                    }
                }
                prerequisites = prereqSb.toString().trim();
            }

            for (int j = descStartIdx; j < lines.length; j++) {
                String line = lines[j].trim();
                // Check for section breaks that should stop description extraction
                if (line.equals("Flaws")
                        || line.equals("FLAWS")
                        || line.equals("Supernatural Merits")) {
                    break;
                }
                descRaw.append(lines[j]).append("\n");
            }

            String fullDescription = super.cleanDescription(descRaw.toString());
            String meritId = UUID.nameUUIDFromBytes((name.trim()).getBytes()).toString();

            Map<String, Object> meritMap = new LinkedHashMap<>();
            meritMap.put("id", meritId);
            meritMap.put("name", name);
            meritMap.put("category", category);
            meritMap.put("ratings", ratings);
            if (!prerequisites.isEmpty()) {
                meritMap.put("prerequisites", prerequisites);
            }
            meritMap.put("description", fullDescription);
            meritMap.put("rawData", part);

            merits.add(meritMap);
        }

        saveMerits(merits, suffix);
    }

    private List<Integer> parseRatings(String rawDots) {
        List<Integer> ratings = new ArrayList<>();
        if (rawDots.contains("0 dots")) {
            ratings.add(0);
            return ratings;
        }

        // Handle range: "•• to •••••"
        if (rawDots.contains(" to ")) {
            String[] parts = rawDots.split(" to ");
            int start = countDots(parts[0]);
            int end = countDots(parts[parts.length - 1]);
            for (int i = Math.max(1, start); i <= end; i++) {
                ratings.add(i);
            }
        }
        // Handle options: "•, •••, or •••••"
        else if (rawDots.contains(",") || rawDots.contains(" or ")) {
            String[] parts = rawDots.split(",| or ");
            for (String p : parts) {
                int c = countDots(p.trim());
                if (c > 0) ratings.add(c);
            }
        }
        // Handle single: "•"
        else {
            int c = countDots(rawDots);
            if (c > 0) ratings.add(c);
        }
        return ratings;
    }

    private int countDots(String s) {
        int count = 0;
        for (char c : s.toCharArray()) {
            if (c == '•' || c == '*') count++;
        }
        return count;
    }

    @Override
    protected String cleanDescription(String text) {
        // Use base class logic but filter out Merit-specific artifacts if needed
        return super.cleanDescription(text);
    }

    private void saveMerits(List<Map<String, Object>> merits, String suffix) throws IOException {
        Path outDir =
                overrideOutputPath != null
                        ? overrideOutputPath
                        : com.vibethema.service.CharmDataService.getUserCharmsPath()
                                .getParent()
                                .resolve("merits");

        if (!Files.exists(outDir)) Files.createDirectories(outDir);

        Path filePath = outDir.resolve("merits" + suffix + ".json");
        try (java.io.Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("version", "1.0.0");
            wrapper.put("merits", merits);
            gson.toJson(wrapper, writer);
        }
    }
}
