package com.vibethema.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vibethema.model.ExaltType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CatalogServiceTest {
    @TempDir Path dataDirectory;

    @BeforeEach
    void setUp() throws Exception {
        System.setProperty("vibethema.data.dir", dataDirectory.toString());
        writeSource(
                "solar-book",
                "SOLAR",
                "Solar Charm",
                "solar-charm");
        writeSource(
                "lunar-book",
                "LUNAR",
                "Lunar Charm",
                "lunar-charm");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("vibethema.data.dir");
    }

    @Test
    void exposesOneFilteredCatalogAndKeepsCustomDefinitions() throws Exception {
        Path custom = PathService.getCustomDataPath().resolve("charms");
        Files.createDirectories(custom);
        Files.writeString(
                custom.resolve("athletics.json"),
                "{\"ability\":\"Athletics\",\"charms\":[{\"id\":\"custom\",\"name\":\"Custom Charm\",\"category\":\"solar\"}]}",
                StandardCharsets.UTF_8);

        CatalogService catalog = new CatalogService();

        assertEquals(1, catalog.getSourcesFor(ExaltType.SOLAR).size());
        assertEquals(2, catalog.getCharmsForAbility("Athletics", ExaltType.SOLAR).size());
        assertEquals(2, catalog.getCharmsForAbility("Athletics", ExaltType.LUNAR).size());
    }

    @Test
    void customDefinitionWithSameIdReplacesOfficialDefinition() throws Exception {
        Path custom = PathService.getCustomDataPath().resolve("charms");
        Files.createDirectories(custom);
        Files.writeString(
                custom.resolve("athletics.json"),
                "{\"ability\":\"Athletics\",\"charms\":[{\"id\":\"solar-charm\",\"name\":\"Custom Replacement\",\"category\":\"solar\"}]}",
                StandardCharsets.UTF_8);

        CatalogService catalog = new CatalogService();

        assertEquals(1, catalog.getCharmsForAbility("Athletics", ExaltType.SOLAR).size());
        assertEquals(
                "Custom Replacement",
                catalog.getCharmsForAbility("Athletics", ExaltType.SOLAR).get(0).getName());
    }

    private void writeSource(String id, String type, String charmName, String charmId)
            throws Exception {
        Path source = PathService.getOfficialSourcePath(id);
        Files.createDirectories(source.resolve("charms"));
        Files.writeString(
                source.resolve("manifest.json"),
                "{\"sourceId\":\"" + id + "\",\"exaltTypes\":[\"" + type + "\"]}",
                StandardCharsets.UTF_8);
        Files.writeString(
                source.resolve("charms/athletics.json"),
                "{\"ability\":\"Athletics\",\"charms\":[{\"id\":\""
                        + charmId
                        + "\",\"name\":\""
                        + charmName
                        + "\",\"category\":\"solar\"}]}",
                StandardCharsets.UTF_8);
    }
}
