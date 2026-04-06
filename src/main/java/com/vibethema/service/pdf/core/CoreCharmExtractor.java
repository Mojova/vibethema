package com.vibethema.service.pdf.core;

import com.vibethema.service.pdf.base.BaseCharmExtractor;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoreCharmExtractor extends BaseCharmExtractor {

    @Override
    public void extractAndSave(String text, String suffix) throws IOException {
        // Pre-process: Strip Core Book page-junk blocks before splitting
        // Patterns: "[Header/Chapter]\nEX3\n[Page#]" or similar
        text = text.replaceAll("(?m)^(?:[A-Z\\s]{5,}|SOLAR CHARMS|MARTIAL ARTS CHARMS)\\s*\\nEX3\\s*\\n\\d{3}\\s*\\n", "\n")
                   .replaceAll("(?m)^\\n\\d{3}\\s*\\nEX3\\s*\\n(?:[A-Z\\s]{5,})", "\n");

        Map<String, List<Map<String, Object>>> charmsByAbility = new HashMap<>();
        Map<String, List<Map<String, Object>>> charmsByMartialArtsStyle = new HashMap<>();
        for (String abil : ABILITIES) {
            charmsByAbility.put(abil, new ArrayList<>());
        }

        String currentMartialArtsStyle = null;

        // Split by Name + Cost line
        String[] parts = text.split("\n(?=[^\n]+\nCost:)");

        for (String part : parts) {
            String[] lines = part.trim().split("\n");
            if (lines.length < 3) continue;

            String name = "";
            String costLine = "";
            int descStartIdx = 0;

            // Detect style headers
            Pattern stylePattern = Pattern.compile("^([A-Z][\\w\\s]+ Style)$", Pattern.MULTILINE);
            Matcher styleMatcher = stylePattern.matcher(part);
            while (styleMatcher.find()) {
                currentMartialArtsStyle = styleMatcher.group(1).trim();
            }

            for (int i = 0; i < lines.length - 1; i++) {
                if (lines[i+1].trim().startsWith("Cost:")) {
                    name = lines[i].trim();
                    costLine = lines[i+1].trim();
                    descStartIdx = i + 2;
                    break;
                }
            }

            // Mins: [Ability] [Dots], Essence [Dots]
            Pattern minsPattern = Pattern.compile("Mins: ([\\w\\s]+) (\\d+), Essence (\\d+)");
            Matcher minsMatcher = minsPattern.matcher(costLine);
            if (!minsMatcher.find()) continue;

            String ability = minsMatcher.group(1).trim();
            int minAbilityValue = Integer.parseInt(minsMatcher.group(2));
            int minEssenceValue = Integer.parseInt(minsMatcher.group(3));

            if (!ABILITIES.contains(ability)) continue;
            if (ability.equalsIgnoreCase("Martial Arts") && currentMartialArtsStyle != null) {
                ability = currentMartialArtsStyle;
            }

            String type = "";
            String keywords = "";
            String duration = "";
            String rawPrereqs = "";
            String currentField = "";
            
            for (int i = descStartIdx; i < Math.min(lines.length, descStartIdx + 10); i++) {
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
                    if (isLikelyDescription(line, currentField, keywords, rawPrereqs)) break;

                    if (currentField.equals("Type")) type += " " + line;
                    else if (currentField.equals("Keywords")) keywords += ", " + line;
                    else if (currentField.equals("Duration")) duration += " " + line;
                    else if (currentField.equals("Prereqs")) rawPrereqs += " " + line;
                    descStartIdx = i + 1;
                } else {
                    break;
                }
            }

            List<Object> prereqGroups = new ArrayList<>();
            String cleanPrereqs = rawPrereqs.trim();
            if (cleanPrereqs.toLowerCase().startsWith("none")) {
                cleanPrereqs = cleanPrereqs.substring(4).trim();
                if (cleanPrereqs.startsWith(",")) cleanPrereqs = cleanPrereqs.substring(1).trim();
            }

            if (!cleanPrereqs.isEmpty()) {
                // Detect patterns like "Any two Keen (Sense) Techniques"
                Pattern anyKeenPattern = Pattern.compile("(?i)Any (two|three|\\d+) Keen \\(Sense\\) Techniques");
                Matcher keenMatcher = anyKeenPattern.matcher(cleanPrereqs);
                
                // Detect patterns like "Awakening Eye + Any 3 non-Excellency Awareness Charms"
                // Note: PDF artifacts may result in "non-Ex- cellency"
                Pattern complexAnyPattern = Pattern.compile("(?i)(.*?) \\+ Any (\\d+) (?:non-Ex-?\\s*cellency )?(?:[\\w\\s-]+? )?Charms");
                Matcher complexMatcher = complexAnyPattern.matcher(cleanPrereqs);

                if (keenMatcher.find()) {
                    Map<String, Object> group = new LinkedHashMap<>();
                    group.put("label", keenMatcher.group(0));
                    group.put("names", Arrays.asList("Keen Sight Technique", "Keen Hearing and Touch Technique", "Keen Taste and Smell Technique"));
                    group.put("minCount", parseNumber(keenMatcher.group(1)));
                    prereqGroups.add(group);
                } else if (complexMatcher.find()) {
                    String mandatoryName = complexMatcher.group(1).trim();
                    String countStr = complexMatcher.group(2);
                    String fullMatch = complexMatcher.group(0).toLowerCase();
                    boolean nonExcellency = fullMatch.contains("non-excellency") || 
                                            fullMatch.contains("non-ex-cellency") ||
                                            fullMatch.contains("non-ex- cellency");

                    // Mandatory part
                    Map<String, Object> mandatoryGroup = new LinkedHashMap<>();
                    mandatoryGroup.put("names", Arrays.asList(mandatoryName));
                    mandatoryGroup.put("minCount", 0);
                    prereqGroups.add(mandatoryGroup);

                    // Optional part with metadata for resolution
                    Map<String, Object> optionalGroup = new LinkedHashMap<>();
                    String flags = nonExcellency ? "|non-Excellency" : "";
                    optionalGroup.put("names", Arrays.asList("__OTHERS_EXCEPT:" + mandatoryName + flags + "__"));
                    optionalGroup.put("minCount", Integer.parseInt(countStr));
                    prereqGroups.add(optionalGroup);
                } else {
                    // Fallback to simple comma-separated list
                    for (String p : cleanPrereqs.split(",")) {
                        String trimmed = p.replaceAll("\\s+", " ").trim();
                        if (!trimmed.isEmpty() && !trimmed.startsWith("The ") && trimmed.length() < 100) {
                            prereqGroups.add(trimmed);
                        }
                    }
                }
            }

            type = cleanField(type);
            keywords = cleanField(keywords);
            duration = cleanField(duration);

            Pattern costPattern = Pattern.compile("Cost: (.*?);");
            Matcher costMatcher = costPattern.matcher(costLine);
            String cost = costMatcher.find() ? costMatcher.group(1) : costLine.replace("Cost: ", "").split(";")[0];

            StringBuilder descRaw = new StringBuilder();
            for (int i = descStartIdx; i < lines.length; i++) {
                String line = lines[i].trim();
                // If it's a section header or sidebar, we've gone past the description
                if (ABILITIES.contains(line) || isSidebarLine(line)) break;
                descRaw.append(lines[i]).append("\n");
            }

            String fullText = cleanDescription(descRaw.toString());
            String charmId = UUID.nameUUIDFromBytes((name.trim() + "|" + ability.trim()).getBytes()).toString();

            Map<String, Object> charmMap = new LinkedHashMap<>();
            charmMap.put("id", charmId);
            charmMap.put("name", name);
            charmMap.put("ability", ability);
            charmMap.put("minAbility", minAbilityValue);
            charmMap.put("minEssence", minEssenceValue);
            charmMap.put("prerequisites", prereqGroups);
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

        List<Map<String, Object>> allCharms = new ArrayList<>();
        charmsByAbility.values().forEach(allCharms::addAll);
        charmsByMartialArtsStyle.values().forEach(allCharms::addAll);
        resolvePrerequisites(allCharms);

        saveCharms(charmsByAbility, suffix, false);
        saveCharms(charmsByMartialArtsStyle, suffix, true);
    }

    private String cleanField(String val) {
        if (val == null) return "";
        // Use a more aggressive regex to catch header/footer junk in fields
        String cleaned = val.replaceAll("(?i)\\s*C\\s*H\\s*A\\s*R\\s*M\\s*S\\s*", " ")
                             .replaceAll("(?i)\\s*E\\s*X\\s*3\\s*", " ")
                             .replaceAll("\\s*\\d{3}\\s*", " ")
                             .replaceAll("\\s+", " ")
                             .trim();
        return cleaned;
    }

    private boolean isLikelyDescription(String line, String currentField, String keywords, String rawPrereqs) {
        if (currentField.equals("Keywords") && keywords.equalsIgnoreCase("none")) return true;
        if (currentField.equals("Prereqs") && rawPrereqs.equalsIgnoreCase("none")) return true;

        String trimmed = line.trim();
        if (trimmed.isEmpty()) return false;

        // ALL CAPS lines are usually headers or sidebars
        if (trimmed.equals(trimmed.toUpperCase()) && trimmed.length() > 5) return true;
        
        // Sidebar headers often start with "ON " (e.g., "ON SURPRISE ANTICIPATION METHOD")
        if (trimmed.startsWith("ON ") && trimmed.length() < 100) return true;
        
        // Ability headers
        if (ABILITIES.contains(trimmed)) return true;

        return trimmed.startsWith("The ") || trimmed.startsWith("This ") ||
               trimmed.startsWith("If ") || trimmed.startsWith("When ") ||
               trimmed.startsWith("A ") || trimmed.startsWith("An ") ||
               trimmed.startsWith("Solar ") || trimmed.startsWith("Lawgiver ") ||
               trimmed.startsWith("Exalt ") || trimmed.startsWith("Once ") ||
               trimmed.startsWith("After ") || trimmed.startsWith("At ") ||
               trimmed.startsWith("By ") || trimmed.startsWith("With ") ||
               trimmed.startsWith("Cast ") || trimmed.startsWith("Solars ") ||
               trimmed.startsWith("Drawing ") || trimmed.startsWith("Charging ") ||
               trimmed.startsWith("Seething ") || trimmed.startsWith("Palming ") ||
               trimmed.startsWith("Honing ") || trimmed.startsWith("Clearing ") ||
               trimmed.startsWith("While ") || trimmed.startsWith("To ") ||
               trimmed.startsWith("Holding ") || trimmed.startsWith("Sensing ") ||
               trimmed.startsWith("Through ") || trimmed.startsWith("Even ") ||
               trimmed.startsWith("Like ") || trimmed.startsWith("Accepting ") ||
               trimmed.startsWith("Given ") || trimmed.startsWith("Tuning ") ||
               trimmed.startsWith("Homing ") || trimmed.startsWith("Channeling ") ||
               trimmed.startsWith("During ") || trimmed.startsWith("Throughout ") ||
               trimmed.startsWith("Under ") || trimmed.startsWith("Across ") ||
               trimmed.startsWith("Within ") || trimmed.startsWith("Upon ") ||
               trimmed.startsWith("Because ") || trimmed.startsWith("Since ") ||
               trimmed.startsWith("Characters ") || trimmed.startsWith("Sometimes ") ||
               trimmed.startsWith("Striking ") || trimmed.startsWith("Summoning ") ||
               trimmed.startsWith("Fearless ") || trimmed.startsWith("Racing ") ||
               trimmed.startsWith("Attuned ") || trimmed.startsWith("Striving ") ||
               trimmed.startsWith("Meditating ") || trimmed.startsWith("It is ") ||
               trimmed.startsWith("Using ") || trimmed.startsWith("Focusing ") ||
               trimmed.startsWith("Relentless ") || trimmed.startsWith("Once per ") ||
               trimmed.startsWith("In ") || trimmed.startsWith("Lightening ") ||
               trimmed.startsWith("That’s ") || trimmed.startsWith("Note: ");
    }

    @Override
    protected String cleanDescription(String text) {
        // Surgical sidebar removal for Core book
        text = text.replaceAll("(?s)WHEN DO I NEED TO AIM\\?.*?waive the aim action\\.", "");
        text = text.replaceAll("(?s)MASTER’S HAND: SOLAR MASTERY AND TERRESTRIAL EFFECTS.*?(?=CHAPTER|EX3|\\d{3})", "");
        text = text.replaceAll("(?s)ON TEN OX MEDITATION.*?(?=ment action for the round|CHAPTER|EX3|\\d{3}\\n)", "");
        return super.cleanDescription(text);
    }

    private int parseNumber(String val) {
        if (val == null) return 0;
        switch (val.toLowerCase()) {
            case "one": return 1;
            case "two": return 2;
            case "three": return 3;
            case "four": return 4;
            case "five": return 5;
            default:
                try {
                    return Integer.parseInt(val);
                } catch (NumberFormatException e) {
                    return 0;
                }
        }
    }
}
