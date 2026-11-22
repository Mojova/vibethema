package com.vibethema.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.vibethema.model.ExaltType;
import com.vibethema.model.equipment.Weapon;
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

    @Test
    void weaponPickerMergesTheOfficialAndCustomCopiesOfUnarmed() throws Exception {
        Path official =
                PathService.getOfficialSourcePath("solar-book").resolve("equipment/weapons");
        Files.createDirectories(official);
        var unarmed =
                new Weapon(
                        Weapon.UNARMED_ID,
                        "Unarmed",
                        Weapon.WeaponRange.CLOSE,
                        Weapon.WeaponType.MORTAL,
                        Weapon.WeaponCategory.LIGHT);
        unarmed.setDescription("Official description");
        Files.writeString(
                official.resolve(Weapon.UNARMED_ID + ".json"), new Gson().toJson(unarmed.toData()));

        var equipment = new EquipmentDataService();
        assertEquals(
                "Official description", equipment.loadWeapon(Weapon.UNARMED_ID).getDescription());
        unarmed.setDescription("Custom description");
        equipment.saveWeapon(unarmed);

        var templates = equipment.getGlobalWeapons();
        assertEquals(1, templates.size());
        assertEquals(Weapon.UNARMED_ID, templates.getFirst().getId());
        assertEquals("Custom description", templates.getFirst().getDescription());
        assertEquals(
                "Custom description", equipment.loadWeapon(Weapon.UNARMED_ID).getDescription());
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
