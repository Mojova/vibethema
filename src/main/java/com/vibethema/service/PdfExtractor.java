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
    public enum PdfSource {
        CORE,
        MOSE
    }

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
        extractAll(pdfFile, "", true, PdfSource.CORE, progressCallback);
    }

    public void extractAll(File pdfFile, String suffix, boolean extractKeywords, PdfSource source, Consumer<Double> progressCallback) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            
            // Extract Charms (Whole PDF)
            if (progressCallback != null) progressCallback.accept(0.1);
            String fullText = stripper.getText(document);
            if (progressCallback != null) progressCallback.accept(0.4);
            extractAndSaveCharms(fullText, suffix, source);
            
            // Extract Keywords (Pages 250-260) - Skip if not requested or not Core
            if (extractKeywords && source == PdfSource.CORE) {
                if (progressCallback != null) progressCallback.accept(0.7);
                stripper.setStartPage(250);
                stripper.setEndPage(260);
                String keywordText = stripper.getText(document);
                extractAndSaveKeywords(keywordText);
            }

            if (source == PdfSource.CORE) {
                if (progressCallback != null) progressCallback.accept(0.50);
                // Sorcery chapter: spells themselves are between 470 and 495 in Core PDF
                stripper.setStartPage(470);
                stripper.setEndPage(495);
                String spellText = stripper.getText(document);
                if (progressCallback != null) progressCallback.accept(0.60);
                extractAndSaveSpells(spellText, suffix);

                if (progressCallback != null) progressCallback.accept(0.85);
                extractAndSaveEquipmentTags(document, stripper);
            }
            
            if (progressCallback != null) progressCallback.accept(1.0);
        }
    }

    private void extractAndSaveCharms(String text, String suffix, PdfSource source) throws IOException {
        Map<String, List<Map<String, Object>>> charmsByAbility = new HashMap<>();
        Map<String, List<Map<String, Object>>> charmsByMartialArtsStyle = new HashMap<>();
        for (String abil : ABILITIES) {
            charmsByAbility.put(abil, new ArrayList<>());
        }

        String currentMartialArtsStyle = null;

        // Split by potential charm starts: [Name]\nCost:
        // Java regex split with lookahead
        String splitRegex = (source == PdfSource.MOSE) ? 
            "\n(?=[^\n:]+\r?\n+(?:Backer:.*\n+)?Cost:)" : 
            "\n(?=[^\n]+\nCost:)";
        String[] parts = text.split(splitRegex);

        for (String part : parts) {
            String[] lines = part.trim().split("\n");
            if (lines.length < 3) continue;

            String name = "";
            String ability = "";
            int minAbilityValue = 0;
            int minEssenceValue = 0;
            String costLine = "";
            int descStartIdx = 0;

            // Pattern for style headers: e.g. "Snake Style" or "Tiger Style"
            Pattern stylePattern = Pattern.compile("^([A-Z][\\w\\s]+ Style)$", Pattern.MULTILINE);
            
            // Look for Style changes in the whole part (especially at the beginning before name)
            Matcher styleMatcher = stylePattern.matcher(part);
            while (styleMatcher.find()) {
                currentMartialArtsStyle = styleMatcher.group(1).trim();
            }

            // Find the Name and Cost line
            if (source == PdfSource.MOSE) {
                for (int i = 0; i < lines.length - 1; i++) {
                    String line = lines[i].trim();
                    if (line.isEmpty() || line.startsWith("Backer:")) continue;
                    
                    // Look ahead for "Cost:" skipping blank lines and Backer lines
                    for (int j = i + 1; j < Math.min(lines.length, i + 5); j++) {
                        String nextLine = lines[j].trim();
                        if (nextLine.startsWith("Cost:")) {
                            name = line;
                            costLine = nextLine;
                            descStartIdx = j + 1;
                            break;
                        }
                        if (!nextLine.isEmpty() && !nextLine.startsWith("Backer:")) {
                            // Hit something that isn't Cost and isn't a known skip line
                            break;
                        }
                    }
                    if (!name.isEmpty()) break;
                }
            } else {
                for (int i = 0; i < lines.length - 1; i++) {
                    if (lines[i+1].trim().startsWith("Cost:")) {
                        name = lines[i].trim();
                        costLine = lines[i+1].trim();
                        descStartIdx = i + 2;
                        break;
                    }
                }
            }

            if (name.isEmpty() || costLine.isEmpty()) continue;

            // Skip if it looks like a Style header was mistaken for a name
            if (name.endsWith("Style")) {
                currentMartialArtsStyle = name;
                continue;
            }

            // Pattern: Mins: [Ability] [Dots], Essence [Dots]
            Pattern minsPattern = Pattern.compile("Mins: ([\\w\\s]+) (\\d+), Essence (\\d+)");
            Matcher minsMatcher;
            if (source == PdfSource.MOSE) {
                minsMatcher = minsPattern.matcher(part);
            } else {
                minsMatcher = minsPattern.matcher(costLine);
            }

            if (!minsMatcher.find()) continue;

            ability = minsMatcher.group(1).trim();
            try {
                minAbilityValue = Integer.parseInt(minsMatcher.group(2));
                minEssenceValue = Integer.parseInt(minsMatcher.group(3));
            } catch (NumberFormatException e) {
                continue;
            }

            if (!ABILITIES.contains(ability)) continue;

            // If it's a Martial Arts charm, assign it to the current style if we have one
            if (ability.equalsIgnoreCase("Martial Arts") && currentMartialArtsStyle != null) {
                ability = currentMartialArtsStyle;
            }

            // Extract headers (Type, Keywords, Duration, Prereqs) with multi-line support
            String type = "";
            String keywords = "";
            String duration = "";
            String rawPrereqs = "";
            String currentField = "";
            
            int headerSearchIdx = descStartIdx;
            for (int i = headerSearchIdx; i < Math.min(lines.length, headerSearchIdx + 10); i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("Type:")) {
                    type = line.replace("Type:", "").trim();
                    currentField = "Type";
                    descStartIdx = i + 1;
                } else if (line.startsWith("Keywords:")) {
                    keywords = line.replace("Keywords:", "").trim();
                    currentField = "Keywords";
                    descStartIdx = i + 1;
                } else if (line.startsWith("Duration:")) {
                    duration = line.replace("Duration:", "").trim();
                    currentField = "Duration";
                    descStartIdx = i + 1;
                } else if (line.startsWith("Prerequisite Charms:")) {
                    rawPrereqs = line.replace("Prerequisite Charms:", "").trim();
                    currentField = "Prereqs";
                    descStartIdx = i + 1;
                } else if (!currentField.isEmpty() && !line.contains(":") && !line.contains("Cost:")) {
                    // Check for description start heuristics
                    boolean isLikelyDescription = line.startsWith("The ") || line.startsWith("This ") || 
                                               line.startsWith("If ") || line.startsWith("When ") || 
                                               line.startsWith("A ") || line.startsWith("Solar ");
                    
                    // Specific "None" check - if we have "None", don't soak description text
                    if (currentField.equals("Keywords") && keywords.equalsIgnoreCase("none")) isLikelyDescription = true;
                    if (currentField.equals("Prereqs") && rawPrereqs.equalsIgnoreCase("none")) isLikelyDescription = true;

                    if (isLikelyDescription) break;

                    // Continuation line
                    if (currentField.equals("Type")) type += " " + line;
                    else if (currentField.equals("Keywords")) keywords += ", " + line;
                    else if (currentField.equals("Duration")) duration += " " + line;
                    else if (currentField.equals("Prereqs")) rawPrereqs += " " + line;
                    descStartIdx = i + 1;
                } else {
                    // Likely start of description
                    break;
                }
            }

            List<String> prereqs = new ArrayList<>();
            // Clean up "None" or variations like "None The Solar..." accidentally sucked in
            String cleanPrereqs = rawPrereqs.trim();
            if (cleanPrereqs.toLowerCase().startsWith("none")) {
                cleanPrereqs = cleanPrereqs.substring(4).trim();
                if (cleanPrereqs.startsWith(",")) cleanPrereqs = cleanPrereqs.substring(1).trim();
            }

            if (!cleanPrereqs.isEmpty()) {
                for (String p : cleanPrereqs.split(",")) {
                    String trimmed = p.trim();
                    // Final sanity check: prerequisite names shouldn't be long sentences
                    if (!trimmed.isEmpty() && !trimmed.startsWith("The ") && trimmed.length() < 100) {
                        prereqs.add(trimmed);
                    }
                }
            }

            // Extract Cost
            Pattern costPattern = Pattern.compile("Cost: (.*?);");
            Matcher costMatcher = costPattern.matcher(costLine);
            String cost = costMatcher.find() ? costMatcher.group(1) : costLine.replace("Cost: ", "").split(";")[0];

            StringBuilder descRaw = new StringBuilder();
            for (int i = descStartIdx; i < lines.length; i++) {
                descRaw.append(lines[i]).append("\n");
            }

            String fullText = cleanDescription(descRaw.toString());
            String charmId = java.util.UUID.nameUUIDFromBytes((name.trim() + "|" + ability.trim()).getBytes()).toString();

            Map<String, Object> charmMap = new LinkedHashMap<>();
            charmMap.put("id", charmId);
            charmMap.put("name", name);
            charmMap.put("ability", ability);
            charmMap.put("minAbility", minAbilityValue);
            charmMap.put("minEssence", minEssenceValue);
            charmMap.put("prerequisites", prereqs);
            charmMap.put("cost", cost);
            charmMap.put("type", type);
            List<String> kwList = new ArrayList<>();
            if (!keywords.isEmpty() && !keywords.equalsIgnoreCase("None")) {
                for (String kw : keywords.split(",")) {
                    String t = kw.trim();
                    if (!t.isEmpty()) kwList.add(t);
                }
            }
            charmMap.put("keywords", kwList);
            charmMap.put("duration", duration);
            charmMap.put("fullText", fullText);
            charmMap.put("rawData", part.trim());

            if (charmsByAbility.containsKey(ability)) {
                charmsByAbility.get(ability).add(charmMap);
            } else {
                charmsByMartialArtsStyle.computeIfAbsent(ability, k -> new ArrayList<>()).add(charmMap);
            }
        }

        // --- POST-PROCESSING: Convert Prereq names to IDs and check integrity ---
        Set<String> allExtractedIds = new HashSet<>();
        List<Map<String, Object>> allExtractedCharms = new ArrayList<>();
        charmsByAbility.values().forEach(allExtractedCharms::addAll);
        charmsByMartialArtsStyle.values().forEach(allExtractedCharms::addAll);
        
        for (Map<String, Object> charm : allExtractedCharms) {
            allExtractedIds.add((String) charm.get("id"));
        }

        for (Map<String, Object> charm : allExtractedCharms) {
            String ability = (String) charm.get("ability");
            @SuppressWarnings("unchecked")
            List<String> prereqNames = (List<String>) charm.get("prerequisites");
            List<String> prereqIds = new ArrayList<>();
            boolean problematic = false;

            if (prereqNames != null) {
                for (String pName : prereqNames) {
                    // Generate deterministic ID for the prerequisite (assuming same ability)
                    String pId = java.util.UUID.nameUUIDFromBytes((pName.trim() + "|" + ability.trim()).getBytes()).toString();
                    prereqIds.add(pId);
                    if (!allExtractedIds.contains(pId)) {
                        problematic = true;
                    }
                }
            }
            charm.put("prerequisites", prereqIds);
            charm.put("potentiallyProblematicImport", problematic);
        }

        Path outDir = CharmDataService.getUserCharmsPath();
        if (!Files.exists(outDir)) {
            Files.createDirectories(outDir);
        }

        // Save Standard Abilities
        for (Map.Entry<String, List<Map<String, Object>>> entry : charmsByAbility.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            String filename = entry.getKey().toLowerCase().replace(" ", "-") + suffix + ".json";
            Path filePath = outDir.resolve(filename);
            try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                Map<String, Object> wrapper = new LinkedHashMap<>();
                wrapper.put("$schema", "./charm-schema.json");
                wrapper.put("version", "0.1.0");
                wrapper.put("ability", entry.getKey());
                wrapper.put("type", entry.getKey().equalsIgnoreCase("Martial Arts") ? "martialArts" : "solarAbility");
                wrapper.put("exalt", "solar");
                wrapper.put("charms", entry.getValue());
                gson.toJson(wrapper, writer);
            }
        }

        // Save Martial Arts Styles to dedicated directory
        Path maOutDir = CharmDataService.getUserMartialArtsPath();
        if (!Files.exists(maOutDir)) {
            Files.createDirectories(maOutDir);
        }

        for (Map.Entry<String, List<Map<String, Object>>> entry : charmsByMartialArtsStyle.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            String filename = entry.getKey().toLowerCase().replace(" ", "-") + suffix + ".json";
            Path filePath = maOutDir.resolve(filename);
            try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                Map<String, Object> wrapper = new LinkedHashMap<>();
                wrapper.put("$schema", "./charm-schema.json");
                wrapper.put("version", "0.1.0");
                wrapper.put("ability", entry.getKey());
                wrapper.put("type", "martialArts");
                wrapper.put("exalt", "solar");
                wrapper.put("charms", entry.getValue());
                gson.toJson(wrapper, writer);
            }
        }
        
        // Export schema to both locations
        new CharmDataService().exportSchema();
    }

    private void extractAndSaveSpells(String text, String suffix) throws IOException {
        Map<String, List<Map<String, Object>>> spellsByCircle = new HashMap<>();
        spellsByCircle.put("TERRESTRIAL", new ArrayList<>());
        spellsByCircle.put("CELESTIAL", new ArrayList<>());
        spellsByCircle.put("SOLAR", new ArrayList<>());

        String currentCircle = "TERRESTRIAL";
        String[] lines = text.split("\r?\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            // Detect Circle shifts - these are usually on their own line as headers
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

            // Potential spell start: current line is name, next line starts with "Cost:"
            if (i + 1 < lines.length && lines[i+1].trim().startsWith("Cost:")) {
                String name = line;
                String costLine = lines[i+1].trim();
                String cost = costLine.replace("Cost:", "").trim();

                // Validation: Spells must contain "sm" (Sorcerous Motes) or "Ritual"
                // Charms contain "m" or "i" or "wp" but not "sm".
                if (!cost.toLowerCase().contains("sm") && !cost.toLowerCase().contains("ritual")) {
                    continue; 
                }

                String keywords = "";
                String duration = "";
                int descStartIdx = i + 2;

                // Scan for Keywords and Duration (usually within next 3 lines)
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

                // Capture description and look for "Distortion"
                StringBuilder descRaw = new StringBuilder();
                int k = descStartIdx;
                while (k < lines.length) {
                    String l = lines[k].trim();
                    
                    // Stop at next potential spell (name + cost) or circle header or next chapter
                    if (k + 1 < lines.length && lines[k+1].trim().startsWith("Cost:")) break;
                    if (l.contains("Circle Spells")) break;
                    if (l.startsWith("Sorcerous Workings")) break;

                    descRaw.append(lines[k]).append("\n");
                    k++;
                }
                
                // Move i to the last line processed for this spell
                i = k - 1;

                String description = cleanDescription(descRaw.toString());
                String spellId = java.util.UUID.nameUUIDFromBytes((name.trim() + "|" + currentCircle).getBytes()).toString();

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

        Path outDir = CharmDataService.getUserSpellsPath();
        if (!Files.exists(outDir)) {
            Files.createDirectories(outDir);
        }

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

    private void extractAndSaveKeywords(String text) throws IOException {
        List<Map<String, String>> outputKeywords = new ArrayList<>();
        
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

    private void extractAndSaveEquipmentTags(PDDocument document, PDFTextStripper stripper) throws IOException {
        Map<String, List<Map<String, String>>> output = new LinkedHashMap<>();

        // Melee Tags: p. 585-587
        stripper.setStartPage(585);
        stripper.setEndPage(587);
        output.put("melee", parseTags(findTagDefinitions(stripper.getText(document)), "melee"));

        // Thrown Tags: p. 588-589
        stripper.setStartPage(588);
        stripper.setEndPage(589);
        output.put("thrown", parseTags(findTagDefinitions(stripper.getText(document)), "thrown"));

        // Archery Tags: p. 591
        stripper.setStartPage(591);
        stripper.setEndPage(591);
        output.put("archery", parseTags(findTagDefinitions(stripper.getText(document)), "archery"));

        // Armor Tags: p. 594
        stripper.setStartPage(594);
        stripper.setEndPage(594);
        output.put("armor", parseTags(findTagDefinitions(stripper.getText(document)), "armor"));

        Path outDir = CharmDataService.getUserCharmsPath().getParent();
        Path filePath = outDir.resolve("equipment_tags.json");
        try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            gson.toJson(output, writer);
        }
    }

    private List<Map<String, String>> parseTags(String text, String category) {
        List<Map<String, String>> list = new ArrayList<>();
        // Updated regex: Names start with Uppercase at start of line, followed by alpha/space/dash, ending with colon.
        // Description follows and ends at the next Tag: or page junk.
        Pattern tagPattern = Pattern.compile("(?m)^([A-Z][A-Za-z- ]+):[ \t]+(.*?)(?=\\r?\\n[A-Z][A-Za-z- ]+:\\s|\\r?\\nEX3|\\r?\\nC H A P T E R|\\z)", Pattern.DOTALL);
        Matcher matcher = tagPattern.matcher(text);
        
        while (matcher.find()) {
            String name = matcher.group(1).trim();
            if (name.equalsIgnoreCase("Tags") || name.equalsIgnoreCase("Cost")) continue;
            
            String desc = cleanDescription(matcher.group(2).trim());
            
            // Further clean descriptions that might have run into headers or page footers
            String[] stopWords = {
                "Dual Wielding", "Thrown weaPonS", "arChery weaPonS", 
                "Special Materials", "mundane armor", "Mortal Armor", 
                "Donning & Removing Armor", "Light Armor", "Medium Armor", "Heavy Armor",
                "Soak:", "Mobility Penalty:", "Hardness:",
                "Artifacts", "Artifact Weapons",
                "EX3", "CHAPTER", "T H E  G R A N D  P A N O P L Y"
            };
            for (String stop : stopWords) {
                if (desc.contains(stop)) {
                    desc = desc.split(Pattern.quote(stop))[0].trim();
                }
            }

            Map<String, String> tag = new LinkedHashMap<>();
            String id = java.util.UUID.nameUUIDFromBytes((category + "|" + name).getBytes()).toString();
            tag.put("id", id);
            tag.put("name", name);
            tag.put("description", desc);
            list.add(tag);
        }
        return list;
    }

    private String findTagDefinitions(String text) {
        // Find the "Tags" header that is followed by definitions, not a weapon's tag list.
        // In the PDF, weapon tag lists are usually "Tags: [List]" whereas the header is "Tags" alone.
        int idx = text.lastIndexOf("\nTags\n");
        if (idx == -1) idx = text.lastIndexOf("\r\nTags\r\n");
        if (idx == -1) idx = text.indexOf("The following tags are available");
        if (idx == -1) return text; // Fallback
        return text.substring(idx);
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
        text = text.replaceAll("\\n\\d+\\nEX3\\n", "\n");
        text = text.replaceAll("\\nEX3\\n", "\n");
        text = text.replaceAll("\\nCHARMS\\n", "\n");
        text = text.replaceAll("\\nCHAPTER \\d+\\n", "\n");
        text = text.replaceAll("Syntax Warning:.*", "");
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
