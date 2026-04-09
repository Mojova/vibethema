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
        text =
                text.replaceAll(
                                "(?m)^(?:[A-Z\\s]{5,}|SOLAR CHARMS|MARTIAL ARTS CHARMS)\\s*\\n"
                                        + "E\\s*X\\s*3\\s*\\n"
                                        + "\\d{3}\\s*\\n",
                                "\n")
                        .replaceAll("(?m)^\\n\\d{3}\\s*\\nE\\s*X\\s*3\\s*\\n(?:[A-Z\\s]{5,})", "\n");

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
                if (lines[i + 1].trim().startsWith("Cost:")) {
                    name = lines[i].trim();
                    costLine = lines[i + 1].trim();
                    descStartIdx = i + 2;
                    break;
                }
            }

            // Mins: [Ability] [Dots], Essence [Dots] - handle possible newlines by joining with next line if needed
            String fullMinsLine = costLine;
            if (!costLine.contains("Essence") && descStartIdx < lines.length) {
                fullMinsLine += " " + lines[descStartIdx].trim();
                descStartIdx++;
            }

            Pattern minsPattern = Pattern.compile("Mins: (.*?) (\\d+), Essence (\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher minsMatcher = minsPattern.matcher(fullMinsLine);
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

                // Skip known sidebar/page junk lines in the metadata block
                if (isSidebarLine(line)) {
                    descStartIdx = i + 1;
                    continue;
                }

                if (line.startsWith("Type:")) {
                    type = line.replace("Type:", "").trim();
                    if (type.contains("Keywords:")) {
                        String[] bits = type.split("Keywords:");
                        type = bits[0].trim();
                        keywords = bits[1].trim();
                        currentField = "Keywords";
                    } else {
                        currentField = "Type";
                    }
                    descStartIdx = i + 1;
                } else if (line.startsWith("Keywords:")) {
                    keywords = line.replace("Keywords:", "").trim();
                    if (keywords.contains("Duration:")) {
                        String[] bits = keywords.split("Duration:");
                        keywords = bits[0].trim();
                        duration = bits[1].trim();
                        currentField = "Duration";
                    } else {
                        currentField = "Keywords";
                    }
                    descStartIdx = i + 1;
                } else if (line.startsWith("Duration:")) {
                    duration = line.replace("Duration:", "").trim();
                    if (duration.contains("Prerequisite Charms:")) {
                        String[] bits = duration.split("Prerequisite Charms:");
                        duration = bits[0].trim();
                        rawPrereqs = bits[1].trim();
                        currentField = "Prereqs";
                    } else {
                        currentField = "Duration";
                    }
                    descStartIdx = i + 1;
                } else if (line.startsWith("Prerequisite Charms:")) {
                    rawPrereqs = line.replace("Prerequisite Charms:", "").trim();
                    currentField = "Prereqs";
                    descStartIdx = i + 1;
                } else if (!currentField.isEmpty()
                        && !line.contains(":")
                        && !line.contains("Cost:")) {
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
            }

            if (!cleanPrereqs.isEmpty()) {
                if (cleanPrereqs.startsWith(",")) cleanPrereqs = cleanPrereqs.substring(1).trim();

                // Join hyphenated words broken across lines
                // Heuristic: dash followed by lowercase means a single word was broken (e.g.
                // Intercept- ing)
                //            dash followed by uppercase means a compound word (e.g. River- Binding)
                // We use a comprehensive set of Unicode dash characters to catch all PDF artifacts.
                String dashPattern = "[\\u002D\\u00AD\\u2010\\u2011\\u2012\\u2013\\u2014]";
                cleanPrereqs =
                        cleanPrereqs
                                .replaceAll(dashPattern + "\\s*(?=[a-z])", "")
                                .replaceAll(dashPattern + "\\s+(?=[A-Z])", "-");

                // Detect patterns like "Any two Keen (Sense) Techniques"
                Pattern anyKeenPattern =
                        Pattern.compile("(?i)Any (two|three|\\d+) Keen \\(Sense\\) Techniques");
                Matcher keenMatcher = anyKeenPattern.matcher(cleanPrereqs);

                // Detect patterns like "Awakening Eye + Any 3 non-Excellency Awareness Charms"
                // Note: PDF artifacts may result in "non-Ex- cellency"
                Pattern complexAnyPattern =
                        Pattern.compile(
                                "(?i)(.*?) \\+ Any (\\d+) (?:non-Ex-?\\s*cellency )?(?:[\\w\\s-]+?"
                                        + " )?Charms");
                Matcher complexMatcher = complexAnyPattern.matcher(cleanPrereqs);

                if (keenMatcher.find()) {
                    Map<String, Object> group = new LinkedHashMap<>();
                    group.put("label", keenMatcher.group(0));
                    group.put(
                            "names",
                            Arrays.asList(
                                    "Keen Sight Technique",
                                    "Keen Hearing and Touch Technique",
                                    "Keen Taste and Smell Technique"));
                    group.put("minCount", parseNumber(keenMatcher.group(1)));
                    prereqGroups.add(group);
                } else if (complexMatcher.find()) {
                    String mandatoryName = complexMatcher.group(1).trim();
                    String countStr = complexMatcher.group(2);
                    String fullMatch = complexMatcher.group(0).toLowerCase();
                    boolean nonExcellency =
                            fullMatch.contains("non-excellency")
                                    || fullMatch.contains("non-ex-cellency")
                                    || fullMatch.contains("non-ex- cellency");

                    // Mandatory part
                    Map<String, Object> mandatoryGroup = new LinkedHashMap<>();
                    mandatoryGroup.put("names", Arrays.asList(mandatoryName));
                    mandatoryGroup.put("minCount", 0);
                    prereqGroups.add(mandatoryGroup);

                    // Optional part with metadata for resolution
                    Map<String, Object> optionalGroup = new LinkedHashMap<>();
                    String flags = nonExcellency ? "|non-Excellency" : "";
                    optionalGroup.put(
                            "names",
                            Arrays.asList("__OTHERS_EXCEPT:" + mandatoryName + flags + "__"));
                    optionalGroup.put("minCount", Integer.parseInt(countStr));
                    prereqGroups.add(optionalGroup);
                } else {
                    // Fallback to simple comma-separated list
                    for (String p : cleanPrereqs.split(",")) {
                        String trimmed = p.replaceAll("\\s+", " ").trim();
                        if (trimmed.isEmpty()
                                || trimmed.startsWith("The ")
                                || trimmed.length() > 100) continue;

                        Matcher countMatcher =
                                Pattern.compile("(.*?) \\(x(\\d+)\\)").matcher(trimmed);
                        Map<String, Object> group = new LinkedHashMap<>();
                        if (countMatcher.find()) {
                            group.put("names", Arrays.asList(countMatcher.group(1).trim()));
                            group.put("minCount", Integer.parseInt(countMatcher.group(2)));
                        } else {
                            // Produce a Map group for consistency
                            group.put("names", Arrays.asList(trimmed));
                            group.put("minCount", 0);
                        }
                        prereqGroups.add(group);
                    }
                }
            }

            type = cleanField(type);
            keywords = cleanField(keywords);
            duration = cleanField(duration);

            Pattern costPattern = Pattern.compile("Cost: (.*?);");
            Matcher costMatcher = costPattern.matcher(costLine);
            String cost =
                    costMatcher.find()
                            ? costMatcher.group(1)
                            : costLine.replace("Cost: ", "").split(";")[0];

            StringBuilder descRaw = new StringBuilder();
            for (int i = descStartIdx; i < lines.length; i++) {
                String line = lines[i].trim();
                // If it's a section header, we've gone past the description
                // But do NOT break on sidebars here, as they can occur mid-description
                if (ABILITIES.contains(line)) break;
                descRaw.append(lines[i]).append("\n");
            }

            String fullText = cleanDescription(descRaw.toString());
            String charmId =
                    UUID.nameUUIDFromBytes((name.trim() + "|" + ability.trim()).getBytes())
                            .toString();

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
            // Automate Stackable if 'repurchase' is mentioned in the full text
            if (fullText.toLowerCase().contains("repurchase") && !kwList.contains("Stackable")) {
                kwList.add("Stackable");
            }
            charmMap.put("keywords", kwList);
            charmMap.put("duration", duration);
            charmMap.put("fullText", fullText);
            charmMap.put("rawData", part.trim());

            if (charmsByAbility.containsKey(ability)) {
                charmsByAbility.get(ability).add(charmMap);
            } else {
                charmsByMartialArtsStyle
                        .computeIfAbsent(ability, k -> new ArrayList<>())
                        .add(charmMap);
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
        String cleaned =
                val.replaceAll("(?i)\\s*C\\s*H\\s*A\\s*R\\s*M\\s*S\\s*", " ")
                        .replaceAll("(?i)\\s*E\\s*X\\s*3\\s*", " ")
                        .replaceAll("\\s*\\d{3}\\s*", " ")
                        .replaceAll("\\s+", " ")
                        .trim();
        return cleaned;
    }

    private boolean isLikelyDescription(
            String line, String currentField, String keywords, String rawPrereqs) {
        if (currentField.equals("Keywords") && keywords.equalsIgnoreCase("none")) return true;
        if (currentField.equals("Prereqs") && rawPrereqs.equalsIgnoreCase("none")) return true;

        String trimmed = line.trim();
        if (trimmed.isEmpty()) return false;

        // ALL CAPS lines are usually headers or sidebars
        if (trimmed.equals(trimmed.toUpperCase()) && trimmed.length() > 5) {
            // Ignore known page junk that isn't a real description or header
            if (isSidebarLine(trimmed)) return false;
            return true;
        }

        // If we have prerequisites (the last likely field),
        // the next non-metadata line is likely the description.
        if (currentField.equals("Prereqs") && !rawPrereqs.isEmpty()) {
            // A description line almost always starts with a capital and isn't another field.
            // We differentiate from broken prerequisite fragments by length or ending punctuation.
            if (Character.isUpperCase(trimmed.charAt(0))
                    && !trimmed.contains(":")
                    && !trimmed.startsWith("Cost:")) {
                if (trimmed.length() > 25 || trimmed.endsWith(".")) return true;
            }
            return false;
        }

        // Sidebar headers often start with "ON " (e.g., "ON SURPRISE ANTICIPATION METHOD")
        if (trimmed.startsWith("ON ") && trimmed.length() < 100) return true;

        // Ability headers
        if (ABILITIES.contains(trimmed)) return true;

        return trimmed.startsWith("The ")
                || trimmed.startsWith("This ")
                || trimmed.startsWith("If ")
                || trimmed.startsWith("When ")
                || trimmed.startsWith("A ")
                || trimmed.startsWith("An ")
                || trimmed.startsWith("Solar ")
                || trimmed.startsWith("Lawgiver ")
                || trimmed.startsWith("Exalt ")
                || trimmed.startsWith("Once ")
                || trimmed.startsWith("After ")
                || trimmed.startsWith("At ")
                || trimmed.startsWith("By ")
                || trimmed.startsWith("With ")
                || trimmed.startsWith("Cast ")
                || trimmed.startsWith("Solars ")
                || trimmed.startsWith("Drawing ")
                || trimmed.startsWith("Charging ")
                || trimmed.startsWith("Seething ")
                || trimmed.startsWith("Palming ")
                || trimmed.startsWith("Honing ")
                || trimmed.startsWith("Clearing ")
                || trimmed.startsWith("While ")
                || trimmed.startsWith("To ")
                || trimmed.startsWith("Holding ")
                || trimmed.startsWith("Sensing ")
                || trimmed.startsWith("Through ")
                || trimmed.startsWith("Even ")
                || trimmed.startsWith("Like ")
                || trimmed.startsWith("Accepting ")
                || trimmed.startsWith("Given ")
                || trimmed.startsWith("Tuning ")
                || trimmed.startsWith("Homing ")
                || trimmed.startsWith("Channeling ")
                || trimmed.startsWith("During ")
                || trimmed.startsWith("Throughout ")
                || trimmed.startsWith("Under ")
                || trimmed.startsWith("Across ")
                || trimmed.startsWith("Within ")
                || trimmed.startsWith("Upon ")
                || trimmed.startsWith("Because ")
                || trimmed.startsWith("Since ")
                || trimmed.startsWith("Characters ")
                || trimmed.startsWith("Sometimes ")
                || trimmed.startsWith("Striking ")
                || trimmed.startsWith("Summoning ")
                || trimmed.startsWith("Fearless ")
                || trimmed.startsWith("Racing ")
                || trimmed.startsWith("Attuned ")
                || trimmed.startsWith("Striving ")
                || trimmed.startsWith("Meditating ")
                || trimmed.startsWith("It is ")
                || trimmed.startsWith("Using ")
                || trimmed.startsWith("Focusing ")
                || trimmed.startsWith("Relentless ")
                || trimmed.startsWith("Once per ")
                || trimmed.startsWith("In ")
                || trimmed.startsWith("Lightening ")
                || trimmed.startsWith("Locked ")
                || trimmed.startsWith("Hardening ")
                || trimmed.startsWith("Empowered ")
                || trimmed.startsWith("As ")
                || trimmed.startsWith("Tearing ")
                || trimmed.startsWith("Hers ")
                || trimmed.startsWith("Feeling ")
                || trimmed.startsWith("Driven ")
                || trimmed.startsWith("Ripping ")
                || trimmed.startsWith("Tossing ")
                || trimmed.startsWith("Assuming ")
                || trimmed.startsWith("Lifting ")
                || trimmed.startsWith("Pulling ")
                || trimmed.startsWith("That’s ")
                || trimmed.startsWith("Note: ");
    }

    @Override
    protected String cleanDescription(String text) {
        // Surgical sidebar removal for Core book
        // Handles sidebars which often interrupt mid-sentence or mid-word.
        text = text.replaceAll("(?s)WHEN DO I NEED TO AIM\\?.*?waive the aim action\\.", "");
        text = text.replaceAll("(?s)WHEN DO I NEED TO AIM\\?.*?(?=laden shot|CHAPTER|EX3|\\d{3}\\n)", "");
        text =
                text.replaceAll(
                        "(?s)MASTER’S HAND: SOLAR MASTERY AND TERRESTRIAL"
                                + " EFFECTS.*?(?=CHAPTER|EX3|\\d{3})",
                        "");
        text =
                text.replaceAll(
                        "(?s)ON TEN OX MEDITATION.*?(?=on her next turn|ment action for the"
                                + " round|CHAPTER|EX3|\\d{3}\\n"
                                + ")",
                        "");
        text =
                text.replaceAll(
                        "(?s)ON HUNDRED SHADOW WAYS.*?(?=applicable|On her next|On the next|CHAPTER|EX3|\\d{3}\\n"
                                + ")",
                        "");
        // Awareness sidebars
        text = text.replaceAll("(?s)ON\\s+SURPRISE\\s+ANTICIPATION\\s+METHOD.*?(?=The Iron Wolf|CHAPTER|EX3|\\d{3}\\n)", "");
        text = text.replaceAll("(?s)That’s\\s+not\\s+a\\s+typo.*?investment\\s+of\\s+experience\\s+points\\.?", "");
        text = text.replaceAll("(?s)SPACE-SAVING\\s+CONCESSION.*?extends\\s+all\\s+of\\s+them\\.", "");
        
        // Brawl sidebars
        text = text.replaceAll("(?s)AN\\s+EXAMPLE\\s+OF\\s+FALLING\\s+HAMMER\\s+STRIKE.*?her\\s+next\\s+attack\\.", "");
        text = text.replaceAll("(?s)ON\\s+THUNDERCLAP\\s+RUSH\\s+ATTACK.*?before\\s+she\\s+makes\\s+her\\s+attack\\s+roll\\.", "");
        text = text.replaceAll("(?s)FELLING\\s+GIGANTIC\\s+FOES.*?grapple\\s+gigantic\\s+foes\\s+in\\s+the\\s+first\\s+place\\.?", "");
        
        // Craft sidebars
        text = text.replaceAll("(?s)ON\\s+POINT-GENERATING\\s+CHARMS.*?use\\s+Craft\\s+intermittently\\.", "");
        text = text.replaceAll("(?s)SHOCKWAVE\\s+TECHNIQUE.*?target\\s+is\\s+thrown\\.", "");
        
        return super.cleanDescription(text);
    }

    private int parseNumber(String val) {
        if (val == null) return 0;
        switch (val.toLowerCase()) {
            case "one":
                return 1;
            case "two":
                return 2;
            case "three":
                return 3;
            case "four":
                return 4;
            case "five":
                return 5;
            default:
                try {
                    return Integer.parseInt(val);
                } catch (NumberFormatException e) {
                    return 0;
                }
        }
    }
}
