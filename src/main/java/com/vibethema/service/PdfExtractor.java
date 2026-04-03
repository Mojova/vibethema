package com.vibethema.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PdfExtractor {
    private static final List<String> ABILITIES = Arrays.asList(
            "Archery", "Athletics", "Awareness", "Brawl", "Bureaucracy",
            "Craft", "Dodge", "Integrity", "Investigation", "Larceny",
            "Linguistics", "Lore", "Martial Arts", "Medicine", "Melee",
            "Occult", "Performance", "Presence", "Resistance", "Ride",
            "Sail", "Socialize", "Stealth", "Survival", "Thrown", "War"
    );

    private static final List<String> KEYWORD_LIST = Arrays.asList(
            "Aggravated", "Bridge", "Clash", "Counterattack", "Decisive-only",
            "Dual", "Form", "Mastery", "Mute", "Perilous", "Pilot",
            "Psyche", "Salient", "Stackable", "Terrestrial", "Uniform",
            "Withering-only", "Written-only"
    );

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void extractAll(File pdfFile, Consumer<Double> progressCallback) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            
            // Extract Charms (Whole PDF)
            if (progressCallback != null) progressCallback.accept(0.1);
            String fullText = stripper.getText(document);
            if (progressCallback != null) progressCallback.accept(0.4);
            extractAndSaveCharms(fullText);
            
            // Extract Keywords (Pages 250-260)
            if (progressCallback != null) progressCallback.accept(0.7);
            stripper.setStartPage(250);
            stripper.setEndPage(260);
            String keywordText = stripper.getText(document);
            extractAndSaveKeywords(keywordText);
            
            if (progressCallback != null) progressCallback.accept(1.0);
        }
    }

    private void extractAndSaveCharms(String text) throws IOException {
        Map<String, List<Map<String, Object>>> charmsByAbility = new HashMap<>();
        for (String abil : ABILITIES) {
            charmsByAbility.put(abil, new ArrayList<>());
        }

        // Split by potential charm starts: [Name]\nCost:
        // Java regex split with lookahead
        String[] parts = text.split("\n(?=[^\n]+\nCost:)");

        for (String part : parts) {
            String[] lines = part.trim().split("\n");
            if (lines.length < 3) continue;

            String name = lines[0].trim();
            String costLine = lines[1].trim();

            if (!costLine.startsWith("Cost:")) continue;

            // Pattern: Mins: [Ability] [Dots], Essence [Dots]
            Pattern minsPattern = Pattern.compile("Mins: ([\\w\\s]+) (\\d+), Essence (\\d+)");
            Matcher minsMatcher = minsPattern.matcher(costLine);

            if (!minsMatcher.find()) continue;

            String ability = minsMatcher.group(1).trim();
            int minAbilityValue;
            int minEssenceValue;
            try {
                minAbilityValue = Integer.parseInt(minsMatcher.group(2));
                minEssenceValue = Integer.parseInt(minsMatcher.group(3));
            } catch (NumberFormatException e) {
                continue;
            }

            if (!ABILITIES.contains(ability)) continue;

            // Extract Cost
            Pattern costPattern = Pattern.compile("Cost: (.*?);");
            Matcher costMatcher = costPattern.matcher(costLine);
            String cost = costMatcher.find() ? costMatcher.group(1) : costLine.replace("Cost: ", "").split(";")[0];

            String type = "";
            String keywords = "";
            String duration = "";
            List<String> prereqs = new ArrayList<>();
            int descStartIdx = 2;

            for (int i = 2; i < Math.min(lines.length, 7); i++) {
                String line = lines[i].trim();
                if (line.startsWith("Type:")) {
                    type = line.replace("Type:", "").trim();
                    descStartIdx = i + 1;
                } else if (line.startsWith("Keywords:")) {
                    keywords = line.replace("Keywords:", "").trim();
                    descStartIdx = i + 1;
                } else if (line.startsWith("Duration:")) {
                    duration = line.replace("Duration:", "").trim();
                    descStartIdx = i + 1;
                } else if (line.startsWith("Prerequisite Charms:")) {
                    String pText = line.replace("Prerequisite Charms:", "").trim();
                    if (!pText.isEmpty() && !pText.equalsIgnoreCase("none")) {
                        for (String p : pText.split(",")) {
                            prereqs.add(p.trim());
                        }
                    }
                    descStartIdx = i + 1;
                }
            }

            StringBuilder descRaw = new StringBuilder();
            for (int i = descStartIdx; i < lines.length; i++) {
                descRaw.append(lines[i]).append("\n");
            }

            String fullText = cleanDescription(descRaw.toString());

            Map<String, Object> charmMap = new LinkedHashMap<>();
            charmMap.put("name", name);
            charmMap.put("ability", ability);
            charmMap.put("minAbility", minAbilityValue);
            charmMap.put("minEssence", minEssenceValue);
            charmMap.put("prerequisites", prereqs);
            charmMap.put("cost", cost);
            charmMap.put("type", type);
            charmMap.put("keywords", keywords);
            charmMap.put("duration", duration);
            charmMap.put("fullText", fullText);

            charmsByAbility.get(ability).add(charmMap);
        }

        Path outDir = CharmDataService.getUserCharmsPath();
        if (!Files.exists(outDir)) {
            Files.createDirectories(outDir);
        }

        for (Map.Entry<String, List<Map<String, Object>>> entry : charmsByAbility.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            String filename = entry.getKey().toLowerCase().replace(" ", "-") + ".json";
            Path filePath = outDir.resolve(filename);
            try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                gson.toJson(entry.getValue(), writer);
            }
        }
    }

    private void extractAndSaveKeywords(String text) throws IOException {
        List<Map<String, String>> outputKeywords = new ArrayList<>();
        
        // Pattern matches bullet point, any amount of non-uppercase characters,
        // and then a keyword from our list followed by a colon.
        StringBuilder sb = new StringBuilder("•[^A-Z]*?(");
        for (int i = 0; i < KEYWORD_LIST.size(); i++) {
            sb.append(Pattern.quote(KEYWORD_LIST.get(i)));
            if (i < KEYWORD_LIST.size() - 1) sb.append("|");
        }
        sb.append("):");
        
        String patternStr = sb.toString();
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(text);
        
        int lastEnd = 0;
        String currentKeyword = null;
        
        while (matcher.find()) {
            if (currentKeyword != null) {
                String rawDesc = text.substring(lastEnd, matcher.start()).trim();
                addKeyword(outputKeywords, currentKeyword, rawDesc);
            }
            currentKeyword = matcher.group(1);
            lastEnd = matcher.end();
        }
        
        if (currentKeyword != null) {
            String rawDesc = text.substring(lastEnd).trim();
            addKeyword(outputKeywords, currentKeyword, rawDesc);
        }
        
        // Manual fallback for Mastery/Terrestrial sidebar
        if (outputKeywords.stream().noneMatch(k -> k.get("name").equals("Mastery"))) {
            Pattern sidebarPattern = Pattern.compile("MASTER’S HAND: SOLAR MASTERY AND TERRESTRIAL EFFECTS(.*?)(?=CHAPTER|EX3|\\d{3})", Pattern.DOTALL);
            Matcher sidebarMatcher = sidebarPattern.matcher(text);
            if (sidebarMatcher.find()) {
                String desc = cleanDescription(sidebarMatcher.group(1));
                Map<String, String> m = new HashMap<>();
                m.put("name", "Mastery");
                m.put("description", desc);
                outputKeywords.add(m);
                
                Map<String, String> t = new HashMap<>();
                t.put("name", "Terrestrial");
                t.put("description", desc);
                outputKeywords.add(t);
            }
        }

        Path outDir = CharmDataService.getUserCharmsPath();
        Path filePath = outDir.resolve("keywords.json");
        try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            gson.toJson(outputKeywords, writer);
        }
    }

    private void addKeyword(List<Map<String, String>> list, String name, String rawDesc) {
        String[] stopWords = {"Duration:", "Prerequisite Charms:", "EX3", "CHAPTER 6"};
        String cleanedRaw = rawDesc;
        for (String stop : stopWords) {
            if (cleanedRaw.contains(stop)) {
                cleanedRaw = cleanedRaw.split(stop)[0].trim();
            }
        }
        String desc = cleanDescription(cleanedRaw);
        Map<String, String> kw = new HashMap<>();
        kw.put("name", name);
        kw.put("description", desc);
        list.add(kw);
    }

    private String cleanDescription(String text) {
        // Remove page numbers, chapter headers, etc.
        text = text.replaceAll("\\n\\d+\\nEX3\\n", "\n");
        text = text.replaceAll("\\nEX3\\n", "\n");
        text = text.replaceAll("\\nCHARMS\\n", "\n");
        text = text.replaceAll("\\nCHAPTER \\d+\\n", "\n");
        text = text.replaceAll("Syntax Warning:.*", "");
        // Remove non-printable characters
        text = text.replaceAll("[\\x00-\\x08\\x0b\\x0c\\x0e-\\x1f\\x7f]", "");

        String[] lines = text.trim().split("\n");
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

            if (currentPara.length() > 0) {
                String prevContent = currentPara.toString().trim();
                char prevChar = prevContent.charAt(prevContent.length() - 1);

                boolean isBullet = line.startsWith("•") || line.startsWith("- ") || line.startsWith("* ");
                boolean isContinuation = Character.isLowerCase(line.charAt(0)) || prevChar == ',';

                if (isBullet || (!isContinuation && ".!? :;".indexOf(prevChar) != -1)) {
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
}
