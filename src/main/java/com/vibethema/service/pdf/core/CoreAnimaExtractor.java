package com.vibethema.service.pdf.core;

import com.vibethema.model.mystic.AnimaLevel;
import com.vibethema.service.DefinitionId;
import com.vibethema.service.PathService;
import com.vibethema.service.pdf.base.BaseCharmExtractor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Imports descriptive anima effects only; costs and conditions remain in the effect text. */
public class CoreAnimaExtractor extends BaseCharmExtractor {
    private static final Pattern HEADING =
            Pattern.compile("(?m)^(?:(Dawn|Zenith|Twilight|Night|Eclipse) )?Anima Effects[ \\t]*$");
    private static final List<String> GROUPS =
            List.of("Universal", "Dawn", "Zenith", "Twilight", "Night", "Eclipse");

    @Override
    public void extractAndSave(String text, String suffix) throws IOException {
        // Printed pages 175-177 span PDF pages 176-178. Remove running page furniture before
        // splitting so the Eclipse effects continue cleanly across the page boundary.
        text =
                text.replace("\r\n", "\n")
                        .replaceAll(
                                "(?m)^["
                                    + " \\t]*(?:C\\s*H\\s*A\\s*P\\s*T\\s*E\\s*R\\s+4|T\\s*R\\s*A\\s*I\\s*T\\s*S|E\\s*X\\s*3|\\d{3})["
                                    + " \\t]*$",
                                "");
        int end = text.indexOf("\nCharacter Advancement");
        if (end < 0) throw new IOException("Missing end of Core anima effects section");
        text = text.substring(0, end);

        var matcher = HEADING.matcher(text);
        List<String> names = new ArrayList<>();
        List<Integer> starts = new ArrayList<>();
        List<Integer> ends = new ArrayList<>();
        while (matcher.find()) {
            names.add(matcher.group(1) == null ? "Universal" : matcher.group(1));
            starts.add(matcher.start());
            ends.add(matcher.end());
        }
        if (!names.equals(GROUPS)) throw new IOException("Incomplete Core anima effects section");

        List<Map<String, Object>> abilities = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            String section =
                    text.substring(
                            ends.get(i), i + 1 < starts.size() ? starts.get(i + 1) : text.length());
            // The universal section ends with a transition to caste-specific benefits.
            section = section.split("In addition to these universal effects,", 2)[0];
            String[] parts = section.split("(?m)^[ \\t]*•[ \\t]*", -1);
            int expected = i == 0 ? 2 : 3;
            if (parts.length != expected + 1) {
                throw new IOException(
                        "Expected " + expected + " anima effects for " + names.get(i));
            }
            List<String> effects = new ArrayList<>();
            for (int j = 1; j < parts.length; j++) {
                String effect = cleanEffect(parts[j]);
                if (effect.isEmpty())
                    throw new IOException("Empty anima effect for " + names.get(i));
                effects.add(effect);
            }
            Map<String, Object> ability = new LinkedHashMap<>();
            ability.put("id", DefinitionId.from(sourceId, "anima", "SOLAR", names.get(i)));
            ability.put("sourceId", sourceId);
            ability.put("exaltTypes", List.of("SOLAR"));
            // An empty caste list denotes effects shared by all castes of the listed Exalt types.
            ability.put(
                    "castes", i == 0 ? List.of() : List.of(names.get(i).toUpperCase(Locale.ROOT)));
            ability.put(
                    "name", i == 0 ? "Universal Anima Effects" : names.get(i) + " Anima Effects");
            ability.put("description", cleanEffect(parts[0]));
            ability.put("effects", effects);
            abilities.add(ability);
        }

        abilities.add(1, extractLevels(text, starts.getFirst()));

        var outDir =
                (overrideOutputPath != null
                                ? overrideOutputPath
                                : PathService.getOfficialSourcePath(sourceId))
                        .resolve("anima");
        Files.createDirectories(outDir);
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("version", "1.1.0");
        wrapper.put("sourceId", sourceId);
        wrapper.put("animaAbilities", abilities);
        try (var writer =
                Files.newBufferedWriter(
                        outDir.resolve("anima" + suffix + ".json"), StandardCharsets.UTF_8)) {
            gson.toJson(wrapper, writer);
        }
    }

    private Map<String, Object> extractLevels(String text, int effectsStart) throws IOException {
        int tableStart = text.indexOf("Anima Level");
        int activationStart = text.lastIndexOf("When your character spends", tableStart);
        int decayStart = text.indexOf("It takes about an hour", tableStart);
        if (tableStart < 0
                || activationStart < 0
                || decayStart < tableStart
                || decayStart >= effectsStart) {
            throw new IOException("Incomplete Core anima levels section");
        }
        String table = text.substring(tableStart, decayStart);
        var matcher = Pattern.compile("(?m)^(Dim|Glowing|Burning|Bonfire/)[ \\t]+").matcher(table);
        List<String> names = new ArrayList<>();
        List<Integer> starts = new ArrayList<>();
        List<Integer> ends = new ArrayList<>();
        while (matcher.find()) {
            names.add(matcher.group(1));
            starts.add(matcher.start());
            ends.add(matcher.end());
        }
        if (!names.equals(List.of("Dim", "Glowing", "Burning", "Bonfire/"))) {
            throw new IOException("Expected four Core anima levels");
        }
        List<AnimaLevel> levels = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            String description =
                    table.substring(
                            ends.get(i),
                            i + 1 < starts.size() ? starts.get(i + 1) : table.length());
            if (i == 3) description = description.replaceFirst("(?m)^Iconic[ \\t]+", "");
            description = cleanEffect(description);
            if (description.isBlank()) throw new IOException("Empty Core anima level description");
            levels.add(new AnimaLevel(i == 3 ? "Bonfire/Iconic" : names.get(i), description));
        }
        Map<String, Object> group = new LinkedHashMap<>();
        group.put("id", DefinitionId.from(sourceId, "anima", "SOLAR", "levels"));
        group.put("sourceId", sourceId);
        group.put("exaltTypes", List.of("SOLAR"));
        group.put("castes", List.of());
        group.put("name", "Anima Levels");
        group.put("description", cleanEffect(text.substring(activationStart, tableStart)));
        group.put("levels", levels);
        group.put("effects", List.of(cleanEffect(text.substring(decayStart, effectsStart))));
        return group;
    }

    private String cleanEffect(String text) {
        return text.replaceAll("[-\\u00AD][ \\t]*\\n\\s*(?=[a-z])", "")
                .replace("\u00AD", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
