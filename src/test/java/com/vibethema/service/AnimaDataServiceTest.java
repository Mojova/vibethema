package com.vibethema.service;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.vibethema.model.Caste;
import com.vibethema.model.ExaltType;
import com.vibethema.model.mystic.AnimaAbility;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AnimaDataServiceTest {
    @TempDir Path directory;
    private String previousDirectory;

    @BeforeEach
    void setup() {
        previousDirectory = System.getProperty("vibethema.data.dir");
        System.setProperty("vibethema.data.dir", directory.toString());
    }

    @AfterEach
    void cleanup() {
        if (previousDirectory == null) System.clearProperty("vibethema.data.dir");
        else System.setProperty("vibethema.data.dir", previousDirectory);
    }

    @Test
    void loadsGeneralThenSelectedCasteAndFiltersExaltType() throws Exception {
        writeSource(
                "solar",
                "SOLAR",
                List.of(
                        ability("Dawn", "SOLAR", "DAWN"), ability("Night", "SOLAR", "NIGHT"),
                        ability("General", "SOLAR"), ability("Other type", "LUNAR")));
        writeSource("lunar", "LUNAR", List.of(ability("Lunar", "LUNAR")));
        var service = new AnimaDataService();
        assertEquals(
                List.of("General", "Dawn"),
                service.loadAbilities(ExaltType.SOLAR, Caste.DAWN).stream()
                        .map(AnimaAbility::name)
                        .toList());
        assertEquals(
                List.of("General", "Night"),
                service.loadAbilities(ExaltType.SOLAR, Caste.NIGHT).stream()
                        .map(AnimaAbility::name)
                        .toList());
        assertEquals(
                List.of("Lunar"),
                service.loadAbilities(ExaltType.LUNAR, Caste.DAWN).stream()
                        .map(AnimaAbility::name)
                        .toList());
        assertTrue(service.loadAbilities(ExaltType.SOLAR, Caste.NONE).isEmpty());
        assertTrue(service.loadAbilities(null, Caste.DAWN).isEmpty());
        assertTrue(service.loadAbilities(ExaltType.SOLAR, null).isEmpty());
    }

    @Test
    void placesLevelsBetweenUniversalAndCasteEffectsRegardlessOfFileOrder() throws Exception {
        var levels =
                new AnimaAbility(
                        "levels",
                        "solar",
                        List.of("SOLAR"),
                        List.of(),
                        "Anima Levels",
                        "Intensifying",
                        List.of("Fading"),
                        List.of(new com.vibethema.model.mystic.AnimaLevel("Dim", "Hidden")));
        writeSource(
                "solar",
                "SOLAR",
                List.of(ability("Dawn", "SOLAR", "DAWN"), levels, ability("General", "SOLAR")));
        var result = new AnimaDataService().loadAbilities(ExaltType.SOLAR, Caste.DAWN);
        assertEquals(
                List.of("General", "Anima Levels", "Dawn"),
                result.stream().map(AnimaAbility::name).toList());
        assertEquals("Hidden", result.get(1).levels().getFirst().description());
        assertEquals(List.of("Fading"), result.get(1).effects());
    }

    @Test
    void handlesMissingAndMalformedDataAndRereadsImports() throws Exception {
        var service = new AnimaDataService();
        var source = PathService.getOfficialSourcePath("solar");
        var manifest = new SourceManifest("solar", "Solar", "3e", "1");
        manifest.exaltTypes.add("SOLAR");
        manifest.writeTo(source);
        assertTrue(service.loadAbilities(ExaltType.SOLAR, Caste.DAWN).isEmpty());
        Files.createDirectories(source.resolve("anima"));
        Files.writeString(source.resolve("anima/anima.json"), "invalid json");
        assertTrue(service.loadAbilities(ExaltType.SOLAR, Caste.DAWN).isEmpty());
        Files.writeString(
                source.resolve("anima/anima.json"),
                """
                {"animaAbilities":[{"name":"Legacy","exaltTypes":["SOLAR"],"castes":[],"effects":["Effect"]}]}
                """);
        var legacy = service.loadAbilities(ExaltType.SOLAR, Caste.DAWN).getFirst();
        assertEquals("Legacy", legacy.name());
        assertTrue(legacy.levels().isEmpty());
        writeSource("solar", "SOLAR", List.of(ability("Imported", "SOLAR")));
        assertEquals(
                "Imported", service.loadAbilities(ExaltType.SOLAR, Caste.DAWN).getFirst().name());
    }

    private AnimaAbility ability(String name, String type, String... castes) {
        return new AnimaAbility(
                name,
                "source",
                List.of(type),
                List.of(castes),
                name,
                "Intro",
                List.of("Effect text"));
    }

    private void writeSource(String id, String type, List<AnimaAbility> abilities)
            throws Exception {
        var source = PathService.getOfficialSourcePath(id);
        var manifest = new SourceManifest(id, id, "3e", "1");
        manifest.exaltTypes.add(type);
        manifest.writeTo(source);
        Files.createDirectories(source.resolve("anima"));
        Files.writeString(
                source.resolve("anima/anima.json"),
                new Gson().toJson(Map.of("animaAbilities", abilities)));
    }
}
