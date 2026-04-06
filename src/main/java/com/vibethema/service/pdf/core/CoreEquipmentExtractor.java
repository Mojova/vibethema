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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoreEquipmentExtractor {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void extractAndSaveTags(String text) throws IOException {
        Map<String, List<Map<String, String>>> output = new LinkedHashMap<>();
        
        // Find tags section - usually starts with a specific blurb
        int startIdx = text.indexOf("The following tags are available");
        String relevantText = (startIdx != -1) ? text.substring(startIdx) : text;

        output.put("melee", parseTags(relevantText, "melee"));
        output.put("thrown", parseTags(relevantText, "thrown"));
        output.put("archery", parseTags(relevantText, "archery"));
        output.put("armor", parseTags(relevantText, "armor"));
        
        Path outDir = CharmDataService.getUserCharmsPath().getParent();
        Path filePath = outDir.resolve("equipment_tags.json");
        if (!Files.exists(outDir)) Files.createDirectories(outDir);
        
        try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            gson.toJson(output, writer);
        }
    }

    private List<Map<String, String>> parseTags(String text, String category) {
        List<Map<String, String>> list = new ArrayList<>();
        // Names start with Uppercase, end with colon. Desc until next name+colon or page footer.
        Pattern tagPattern = Pattern.compile("(?m)^([A-Z][A-Za-z- ]+):[ \t]+(.*?)(?=\\r?\\n[A-Z][A-Za-z- ]+:\\s|\\r?\\nEX3|\\r?\\nC H A P T E R|\\z)", Pattern.DOTALL);
        Matcher matcher = tagPattern.matcher(text);
        
        while (matcher.find()) {
            String name = matcher.group(1).trim();
            if (name.equalsIgnoreCase("Tags") || name.equalsIgnoreCase("Cost") || name.equalsIgnoreCase("Syntax")) continue;
            
            String desc = matcher.group(2).trim()
                .replaceAll("(?i)\\bE X 3\\b", "")
                .replaceAll("(?i)\\bC H A P T E R\\s+\\d+\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
            
            Map<String, String> tag = new LinkedHashMap<>();
            String id = java.util.UUID.nameUUIDFromBytes((category + "|" + name).getBytes()).toString();
            tag.put("id", id);
            tag.put("name", name);
            tag.put("description", desc);
            list.add(tag);
        }
        return list;
    }
}
