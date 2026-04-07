package com.vibethema.service;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.social.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.logic.*;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EquipmentDataService {
    private static final Logger logger = LoggerFactory.getLogger(EquipmentDataService.class);
    private static final String TAGS_FILE = "equipment_tags.json";
    private static final String EQUIPMENT_DIR = "equipment";
    private final Gson gson = new Gson();

    public static Path getEquipmentTagsPath() {
        return PathService.getDataPath().resolve(TAGS_FILE);
    }

    private static Path getGlobalEquipmentPath() {
        return PathService.getDataPath().resolve(EQUIPMENT_DIR);
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
    public void saveWeapon(com.vibethema.model.equipment.Weapon w) throws IOException { saveGlobalItem(w.toData(), w.getId(), getWeaponsPath()); }
    public com.vibethema.model.equipment.Weapon loadWeapon(String id) { 
        return com.vibethema.model.equipment.Weapon.fromData(loadGlobalItem(id, getWeaponsPath(), com.vibethema.model.equipment.Weapon.WeaponData.class)); 
    }
    public void deleteWeapon(String id) throws IOException { 
        if (com.vibethema.model.equipment.Weapon.UNARMED_ID.equals(id)) return;
        deleteGlobalItem(id, getWeaponsPath()); 
    }
    public List<com.vibethema.model.equipment.Weapon> getGlobalWeapons() { 
        return listGlobalItems(getWeaponsPath(), com.vibethema.model.equipment.Weapon.WeaponData.class).stream()
                .map(com.vibethema.model.equipment.Weapon::fromData)
                .collect(java.util.stream.Collectors.toList()); 
    }

    public void saveArmor(com.vibethema.model.equipment.Armor a) throws IOException { saveGlobalItem(a.toData(), a.getId(), getArmorsPath()); }
    public com.vibethema.model.equipment.Armor loadArmor(String id) { 
        return com.vibethema.model.equipment.Armor.fromData(loadGlobalItem(id, getArmorsPath(), com.vibethema.model.equipment.Armor.ArmorData.class)); 
    }
    public void deleteArmor(String id) throws IOException { deleteGlobalItem(id, getArmorsPath()); }
    public List<com.vibethema.model.equipment.Armor> getGlobalArmors() { 
        return listGlobalItems(getArmorsPath(), com.vibethema.model.equipment.Armor.ArmorData.class).stream()
                .map(com.vibethema.model.equipment.Armor::fromData)
                .collect(java.util.stream.Collectors.toList()); 
    }

    public void saveHearthstone(com.vibethema.model.equipment.Hearthstone h) throws IOException { saveGlobalItem(h.toData(), h.getId(), getHearthstonesPath()); }
    public com.vibethema.model.equipment.Hearthstone loadHearthstone(String id) { 
        return com.vibethema.model.equipment.Hearthstone.fromData(loadGlobalItem(id, getHearthstonesPath(), com.vibethema.model.equipment.Hearthstone.HearthstoneData.class)); 
    }
    public void deleteHearthstone(String id) throws IOException { deleteGlobalItem(id, getHearthstonesPath()); }
    public List<com.vibethema.model.equipment.Hearthstone> getGlobalHearthstones() { 
        return listGlobalItems(getHearthstonesPath(), com.vibethema.model.equipment.Hearthstone.HearthstoneData.class).stream()
                .map(com.vibethema.model.equipment.Hearthstone::fromData)
                .collect(java.util.stream.Collectors.toList()); 
    }

    public void saveOtherEquipment(com.vibethema.model.equipment.OtherEquipment oe) throws IOException { saveGlobalItem(oe.toData(), oe.getId(), getOtherEquipmentPath()); }
    public com.vibethema.model.equipment.OtherEquipment loadOtherEquipment(String id) { 
        return com.vibethema.model.equipment.OtherEquipment.fromData(loadGlobalItem(id, getOtherEquipmentPath(), com.vibethema.model.equipment.OtherEquipment.OtherEquipmentData.class)); 
    }
    public void deleteOtherEquipment(String id) throws IOException { deleteGlobalItem(id, getOtherEquipmentPath()); }
    public List<com.vibethema.model.equipment.OtherEquipment> getGlobalOtherEquipment() { 
        return listGlobalItems(getOtherEquipmentPath(), com.vibethema.model.equipment.OtherEquipment.OtherEquipmentData.class).stream()
                .map(com.vibethema.model.equipment.OtherEquipment::fromData)
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