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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EquipmentDataService {
    private static final Logger logger = LoggerFactory.getLogger(EquipmentDataService.class);
    private static final String APP_DIR = ".vibethema";
    private static final String TAGS_FILE = "equipment_tags.json";
    private static final String EQUIPMENT_DIR = "equipment";
    private final Gson gson = new Gson();

    public static Path getEquipmentTagsPath() {
        return Paths.get(System.getProperty("user.home"), APP_DIR, TAGS_FILE);
    }

    private static Path getGlobalEquipmentPath() {
        return Paths.get(System.getProperty("user.home"), APP_DIR, EQUIPMENT_DIR);
    }

    public static Path getWeaponsPath() { return getGlobalEquipmentPath().resolve("weapons"); }
    public static Path getArmorsPath() { return getGlobalEquipmentPath().resolve("armors"); }
    public static Path getHearthstonesPath() { return getGlobalEquipmentPath().resolve("hearthstones"); }
    public static Path getOtherEquipmentPath() { return getGlobalEquipmentPath().resolve("other"); }

    public EquipmentDataService() {
        try {
            Files.createDirectories(getWeaponsPath());
            Files.createDirectories(getArmorsPath());
            Files.createDirectories(getHearthstonesPath());
            Files.createDirectories(getOtherEquipmentPath());
        } catch (IOException e) {
            logger.error("Failed to create global equipment directories", e);
        }
    }

    // --- Generic CRUD ---
    private <T> void saveGlobalItem(T item, String id, Path dir) throws IOException {
        Path filePath = dir.resolve(id + ".json");
        try (java.io.Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            gson.toJson(item, writer);
        }
    }

    private void deleteGlobalItem(String id, Path dir) throws IOException {
        Path filePath = dir.resolve(id + ".json");
        Files.deleteIfExists(filePath);
    }

    private <T> T loadGlobalItem(String id, Path dir, Class<T> type) {
        Path filePath = dir.resolve(id + ".json");
        if (!Files.exists(filePath)) return null;
        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, type);
        } catch (IOException e) {
            logger.error("Error loading global item from: {}", filePath, e);
            return null;
        }
    }

    private <T> List<T> listGlobalItems(Path dir, Class<T> type) {
        List<T> items = new ArrayList<>();
        if (!Files.exists(dir)) return items;
        try (java.util.stream.Stream<Path> files = Files.list(dir)) {
            files.filter(f -> f.toString().endsWith(".json")).forEach(f -> {
                try (Reader reader = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                    T item = gson.fromJson(reader, type);
                    if (item != null) items.add(item);
                } catch (IOException e) {
                    logger.error("Error reading equipment file: {}", f, e);
                }
            });
        } catch (IOException e) {
            logger.error("Error listing equipment directory: {}", dir, e);
        }
        return items;
    }

    // --- Specific API ---
    public void saveWeapon(com.vibethema.model.Weapon w) throws IOException { saveGlobalItem(w.toData(), w.getId(), getWeaponsPath()); }
    public com.vibethema.model.Weapon loadWeapon(String id) { 
        return com.vibethema.model.Weapon.fromData(loadGlobalItem(id, getWeaponsPath(), com.vibethema.model.Weapon.WeaponData.class)); 
    }
    public void deleteWeapon(String id) throws IOException { 
        if (com.vibethema.model.Weapon.UNARMED_ID.equals(id)) return;
        deleteGlobalItem(id, getWeaponsPath()); 
    }
    public List<com.vibethema.model.Weapon> getGlobalWeapons() { 
        return listGlobalItems(getWeaponsPath(), com.vibethema.model.Weapon.WeaponData.class).stream()
                .map(com.vibethema.model.Weapon::fromData)
                .collect(java.util.stream.Collectors.toList()); 
    }

    public void saveArmor(com.vibethema.model.Armor a) throws IOException { saveGlobalItem(a.toData(), a.getId(), getArmorsPath()); }
    public com.vibethema.model.Armor loadArmor(String id) { 
        return com.vibethema.model.Armor.fromData(loadGlobalItem(id, getArmorsPath(), com.vibethema.model.Armor.ArmorData.class)); 
    }
    public void deleteArmor(String id) throws IOException { deleteGlobalItem(id, getArmorsPath()); }
    public List<com.vibethema.model.Armor> getGlobalArmors() { 
        return listGlobalItems(getArmorsPath(), com.vibethema.model.Armor.ArmorData.class).stream()
                .map(com.vibethema.model.Armor::fromData)
                .collect(java.util.stream.Collectors.toList()); 
    }

    public void saveHearthstone(com.vibethema.model.Hearthstone h) throws IOException { saveGlobalItem(h.toData(), h.getId(), getHearthstonesPath()); }
    public com.vibethema.model.Hearthstone loadHearthstone(String id) { 
        return com.vibethema.model.Hearthstone.fromData(loadGlobalItem(id, getHearthstonesPath(), com.vibethema.model.Hearthstone.HearthstoneData.class)); 
    }
    public void deleteHearthstone(String id) throws IOException { deleteGlobalItem(id, getHearthstonesPath()); }
    public List<com.vibethema.model.Hearthstone> getGlobalHearthstones() { 
        return listGlobalItems(getHearthstonesPath(), com.vibethema.model.Hearthstone.HearthstoneData.class).stream()
                .map(com.vibethema.model.Hearthstone::fromData)
                .collect(java.util.stream.Collectors.toList()); 
    }

    public void saveOtherEquipment(com.vibethema.model.OtherEquipment oe) throws IOException { saveGlobalItem(oe.toData(), oe.getId(), getOtherEquipmentPath()); }
    public com.vibethema.model.OtherEquipment loadOtherEquipment(String id) { 
        return com.vibethema.model.OtherEquipment.fromData(loadGlobalItem(id, getOtherEquipmentPath(), com.vibethema.model.OtherEquipment.OtherEquipmentData.class)); 
    }
    public void deleteOtherEquipment(String id) throws IOException { deleteGlobalItem(id, getOtherEquipmentPath()); }
    public List<com.vibethema.model.OtherEquipment> getGlobalOtherEquipment() { 
        return listGlobalItems(getOtherEquipmentPath(), com.vibethema.model.OtherEquipment.OtherEquipmentData.class).stream()
                .map(com.vibethema.model.OtherEquipment::fromData)
                .collect(java.util.stream.Collectors.toList()); 
    }

    public static class Tag {
        private String id;
        private String name;
        private String description;

        public Tag() {}

        public Tag(String name, String description) {
            this.id = java.util.UUID.randomUUID().toString();
            this.name = name;
            this.description = description;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    public Map<String, List<Tag>> loadEquipmentTags() {
        Path path = getEquipmentTagsPath();
        if (!Files.exists(path))
            return new HashMap<>();

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, List<Tag>>>() {
            }.getType();
            Map<String, List<Tag>> tags = gson.fromJson(reader, type);
            return tags != null ? tags : new HashMap<>();
        } catch (IOException e) {
            logger.error("Error loading equipment tags from: {}", path, e);
            return new HashMap<>();
        }
    }

    public List<Tag> getTagsForCategory(String category) {
        Map<String, List<Tag>> allTags = loadEquipmentTags();
        return allTags.getOrDefault(category.toLowerCase(), new ArrayList<>());
    }

    public List<Tag> getAllTags() {
        Map<String, List<Tag>> allTags = loadEquipmentTags();
        List<Tag> flat = new ArrayList<>();
        allTags.values().forEach(flat::addAll);
        return flat;
    }
}
