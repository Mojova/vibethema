package com.vibethema.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class PathServiceTest {

    private String originalOs;
    private String originalHome;

    @BeforeEach
    void setUp() {
        originalOs = System.getProperty("os.name");
        originalHome = System.getProperty("user.home");
    }

    @AfterEach
    void tearDown() {
        System.setProperty("os.name", originalOs);
        System.setProperty("user.home", originalHome);
    }

    @Test
    void testMacPaths() {
        System.setProperty("os.name", "Mac OS X");
        System.setProperty("user.home", "/Users/tester");

        Path dataPath = PathService.getDataPath();
        Path configPath = PathService.getConfigPath();

        assertTrue(dataPath.toString().contains("/Users/tester/Library/Application Support/Mojova/Vibethema"));
        assertTrue(configPath.toString().contains("/Users/tester/Library/Preferences/Mojova/Vibethema"));
    }

    @Test
    void testWindowsPaths() {
        System.setProperty("os.name", "Windows 10");
        System.setProperty("user.home", "C:\\Users\\tester");

        // Environment variables are harder to mock in pure Java without extra libs,
        // so we test the fallback or assumption.
        Path dataPath = PathService.getDataPath();
        Path configPath = PathService.getConfigPath();

        assertTrue(dataPath.toString().contains("Mojova") && dataPath.toString().contains("Vibethema") && dataPath.toString().contains("Data"));
        assertTrue(configPath.toString().contains("Mojova") && configPath.toString().contains("Vibethema") && configPath.toString().contains("Config"));
    }

    @Test
    void testLinuxPaths() {
        System.setProperty("os.name", "Linux");
        System.setProperty("user.home", "/home/tester");

        Path dataPath = PathService.getDataPath();
        Path configPath = PathService.getConfigPath();

        assertTrue(dataPath.toString().contains("/home/tester/.local/share/mojova/vibethema"));
        assertTrue(configPath.toString().contains("/home/tester/.config/mojova/vibethema"));
    }
}
