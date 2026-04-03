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

public class CharmDataService {
    private static final String APP_DIR = ".vibethema";
    private static final String CHARMS_DIR = "charms";
    private final Gson gson = new Gson();

    public static Path getUserCharmsPath() {
        return Paths.get(System.getProperty("user.home"), APP_DIR, CHARMS_DIR);
    }

    public List<Charm> loadCharmsForAbility(String ability) {
        String filename = ability.toLowerCase().replace(" ", "-") + ".json";
        Path userPath = getUserCharmsPath().resolve(filename);

        // Try user directory first
        if (Files.exists(userPath)) {
            try (Reader reader = Files.newBufferedReader(userPath, StandardCharsets.UTF_8)) {
                Type listType = new TypeToken<ArrayList<Charm>>() {}.getType();
                return gson.fromJson(reader, listType);
            } catch (IOException e) {
                System.err.println("Error loading user charm file: " + userPath + " - " + e.getMessage());
            }
        }

        // Fallback to resources
        try (InputStream is = getClass().getResourceAsStream("/charms/" + filename)) {
            if (is != null) {
                try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    Type listType = new TypeToken<ArrayList<Charm>>() {}.getType();
                    return gson.fromJson(reader, listType);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading resource charm file: " + filename + " - " + e.getMessage());
        }

        return new ArrayList<>();
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
