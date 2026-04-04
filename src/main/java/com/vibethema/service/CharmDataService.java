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

public class CharmDataService {
    private static final String APP_DIR = ".vibethema";
    private static final String CHARMS_DIR = "charms";
    private static final String MA_DIR = "martial_arts";
    private static final String EVOCATIONS_DIR = "evocations";
    private final Gson gson = new GsonBuilder().create();

    public static Path getUserCharmsPath() {
        return Paths.get(System.getProperty("user.home"), APP_DIR, CHARMS_DIR);
    }

    public static Path getUserMartialArtsPath() {
        return Paths.get(System.getProperty("user.home"), APP_DIR, CHARMS_DIR, MA_DIR);
    }

    public static Path getUserEvocationsPath() {
        return Paths.get(System.getProperty("user.home"), APP_DIR, CHARMS_DIR, EVOCATIONS_DIR);
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
        private String type;
        private String exalt = "solar";
        private List<Charm> charms = new ArrayList<>();

        public CharmListWrapper() {}
        public CharmListWrapper(String ability, String type, List<Charm> charms) {
            this.ability = ability;
            this.type = type;
            this.charms = charms;
        }
    }

    public static class EvocationCollection {
        public String artifactId;
        public String artifactName;
        public List<Charm> evocations = new ArrayList<>();

        public EvocationCollection() {}
        public EvocationCollection(String artifactId, String artifactName, List<Charm> charms) {
            this.artifactId = artifactId;
            this.artifactName = artifactName;
            this.evocations = charms;
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

        // Determine the type for the wrapper
        String typeAttr = "solarAbility";
        if (targetPath.startsWith(getUserMartialArtsPath()) || ability.equalsIgnoreCase("Martial Arts")) {
            typeAttr = "martialArts";
        }

        // Save back pretty-printed with wrapper
        Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = Files.newBufferedWriter(targetPath, StandardCharsets.UTF_8)) {
            prettyGson.toJson(new CharmListWrapper(ability, typeAttr, charms), writer);
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
            
            // Determine the type for the wrapper
            String typeAttr = "solarAbility";
            if (filePath.startsWith(getUserMartialArtsPath()) || ability.equalsIgnoreCase("Martial Arts")) {
                typeAttr = "martialArts";
            }

            Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
            try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                prettyGson.toJson(new CharmListWrapper(ability, typeAttr, existing), writer);
            }
        }
    }

    public EvocationCollection loadEvocations(String artifactId) {
        Path filePath = getUserEvocationsPath().resolve(artifactId + ".json");
        if (Files.exists(filePath)) {
            try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                // Try reading as new EvocationCollection format
                EvocationCollection collection = gson.fromJson(reader, EvocationCollection.class);
                // If it has artifactName, it's the new format
                if (collection != null && collection.evocations != null && collection.artifactName != null) {
                    return collection;
                }
            } catch (IOException e) {
                System.err.println("Error loading evocations (new format): " + e.getMessage());
            }

            // Fallback: Try reading as old CharmListWrapper format
            try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                CharmListWrapper wrapper = gson.fromJson(reader, CharmListWrapper.class);
                if (wrapper != null && wrapper.charms != null) {
                    return new EvocationCollection(artifactId, wrapper.ability, wrapper.charms);
                }
            } catch (IOException e) {
                System.err.println("Error loading evocations (old format): " + e.getMessage());
            }
        }
        return new EvocationCollection(artifactId, "Artifact (" + (artifactId.length() > 8 ? artifactId.substring(0, 8) : artifactId) + ")", new ArrayList<>());
    }

    public void saveEvocation(String artifactId, String artifactName, Charm charm) throws IOException {
        Path filePath = getUserEvocationsPath().resolve(artifactId + ".json");
        if (!Files.exists(filePath.getParent())) {
            Files.createDirectories(filePath.getParent());
        }

        EvocationCollection collection = loadEvocations(artifactId);
        collection.artifactName = artifactName; // Ensure name is current
        
        List<Charm> charms = collection.evocations;
        boolean found = false;
        for (int i = 0; i < charms.size(); i++) {
            if (charms.get(i).getId().equals(charm.getId())) {
                charms.set(i, charm);
                found = true;
                break;
            }
        }
        if (!found) charms.add(charm);

        Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            prettyGson.toJson(collection, writer);
        }
    }

    public void updateEvocationCollectionName(String artifactId, String artifactName) throws IOException {
        Path filePath = getUserEvocationsPath().resolve(artifactId + ".json");
        if (Files.exists(filePath)) {
            EvocationCollection collection = loadEvocations(artifactId);
            collection.artifactName = artifactName;
            Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
            try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                prettyGson.toJson(collection, writer);
            }
        }
    }

    public void deleteEvocation(String artifactId, Charm charm) throws IOException {
        Path filePath = getUserEvocationsPath().resolve(artifactId + ".json");
        if (!Files.exists(filePath)) return;

        EvocationCollection collection = loadEvocations(artifactId);
        collection.evocations.removeIf(c -> c.getId().equals(charm.getId()));

        Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            prettyGson.toJson(collection, writer);
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
        
        // Scan User Directories
        scanDirectoryForStyles(getUserCharmsPath(), styles);
        scanDirectoryForStyles(getUserMartialArtsPath(), styles);
        
        return new ArrayList<>(styles);
    }

    private void scanDirectoryForStyles(Path dir, java.util.Set<String> styles) {
        if (!Files.exists(dir)) return;
        try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.toString().endsWith(".json") && 
                             !p.toString().endsWith("-custom.json") && 
                             !p.toString().endsWith("keywords.json") &&
                             !p.toString().endsWith("charm-schema.json"))
                  .forEach(p -> {
                      try (Reader reader = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                          CharmListWrapper wrapper = gson.fromJson(reader, CharmListWrapper.class);
                          if (wrapper != null && "martialArts".equals(wrapper.type)) {
                              styles.add(wrapper.ability);
                          }
                      } catch (Exception e) {
                          // Skip invalid/unreadable JSON or missing type fields
                          System.err.println("Skipping non-martialArts style file: " + p + " - " + e.getMessage());
                      }
                  });
        } catch (IOException e) {
            e.printStackTrace();
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

}
