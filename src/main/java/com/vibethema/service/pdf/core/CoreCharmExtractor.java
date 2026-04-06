package com.vibethema.service.pdf.core;

import com.vibethema.service.pdf.base.BaseCharmExtractor;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoreCharmExtractor extends BaseCharmExtractor {

    @Override
    public void extractAndSave(String text, String suffix) throws IOException {
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

            if (name.isEmpty() || costLine.isEmpty()) continue;
            if (name.endsWith("Style")) {
                currentMartialArtsStyle = name;
                continue;
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

            List<String> prereqs = new ArrayList<>();
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

            type = cleanField(type);
            keywords = cleanField(keywords);
            duration = cleanField(duration);

            Pattern costPattern = Pattern.compile("Cost: (.*?);");
            Matcher costMatcher = costPattern.matcher(costLine);
            String cost = costMatcher.find() ? costMatcher.group(1) : costLine.replace("Cost: ", "").split(";")[0];

            StringBuilder descRaw = new StringBuilder();
            for (int i = descStartIdx; i < lines.length; i++) {
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

        return line.startsWith("The ") || line.startsWith("This ") ||
               line.startsWith("If ") || line.startsWith("When ") ||
               line.startsWith("A ") || line.startsWith("An ") ||
               line.startsWith("Solar ") || line.startsWith("Lawgiver ") ||
               line.startsWith("Exalt ") || line.startsWith("Once ") ||
               line.startsWith("After ") || line.startsWith("At ") ||
               line.startsWith("By ") || line.startsWith("With ") ||
               line.startsWith("Drawing ") || line.startsWith("Charging ") ||
               line.startsWith("Seething ") || line.startsWith("Palming ") ||
               line.startsWith("Honing ") || line.startsWith("Clearing ") ||
               line.startsWith("While ") || line.startsWith("To ") ||
               line.startsWith("Holding ") || line.startsWith("Sensing ") ||
               line.startsWith("Through ") || line.startsWith("Even ") ||
               line.startsWith("Like ") || line.startsWith("Accepting ") ||
               line.startsWith("Given ") || line.startsWith("Tuning ") ||
               line.startsWith("Homing ") || line.startsWith("Channeling ") ||
               line.startsWith("During ") || line.startsWith("Throughout ") ||
               line.startsWith("Under ") || line.startsWith("Across ") ||
               line.startsWith("Within ") || line.startsWith("Upon ") ||
               line.startsWith("Because ") || line.startsWith("Since ") ||
               line.startsWith("Characters ") || line.startsWith("Sometimes ");
    }

    @Override
    protected String cleanDescription(String text) {
        // Surgical sidebar removal for Core book
        text = text.replaceAll("(?s)WHEN DO I NEED TO AIM\\?.*?waive the aim action\\.", "");
        text = text.replaceAll("(?s)MASTER’S HAND: SOLAR MASTERY AND TERRESTRIAL EFFECTS.*?(?=CHAPTER|EX3|\\d{3})", "");
        return super.cleanDescription(text);
    }
}
