package com.vibethema.service.pdf.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vibethema.service.DefinitionId;
import com.vibethema.service.EquipmentDataService;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoreEquipmentExtractor {
    private static final Pattern PAGE_FURNITURE =
            Pattern.compile(
                    "(?m)^\\h*(?:C H A P T E R\\h+9|T H E\\h+G R A N D\\h+P A N O P L ?Y|"
                            + "E\\h*X\\h*3|[0-9]{3})\\h*\\R");
    private static final String TAG_LABEL = "^\\h*([A-Z][A-Za-z -]*):";
    private static final Pattern TAG_ENTRY =
            Pattern.compile("(?ms)" + TAG_LABEL + "\\h*(.*?)(?=" + TAG_LABEL + "|\\z)");

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Path overrideOutputPath;
    private String sourceId = "exalted-3e-core";

    public void setOverrideOutputPath(Path path) {
        this.overrideOutputPath = path;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public void extractAndSaveTags(String text) throws IOException {
        // A tag description can continue after a running header on the next page.
        String content = PAGE_FURNITURE.matcher(text).replaceAll("");
        Map<String, List<Map<String, String>>> output = new LinkedHashMap<>();
        output.put("melee", parseTags(content, "melee", null, "Dual Wielding"));
        output.put("thrown", parseTags(content, "thrown", "Thrown Weapons", "Archery Weapons"));
        output.put(
                "archery", parseTags(content, "archery", "Archery Weapons", "Special Materials"));
        output.put("armor", parseTags(content, "armor", "Mundane Armor", "Artifacts"));

        Path filePath =
                overrideOutputPath != null
                        ? overrideOutputPath.resolve("equipment/equipment_tags.json")
                        : EquipmentDataService.getEquipmentTagsPath();
        Files.createDirectories(filePath.getParent());

        try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            gson.toJson(output, writer);
        }
    }

    private List<Map<String, String>> parseTags(
            String text, String category, String startHeading, String endHeading)
            throws IOException {
        int start = startHeading == null ? 0 : findHeading(text, startHeading, 0).end();
        int end = findHeading(text, endHeading, start).start();
        String section = text.substring(start, end);
        int tagsStart = findHeading(section, "Tags", 0).end();
        Matcher matcher = TAG_ENTRY.matcher(section.substring(tagsStart));
        List<Map<String, String>> list = new ArrayList<>();
        while (matcher.find()) {
            String name = matcher.group(1).trim();
            String desc =
                    matcher.group(2).replaceAll("-\\h*\\R\\h*", "-").replaceAll("\\s+", " ").trim();

            Map<String, String> tag = new LinkedHashMap<>();
            String id = DefinitionId.forEquipment(sourceId, category, name);
            tag.put("id", id);
            tag.put("sourceId", sourceId);
            tag.put("exaltTypes", "SOLAR");
            tag.put("name", name);
            tag.put("description", desc);
            list.add(tag);
        }
        if (list.isEmpty()) throw new IOException("No Core equipment tags found for " + category);
        return list;
    }

    private static Matcher findHeading(String text, String heading, int start) throws IOException {
        Matcher matcher =
                Pattern.compile("(?m)^\\h*" + Pattern.quote(heading) + "\\h*$").matcher(text);
        if (!matcher.find(start)) {
            throw new IOException("Missing Core equipment heading: " + heading);
        }
        return matcher;
    }
}
