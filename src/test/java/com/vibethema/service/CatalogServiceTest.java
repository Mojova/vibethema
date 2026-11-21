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
        writeSource("solar-book", "SOLAR", "Solar Charm", "solar-charm");
        writeSource("lunar-book", "LUNAR", "Lunar Charm", "lunar-charm");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("vibethema.data.dir");
    }

    @Test
    void exposesOneFilteredCatalogAndKeepsCustomDefinitions() throws Exception {
        Path custom = PathService.getCustomDataPath().resolve("charms/solar");
        Files.createDirectories(custom);
        Files.writeString(
                custom.resolve("athletics.json"),
                CharmFixtures.solar(
                        "custom", "Athletics", "[{\"id\":\"custom\",\"name\":\"Custom Charm\"}]"),
                StandardCharsets.UTF_8);

        CatalogService catalog = new CatalogService();

        assertEquals(1, catalog.getSourcesFor(ExaltType.SOLAR).size());
        assertEquals(2, catalog.getCharmsForAbility("Athletics", ExaltType.SOLAR).size());
        org.junit.jupiter.api.Assertions.assertThrows(
                com.google.gson.JsonParseException.class,
                () -> catalog.getCharmsForAbility("Athletics", ExaltType.LUNAR));
    }

    @Test
    void customDefinitionWithSameIdReplacesOfficialDefinition() throws Exception {
        Path custom = PathService.getCustomDataPath().resolve("charms/solar");
        Files.createDirectories(custom);
        Files.writeString(
                custom.resolve("athletics.json"),
                CharmFixtures.solar(
                        "custom",
                        "Athletics",
                        "[{\"id\":\"solar-charm\",\"name\":\"Custom Replacement\"}]"),
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
        Files.createDirectories(source.resolve("charms/" + type.toLowerCase()));
        Files.writeString(
                source.resolve("manifest.json"),
                "{\"sourceId\":\"" + id + "\",\"exaltTypes\":[\"" + type + "\"]}",
                StandardCharsets.UTF_8);
        Files.writeString(
                source.resolve("charms/" + type.toLowerCase() + "/athletics.json"),
                CharmFixtures.nativeCharms(
                        id,
                        type,
                        "Athletics",
                        "[{\"id\":\"" + charmId + "\",\"name\":\"" + charmName + "\"}]"),
                StandardCharsets.UTF_8);
    }
}
