package com.vibethema.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.vibethema.model.Charm;
import com.vibethema.model.Keyword;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
        String filename = ability.toLowerCase().replace(" ", "-") + ".json";
        String customFilename = ability.toLowerCase().replace(" ", "-") + "-custom.json";
        
        Path userPath = getUserCharmsPath().resolve(filename);
        Path maPath = getUserMartialArtsPath().resolve(filename);
        Path customPath = getUserCharmsPath().resolve(customFilename);

        // 1. Load standard charms (user-imported, martial arts, or resource)
        boolean loadedFromFile = false;
        if (Files.exists(userPath)) {
            loadedFromFile = loadFromFile(userPath, allCharms);
        } else if (Files.exists(maPath)) {
            loadedFromFile = loadFromFile(maPath, allCharms);
        }

        if (!loadedFromFile) {
            loadFromResource(filename, allCharms);
        }
        
        // Ensure standard charms have IDs
        assignMigrationIds(allCharms, ability);

        // 2. Load custom charms
        if (Files.exists(customPath)) {
            try (Reader reader = Files.newBufferedReader(customPath, StandardCharsets.UTF_8)) {
                Type listType = new TypeToken<ArrayList<Charm>>() {}.getType();
                List<Charm> custom = gson.fromJson(reader, listType);
                if (custom != null) {
                    custom.forEach(c -> {
                        c.setCustom(true);
                        if (c.getId() == null || c.getId().isEmpty()) {
                            c.setId(java.util.UUID.randomUUID().toString());
                        }
                    });
                    allCharms.addAll(custom);
                }
            } catch (IOException e) {
                System.err.println("Error loading custom charm file: " + customPath + " - " + e.getMessage());
            }
        }

        return allCharms;
    }

    public void saveCustomCharm(Charm charm) throws IOException {
        String ability = charm.getAbility();
        List<Charm> existing = loadCharmsForAbility(ability);
        List<Charm> customOnly = existing.stream().filter(Charm::isCustom).collect(Collectors.toList());
        
        if (charm.getId() == null || charm.getId().isEmpty()) {
            charm.setId(java.util.UUID.randomUUID().toString());
        }
        
        // Check if updating existing custom charm
        boolean found = false;
        for (int i = 0; i < customOnly.size(); i++) {
            if (customOnly.get(i).getId().equals(charm.getId())) {
                customOnly.set(i, charm);
                found = true;
                break;
            }
        }
        if (!found) {
            customOnly.add(charm);
        }
        
        saveCustomList(ability, customOnly);
    }

    public void deleteCustomCharm(Charm charm) throws IOException {
        String ability = charm.getAbility();
        if (charm.getId() == null || charm.getId().isEmpty()) return;

        List<Charm> existing = loadCharmsForAbility(ability);
        List<Charm> customOnly = existing.stream()
                .filter(c -> c.isCustom() && !c.getId().equals(charm.getId()))
                .collect(Collectors.toList());
        
        saveCustomList(ability, customOnly);
    }

    private void saveCustomList(String ability, List<Charm> charms) throws IOException {
        String filename = ability.toLowerCase().replace(" ", "-") + "-custom.json";
        Path outDir = getUserCharmsPath();
        if (!Files.exists(outDir)) {
            Files.createDirectories(outDir);
        }
        Path filePath = outDir.resolve(filename);
        
        Gson prettyGson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            prettyGson.toJson(charms, writer);
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

        // Fallback to resources
        try (InputStream is = getClass().getResourceAsStream("/charms/" + filename)) {
            if (is != null) {
                try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    Type listType = new TypeToken<ArrayList<Keyword>>() {}.getType();
                    return gson.fromJson(reader, listType);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading resource keyword file: " + filename + " - " + e.getMessage());
        }

        return new ArrayList<>();
    }

    private boolean loadFromFile(Path path, List<Charm> allCharms) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<ArrayList<Charm>>() {}.getType();
            List<Charm> base = gson.fromJson(reader, listType);
            if (base != null) {
                allCharms.addAll(base);
                return true;
            }
        } catch (IOException e) {
            System.err.println("Error loading charm file: " + path + " - " + e.getMessage());
        }
        return false;
    }

    private void loadFromResource(String filename, List<Charm> allCharms) {
        try (InputStream is = getClass().getResourceAsStream("/charms/" + filename)) {
            if (is != null) {
                try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    Type listType = new TypeToken<ArrayList<Charm>>() {}.getType();
                    List<Charm> base = gson.fromJson(reader, listType);
                    if (base != null) allCharms.addAll(base);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading resource charm file: " + filename + " - " + e.getMessage());
        }
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
