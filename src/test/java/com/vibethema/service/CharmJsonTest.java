package com.vibethema.service;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import com.vibethema.model.ExaltType;
import com.vibethema.model.mystic.*;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class CharmJsonTest {
    @TempDir Path directory;
    private final CharmJson codec = new CharmJson();

    private CharmCollection read(String text) {
        return codec.read(JsonParser.parseString(text).getAsJsonObject());
    }

    @Test
    void collectionMetadataClassifiesNativeAndSharedCharms() {
        var solar =
                read(
                        CharmFixtures.solar(
                                "core", "Melee", "[{\"id\":\"solar\",\"name\":\"Test\"}]"));
        assertInstanceOf(SolarCharm.class, solar.charms.getFirst());
        assertEquals(CharmKind.EXALTED, solar.charms.getFirst().getKind());
        assertEquals(ExaltType.SOLAR, solar.charms.getFirst().getExaltType());
        assertEquals("core", solar.charms.getFirst().getSourceId());
        assertEquals("Melee", solar.charms.getFirst().getAbility());
        var martial =
                read(
                        CharmFixtures.martialArts(
                                "core", "Test Style", "[{\"id\":\"ma\",\"name\":\"Test\"}]"));
        assertInstanceOf(MartialArtsCharm.class, martial.charms.getFirst());
        assertEquals(CharmKind.MARTIAL_ARTS, martial.charms.getFirst().getKind());
        assertNull(martial.charms.getFirst().getExaltType());
        var evocations =
                read(
                        CharmFixtures.evocations(
                                "core", "artifact", "[{\"id\":\"ev\",\"name\":\"Test\"}]"));
        assertInstanceOf(Evocation.class, evocations.charms.getFirst());
        assertEquals("artifact", evocations.charms.getFirst().getAbility());
        assertNull(evocations.charms.getFirst().getExaltType());
    }

    @Test
    void roundTripInheritsMetadataWithoutRepeatingItOnEntries() throws Exception {
        var collection =
                read(
                        CharmFixtures.solar(
                                "core",
                                "Melee",
                                """
                                [{"id":"stable-id","name":"Test","minAbility":3,"minEssence":2,
                                  "prerequisites":{"type":"charm","charmId":"parent"},"type":"Reflexive"}]
                                """));
        Path path = directory.resolve("solar/melee.json");
        codec.write(path, collection);
        JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        var entry = json.getAsJsonArray("charms").get(0).getAsJsonObject();
        for (String field :
                List.of("kind", "exaltType", "exaltTypes", "category", "sourceId", "ability"))
            assertFalse(entry.has(field), field);
        assertEquals("Reflexive", entry.get("type").getAsString());
        var restored = codec.read(path).charms.getFirst();
        assertEquals("stable-id", restored.getId());
        assertEquals(ExaltType.SOLAR, restored.getExaltType());
        assertEquals("Melee", restored.getAbility());
        assertEquals("core", restored.getSourceId());
        assertEquals("parent", restored.getPrerequisites().getCharmId());
        assertEquals(3, restored.getMinAbility());
    }

    @Test
    void rejectsLegacyMissingConflictingAndUnsupportedMetadata() {
        assertThrows(JsonParseException.class, () -> read("{\"ability\":\"Melee\",\"charms\":[]}"));
        assertThrows(
                JsonParseException.class,
                () ->
                        read(
                                CharmFixtures.solar(
                                        "core",
                                        "Melee",
                                        "[{\"id\":\"test\",\"name\":\"Test\",\"category\":\"solar\"}]")));
        JsonObject json =
                JsonParser.parseString(CharmFixtures.solar("core", "Melee", "[]"))
                        .getAsJsonObject();
        json.remove("exaltType");
        assertThrows(JsonParseException.class, () -> codec.read(json));
        json.addProperty("kind", "unknown");
        assertThrows(JsonParseException.class, () -> codec.read(json));
        var unsupported =
                assertThrows(
                        JsonParseException.class,
                        () -> read(CharmFixtures.nativeCharms("lunars", "LUNAR", "Melee", "[]")));
        assertTrue(unsupported.getMessage().contains("LUNAR"));
        JsonObject martial =
                JsonParser.parseString(CharmFixtures.martialArts("core", "Test Style", "[]"))
                        .getAsJsonObject();
        martial.addProperty("exaltType", "SOLAR");
        assertThrows(JsonParseException.class, () -> codec.read(martial));
        assertThrows(
                JsonParseException.class,
                () ->
                        read(
                                CharmFixtures.solar(
                                        "core",
                                        "Melee",
                                        "[{\"id\":\"test\",\"name\":\"Test\",\"exaltType\":\"LUNAR\"}]")));
    }

    @Test
    void fileErrorsIncludeThePathAndDuplicateIdsAreRejected() throws Exception {
        Path file = directory.resolve("bad.json");
        Files.writeString(file, CharmFixtures.nativeCharms("source", "UNKNOWN", "Melee", "[]"));
        assertTrue(
                assertThrows(JsonParseException.class, () -> codec.read(file))
                        .getMessage()
                        .contains(file.toString()));
        assertThrows(
                JsonParseException.class,
                () ->
                        read(
                                CharmFixtures.solar(
                                        "core",
                                        "Melee",
                                        "[{\"id\":\"same\",\"name\":\"A\"},{\"id\":\"same\",\"name\":\"B\"}]")));
    }

    @Test
    void invalidEditsUseTheSaveErrorPathAndPreserveTheExistingFile() throws Exception {
        var collection =
                read(
                        CharmFixtures.solar(
                                "custom", "Melee", "[{\"id\":\"test\",\"name\":\"Valid\"}]"));
        Path file = directory.resolve("solar/melee.json");
        codec.write(file, collection);
        String before = Files.readString(file);
        collection.charms.getFirst().setName("");
        var error = assertThrows(java.io.IOException.class, () -> codec.write(file, collection));
        assertTrue(error.getMessage().contains("name"));
        assertEquals(before, Files.readString(file));
    }
}
