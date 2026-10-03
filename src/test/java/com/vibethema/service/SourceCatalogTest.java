package com.vibethema.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vibethema.model.ExaltType;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceCatalogTest {
    @TempDir Path dataDirectory;

    @BeforeEach
    void setDataDirectory() throws Exception {
        System.setProperty("vibethema.data.dir", dataDirectory.toString());
        writeManifest(
                "exalted-3e-lunars",
                "{\"sourceId\":\"exalted-3e-lunars\",\"displayName\":\"Lunars\",\"exaltTypes\":[\"LUNAR\"]}");
        writeManifest(
                "exalted-3e-core",
                "{\"sourceId\":\"exalted-3e-core\",\"displayName\":\"Core\",\"exaltTypes\":[\"SOLAR\",\"LUNAR\"]}");
    }

    @AfterEach
    void clearDataDirectory() {
        System.clearProperty("vibethema.data.dir");
    }

    @Test
    void discoversSourcesInStableOrderAndFiltersByExaltType() {
        SourceCatalog catalog = new SourceCatalog();

        assertEquals(
                java.util.List.of("exalted-3e-core", "exalted-3e-lunars"),
                catalog.getSources().stream().map(source -> source.sourceId).toList());
        assertEquals(1, catalog.getSourcesFor(ExaltType.SOLAR).size());
        assertEquals(2, catalog.getSourcesFor(ExaltType.LUNAR).size());
        assertTrue(catalog.findSource("exalted-3e-core").isPresent());
    }

    @Test
    void ignoresDirectoriesWithoutValidManifests() throws Exception {
        Files.createDirectories(PathService.getOfficialSourcePath("broken"));
        Files.writeString(
                PathService.getOfficialSourcePath("broken").resolve("manifest.json"),
                "{\"displayName\":\"Missing id\"}");

        SourceCatalog catalog = new SourceCatalog();

        assertEquals(2, catalog.getSources().size());
        assertTrue(catalog.findSource("broken").isEmpty());
    }

    @Test
    void filtersCharmAndMeritLoadingByExaltType() throws Exception {
        Path coreCharms = PathService.getOfficialSourcePath("exalted-3e-core").resolve("charms");
        Files.createDirectories(coreCharms);
        Files.writeString(
                coreCharms.resolve("athletics.json"),
                "{\"ability\":\"Athletics\",\"charms\":[{\"id\":\"solar\",\"name\":\"Solar Charm\",\"category\":\"solar\"}]}" );
        Path lunarsCharms = PathService.getOfficialSourcePath("exalted-3e-lunars").resolve("charms");
        Files.createDirectories(lunarsCharms);
        Files.writeString(
                lunarsCharms.resolve("athletics.json"),
                "{\"ability\":\"Athletics\",\"charms\":[{\"id\":\"lunar\",\"name\":\"Lunar Charm\",\"category\":\"solar\"}]}" );

        CharmDataService service = new CharmDataService();

        assertEquals(1, service.loadCharmsForAbility("Athletics", ExaltType.SOLAR).size());
        assertEquals(2, service.loadCharmsForAbility("Athletics", ExaltType.LUNAR).size());
    }

    private void writeManifest(String sourceId, String json) throws Exception {
        Path sourcePath = PathService.getOfficialSourcePath(sourceId);
        Files.createDirectories(sourcePath);
        Files.writeString(sourcePath.resolve("manifest.json"), json);
    }
}
