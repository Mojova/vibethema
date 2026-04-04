package com.vibethema.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EquipmentDataService {
    private static final String APP_DIR = ".vibethema";
    private static final String TAGS_FILE = "equipment_tags.json";
    private final Gson gson = new Gson();

    public static Path getEquipmentTagsPath() {
        return Paths.get(System.getProperty("user.home"), APP_DIR, TAGS_FILE);
    }

    public static class Tag {
        private String id;
        private String name;
        private String description;

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
    }

    public Map<String, List<Tag>> loadEquipmentTags() {
        Path path = getEquipmentTagsPath();
        if (!Files.exists(path)) return new HashMap<>();

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, List<Tag>>>() {}.getType();
            Map<String, List<Tag>> tags = gson.fromJson(reader, type);
            return tags != null ? tags : new HashMap<>();
        } catch (IOException e) {
            System.err.println("Error loading equipment tags: " + e.getMessage());
            return new HashMap<>();
        }
    }

    public List<Tag> getTagsForCategory(String category) {
        Map<String, List<Tag>> allTags = loadEquipmentTags();
        return allTags.getOrDefault(category.toLowerCase(), new ArrayList<>());
    }
}
