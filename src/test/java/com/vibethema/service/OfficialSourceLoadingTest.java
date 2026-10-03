package com.vibethema.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OfficialSourceLoadingTest {
    @TempDir Path dataDirectory;

    @BeforeEach
    void setDataDirectory() {
        System.setProperty("vibethema.data.dir", dataDirectory.toString());
    }

    @AfterEach
    void clearDataDirectory() {
        System.clearProperty("vibethema.data.dir");
    }

    @Test
    void loadsMeritsSpellsTagsAndCharmCountsFromOfficialSource() throws Exception {
        Path source = PathService.getOfficialSourcePath("exalted-3e-core");
        Files.createDirectories(source.resolve("merits"));
        Files.createDirectories(source.resolve("spells"));
        Files.createDirectories(source.resolve("charms"));
        Files.createDirectories(source.resolve("equipment"));

        Files.writeString(
                source.resolve("merits/merits.json"),
                "{\"merits\":[{\"name\":\"Artifact\",\"category\":\"Story\"}]}",
                StandardCharsets.UTF_8);
        Files.writeString(
                source.resolve("spells/terrestrial.json"),
                "{\"spells\":[{\"id\":\"spell-1\",\"name\":\"Fire\",\"circle\":\"Terrestrial\"}]}",
                StandardCharsets.UTF_8);
        Files.writeString(
                source.resolve("charms/athletics.json"),
                "{\"ability\":\"Athletics\",\"charms\":[{\"id\":\"charm-1\",\"name\":\"Run\",\"category\":\"solar\"}]}",
                StandardCharsets.UTF_8);
        Files.writeString(
                source.resolve("equipment/equipment_tags.json"),
                "{\"weapon\":[{\"id\":\"tag-1\",\"name\":\"Reach\"}]}",
                StandardCharsets.UTF_8);

        assertEquals(1, new MeritService().getAvailableMerits().size());
        assertEquals(1, new CharmDataService().loadSpells("terrestrial").size());
        assertEquals(1, new CharmDataService().getGlobalCharmCount());
        Map<String, List<EquipmentDataService.Tag>> tags = new EquipmentDataService().loadEquipmentTags();
        assertTrue(tags.containsKey("weapon"));
        assertEquals("Reach", tags.get("weapon").get(0).getName());
    }

    @Test
    void customDataIsKeptSeparateFromOfficialData() throws Exception {
        Path source = PathService.getOfficialSourcePath("exalted-3e-core");
        Files.createDirectories(source.resolve("charms"));
        Files.createDirectories(PathService.getCustomDataPath().resolve("charms"));
        Files.writeString(
                source.resolve("charms/athletics.json"),
                "{\"ability\":\"Athletics\",\"charms\":[{\"id\":\"official\",\"name\":\"Official\",\"category\":\"solar\"}]}",
                StandardCharsets.UTF_8);
        Files.writeString(
                PathService.getCustomDataPath().resolve("charms/athletics.json"),
                "{\"ability\":\"Athletics\",\"charms\":[{\"id\":\"custom\",\"name\":\"Custom\",\"category\":\"solar\"}]}",
                StandardCharsets.UTF_8);

        assertEquals(2, new CharmDataService().loadCharmsForAbility("Athletics").size());
        assertEquals(PathService.getCustomDataPath().resolve("charms"), CharmDataService.getUserCharmsPath());
    }
}
