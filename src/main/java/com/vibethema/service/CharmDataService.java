package com.vibethema.service;

import com.google.gson.Gson;
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
import java.util.List;
import java.util.stream.Collectors;

public class CharmDataService {
    private static final String APP_DIR = ".vibethema";
    private static final String CHARMS_DIR = "charms";
    private final Gson gson = new Gson();

    public static Path getUserCharmsPath() {
        return Paths.get(System.getProperty("user.home"), APP_DIR, CHARMS_DIR);
    }

    public List<Charm> loadCharmsForAbility(String ability) {
        List<Charm> allCharms = new ArrayList<>();
        String filename = ability.toLowerCase().replace(" ", "-") + ".json";
        String customFilename = ability.toLowerCase().replace(" ", "-") + "-custom.json";
        
        Path userPath = getUserCharmsPath().resolve(filename);
        Path customPath = getUserCharmsPath().resolve(customFilename);

        // 1. Load standard charms (user-imported or resource)
        boolean loadedFromUser = false;
        if (Files.exists(userPath)) {
            try (Reader reader = Files.newBufferedReader(userPath, StandardCharsets.UTF_8)) {
                Type listType = new TypeToken<ArrayList<Charm>>() {}.getType();
                List<Charm> base = gson.fromJson(reader, listType);
                if (base != null) {
                    allCharms.addAll(base);
                    loadedFromUser = true;
                }
            } catch (IOException e) {
                System.err.println("Error loading user charm file: " + userPath + " - " + e.getMessage());
            }
        }

        if (!loadedFromUser) {
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

        // 2. Load custom charms
        if (Files.exists(customPath)) {
            try (Reader reader = Files.newBufferedReader(customPath, StandardCharsets.UTF_8)) {
                Type listType = new TypeToken<ArrayList<Charm>>() {}.getType();
                List<Charm> custom = gson.fromJson(reader, listType);
                if (custom != null) {
                    custom.forEach(c -> c.setCustom(true));
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
        
        // Check if updating existing custom charm
        boolean found = false;
        for (int i = 0; i < customOnly.size(); i++) {
            if (customOnly.get(i).getName().equals(charm.getName())) {
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
        List<Charm> existing = loadCharmsForAbility(ability);
        List<Charm> customOnly = existing.stream()
                .filter(c -> c.isCustom() && !c.getName().equals(charm.getName()))
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
}
