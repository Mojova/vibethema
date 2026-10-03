package com.vibethema.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Centralized service for determining platform-specific application data and configuration paths.
 *
 * <p>Standard Locations: - macOS: ~/Library/Application Support/Vibethema/ (Data) &
 * ~/Library/Preferences/Vibethema/ (Config) - Windows: %AppData%/Vibethema/Data/ &
 * %AppData%/Vibethema/Config/ - Linux: ~/.local/share/vibethema/ (Data) & ~/.config/vibethema/
 * (Config)
 */
public class PathService {

    private static final String DEVELOPER_NAME = "Mojova";
    private static final String APP_NAME = "Vibethema";

    /**
     * returns the standard base path for application data (Core Book imports, characters, etc.).
     */
    public static Path getDataPath() {
        String override = System.getProperty("vibethema.data.dir");
        if (override != null && !override.isEmpty()) {
            return Paths.get(override);
        }

        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");

        if (os.contains("mac")) {
            return Paths.get(home, "Library", "Application Support", DEVELOPER_NAME, APP_NAME);
        } else if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                return Paths.get(appData, DEVELOPER_NAME, APP_NAME, "Data");
            }
            return Paths.get(home, "AppData", "Roaming", DEVELOPER_NAME, APP_NAME, "Data");
        } else {
            // Linux/Unix (XDG)
            String xdgData = System.getenv("XDG_DATA_HOME");
            if (xdgData != null && !xdgData.isEmpty()) {
                return Paths.get(xdgData, DEVELOPER_NAME.toLowerCase(), APP_NAME.toLowerCase());
            }
            return Paths.get(
                    home, ".local", "share", DEVELOPER_NAME.toLowerCase(), APP_NAME.toLowerCase());
        }
    }

    public static Path getOfficialSourcesPath() {
        return getDataPath().resolve("official");
    }

    public static Path getOfficialSourcePath(String sourceId) {
        return getOfficialSourcesPath().resolve(sourceId);
    }

    public static Path getCustomDataPath() {
        return getDataPath().resolve("custom");
    }

    /** Returns the installed official source directories in deterministic order. */
    public static List<Path> getOfficialSourcePaths() {
        Path sourcesPath = getOfficialSourcesPath();
        if (!java.nio.file.Files.isDirectory(sourcesPath)) {
            return List.of();
        }
        try (java.util.stream.Stream<Path> paths = java.nio.file.Files.list(sourcesPath)) {
            return paths.filter(java.nio.file.Files::isDirectory)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        } catch (IOException e) {
            return List.of();
        }
    }

    /** Returns the standard base path for application configuration (user settings). */
    public static Path getConfigPath() {
        String override = System.getProperty("vibethema.config.dir");
        if (override != null && !override.isEmpty()) {
            return Paths.get(override);
        }

        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");

        if (os.contains("mac")) {
            return Paths.get(home, "Library", "Preferences", DEVELOPER_NAME, APP_NAME);
        } else if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                return Paths.get(appData, DEVELOPER_NAME, APP_NAME, "Config");
            }
            return Paths.get(home, "AppData", "Roaming", DEVELOPER_NAME, APP_NAME, "Config");
        } else {
            // Linux/Unix (XDG)
            String xdgConfig = System.getenv("XDG_CONFIG_HOME");
            if (xdgConfig != null && !xdgConfig.isEmpty()) {
                return Paths.get(xdgConfig, DEVELOPER_NAME.toLowerCase(), APP_NAME.toLowerCase());
            }
            return Paths.get(home, ".config", DEVELOPER_NAME.toLowerCase(), APP_NAME.toLowerCase());
        }
    }
}
