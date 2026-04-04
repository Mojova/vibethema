package com.vibethema.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.vibethema.model.Charm;
import com.vibethema.model.Keyword;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CharmDataService {
    private static final String APP_DIR = ".vibethema";
    private static final String CHARMS_DIR = "charms";
    private static final String MA_DIR = "martial_arts";
    private final Gson gson = new GsonBuilder().create();

    public static Path getUserCharmsPath() {
        return Paths.get(System.getProperty("user.home"), APP_DIR, CHARMS_DIR);
    }

    public static Path getUserMartialArtsPath() {
        return Paths.get(System.getProperty("user.home"), APP_DIR, MA_DIR);
    }

    public List<Charm> loadCharmsForAbility(String ability) {
        List<Charm> allCharms = new ArrayList<>();
        String baseName = ability.toLowerCase().replace(" ", "-");
        
        // 1. Scan for all matching files in User Charms and Martial Arts directories
        scanAndLoadMatchingFiles(getUserCharmsPath(), baseName, allCharms);
        scanAndLoadMatchingFiles(getUserMartialArtsPath(), baseName, allCharms);
        
        // Ensure standard charms have IDs (migration helper)
        assignMigrationIds(allCharms, ability);

        return allCharms;
    }

    private void scanAndLoadMatchingFiles(Path dir, String baseName, List<Charm> allCharms) {
        if (!Files.exists(dir)) return;
        try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> {
                String fileName = p.getFileName().toString();
                // Match baseName.json or baseName-something.json
                return fileName.equals(baseName + ".json") || 
                       (fileName.startsWith(baseName + "-") && fileName.endsWith(".json"));
            }).forEach(p -> loadFromFile(p, allCharms));
        } catch (IOException e) {
            System.err.println("Error scanning directory: " + dir + " - " + e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    private static class CharmListWrapper {
        private String $schema = "./charm-schema.json";
        private String version = "0.1.0";
        private String ability;
        private String exalt = "solar";
        private List<Charm> charms = new ArrayList<>();

        public CharmListWrapper() {}
        public CharmListWrapper(String ability, List<Charm> charms) {
            this.ability = ability;
            this.charms = charms;
        }
    }

    public void exportSchema() throws IOException {
        Path charmsDir = getUserCharmsPath();
        if (!Files.exists(charmsDir)) Files.createDirectories(charmsDir);
        
        try (InputStream is = getClass().getResourceAsStream("/charms/charm-schema.json")) {
            if (is != null) {
                Files.copy(is, charmsDir.resolve("charm-schema.json"), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        
        Path maDir = getUserMartialArtsPath();
        if (!Files.exists(maDir)) Files.createDirectories(maDir);
        try (InputStream is = getClass().getResourceAsStream("/charms/charm-schema.json")) {
            if (is != null) {
                Files.copy(is, maDir.resolve("charm-schema.json"), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    public void saveCharm(Charm charm) throws IOException {
        String ability = charm.getAbility();
        String filename = ability.toLowerCase().replace(" ", "-") + ".json";
        String customFilename = ability.toLowerCase().replace(" ", "-") + "-custom.json";
        
        Path targetPath;
        if (charm.isCustom()) {
            targetPath = getUserCharmsPath().resolve(customFilename);
        } else {
            Path standardAbPath = getUserCharmsPath().resolve(filename);
            Path maPath = getUserMartialArtsPath().resolve(filename);
            if (Files.exists(maPath)) targetPath = maPath;
            else targetPath = standardAbPath;
        }

        // Ensure directory exists
        if (!Files.exists(targetPath.getParent())) {
            Files.createDirectories(targetPath.getParent());
        }

        // Load existing
        List<Charm> charms = new ArrayList<>();
        if (Files.exists(targetPath)) {
            try (Reader reader = Files.newBufferedReader(targetPath, StandardCharsets.UTF_8)) {
                CharmListWrapper wrapper = gson.fromJson(reader, CharmListWrapper.class);
                if (wrapper != null && wrapper.charms != null) {
                    charms = wrapper.charms;
                }
            }
        }

        // Replace or add
        boolean found = false;
        for (int i = 0; i < charms.size(); i++) {
            if (charms.get(i).getId().equals(charm.getId())) {
                charms.set(i, charm);
                found = true;
                break;
            }
        }
        if (!found) charms.add(charm);

        // Save back pretty-printed with wrapper
        Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = Files.newBufferedWriter(targetPath, StandardCharsets.UTF_8)) {
            prettyGson.toJson(new CharmListWrapper(ability, charms), writer);
        }
        exportSchema(); // Ensure schema is present for the user
    }

    public void deleteCustomCharm(Charm charm) throws IOException {
        String ability = charm.getAbility();
        if (charm.getId() == null || charm.getId().isEmpty()) return;

        String filename = ability.toLowerCase().replace(" ", "-") + "-custom.json";
        Path filePath = getUserCharmsPath().resolve(filename);
        
        if (!Files.exists(filePath)) return;

        List<Charm> existing = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            CharmListWrapper wrapper = gson.fromJson(reader, CharmListWrapper.class);
            if (wrapper != null && wrapper.charms != null) {
                existing = wrapper.charms;
            }
        }

        if (existing != null) {
            existing.removeIf(c -> c.getId().equals(charm.getId()));
            Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
            try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                prettyGson.toJson(new CharmListWrapper(ability, existing), writer);
            }
        }
    }

    public List<Keyword> loadKeywords() {
        String filename = "keywords.json";
        Path userPath = getUserCharmsPath().resolve(filename);

        // Try user directory first
        if (Files.exists(userPath)) {
            try (Reader reader = Files.newBufferedReader(userPath, StandardCharsets.UTF_8)) {
                Type listType = new TypeToken<ArrayList<Keyword>>() {}.getType();
                return gson.fromJson(reader, listType);
            } catch (IOException e) {
                System.err.println("Error loading user keyword file: " + userPath + " - " + e.getMessage());
            }
        }

        return new ArrayList<>();
    }

    private boolean loadFromFile(Path path, List<Charm> allCharms) {
        if (!Files.exists(path)) return false;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            CharmListWrapper wrapper = gson.fromJson(reader, CharmListWrapper.class);
            if (wrapper != null && wrapper.charms != null) {
                allCharms.addAll(wrapper.charms);
                return true;
            }
        } catch (IOException e) {
            System.err.println("Error loading charm file: " + path + " - " + e.getMessage());
        }
        return false;
    }

    public List<String> getAvailableMartialArtsStyles() {
        java.util.Set<String> styles = new java.util.TreeSet<>();
        
        // Scan Resources (limited by getResourceAsStream - hard to "scan" jars easily without libraries)
        // We already added the core ones above.
        
        // Scan User Directories
        scanDirectoryForStyles(getUserCharmsPath(), styles);
        scanDirectoryForStyles(getUserMartialArtsPath(), styles);
        
        // Standard abilities to filter out
        java.util.Set<String> standard = java.util.Set.of(
            "archery", "brawl", "martial-arts", "melee", "thrown", "war",
            "dodge", "integrity", "performance", "presence", "resistance", "survival",
            "craft", "investigation", "lore", "occult", "medicine",
            "athletics", "awareness", "larceny", "stealth", "socialize",
            "bureaucracy", "linguistics", "ride", "sail"
        );
        
        return styles.stream()
                .filter(s -> !standard.contains(s.toLowerCase().replace(" ", "-")))
                .collect(Collectors.toList());
    }

    private void scanDirectoryForStyles(Path dir, java.util.Set<String> styles) {
        if (Files.exists(dir)) {
            try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
                stream.filter(p -> p.toString().endsWith(".json") && !p.toString().endsWith("-custom.json") && !p.toString().endsWith("keywords.json"))
                      .map(p -> p.getFileName().toString().replace(".json", ""))
                      .map(this::toTitleCase)
                      .forEach(styles::add);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void createNewMartialArtsStyle(String name) throws IOException {
        String filename = name.toLowerCase().replace(" ", "-") + ".json";
        Path outDir = getUserMartialArtsPath();
        if (!Files.exists(outDir)) {
            Files.createDirectories(outDir);
        }
        Path filePath = outDir.resolve(filename);
        if (!Files.exists(filePath)) {
            try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                writer.write("[]");
            }
        }
    }

    private void assignMigrationIds(List<Charm> charms, String ability) {
        for (Charm c : charms) {
            if (c.getId() == null || c.getId().isEmpty()) {
                String id = java.util.UUID.nameUUIDFromBytes((c.getName().trim() + "|" + ability.trim()).getBytes()).toString();
                c.setId(id);
            }
        }
    }

    private String toTitleCase(String input) {
        StringBuilder sb = new StringBuilder();
        boolean nextTitleCase = true;
        for (char c : input.replace("-", " ").toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            } else {
                c = Character.toLowerCase(c);
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
