package com.vibethema.service;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParseException;
import com.vibethema.model.ExaltType;
import com.vibethema.model.mystic.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class CharmFamilyCatalogTest {
    @TempDir Path directory;

    @BeforeEach
    void configure() {
        System.setProperty("vibethema.data.dir", directory.toString());
    }

    @AfterEach
    void clear() {
        System.clearProperty("vibethema.data.dir");
    }

    private Path write(String path, String text) throws Exception {
        Path file = directory.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
        return file;
    }

    @Test
    void metadataFiltersMixedBooksAndSharedStylesAreNotSolar() throws Exception {
        write(
                "official/mixed/charms/solar/melee.json",
                CharmFixtures.solar("mixed", "Melee", "[{\"id\":\"solar\",\"name\":\"Solar\"}]"));
        write(
                "official/mixed/charms/lunar/melee.json",
                CharmFixtures.nativeCharms(
                        "mixed", "LUNAR", "Melee", "[{\"id\":\"lunar\",\"name\":\"Lunar\"}]"));
        // A misleading filename/directory cannot turn a shared discipline into a Solar Charm.
        write(
                "official/mixed/charms/solar/unrelated-name.json",
                CharmFixtures.martialArts(
                        "mixed", "Melee", "[{\"id\":\"shared\",\"name\":\"Shared\"}]"));
        CharmDataService service = new CharmDataService();
        var solar = service.loadCharmsForAbility("Melee", ExaltType.SOLAR);
        assertEquals(2, solar.size());
        assertEquals(1, solar.stream().filter(c -> c.getExaltType() == ExaltType.SOLAR).count());
        assertEquals(
                1,
                solar.stream()
                        .filter(c -> c instanceof MartialArtsCharm && c.getExaltType() == null)
                        .count());
        assertThrows(
                JsonParseException.class,
                () -> service.loadCharmsForAbility("Melee", ExaltType.LUNAR));
    }

    @Test
    void discoversOfficialStylesAndRoundTripsCustomStylesAndEvocations() throws Exception {
        write(
                "official/core/charms/martial_arts/test-style.json",
                CharmFixtures.martialArts(
                        "core",
                        "Official Style",
                        "[{\"id\":\"official-style\",\"name\":\"Test\"}]"));
        CharmDataService service = new CharmDataService();
        assertTrue(service.getAvailableMartialArtsStyles().contains("Official Style"));
        service.createNewMartialArtsStyle("style-id", "Custom Style");
        MartialArtsCharm charm = new MartialArtsCharm();
        charm.setName("Custom Technique");
        charm.setAbility("Custom Style");
        charm.setCustom(true);
        service.saveCharm(charm);
        assertEquals(1, service.loadCharmsForAbility("Custom Style").size());
        assertNull(service.findCharmById(charm.getId()).getExaltType());
        service.updateMartialArtsStyleName("style-id", "Renamed Style");
        assertEquals("Renamed Style", service.findCharmById(charm.getId()).getAbility());
        service.deleteCustomCharm(service.findCharmById(charm.getId()));
        assertNull(service.findCharmById(charm.getId()));
        Evocation evocation = new Evocation();
        evocation.setName("Custom Evocation");
        evocation.setArtifactId("artifact");
        service.saveEvocation("artifact", "Test Artifact", evocation);
        assertInstanceOf(Evocation.class, service.loadEvocations("artifact").evocations.getFirst());
        assertNull(service.findCharmById(evocation.getId()).getExaltType());
        service.deleteEvocation("artifact", evocation);
        assertTrue(service.loadEvocations("artifact").evocations.isEmpty());
    }

    @Test
    void customOverridesKeepTheirFamilyAndCanRemovePrerequisites() throws Exception {
        write(
                "official/core/charms/solar/melee.json",
                CharmFixtures.solar(
                        "core",
                        "Melee",
                        "[{\"id\":\"test\",\"name\":\"Original\",\"prerequisites\":{\"type\":\"charm\",\"charmId\":\"parent\"}}]"));
        CharmDataService service = new CharmDataService();
        Charm charm = service.findCharmById("test");
        charm.setName("Edited");
        charm.setPrerequisites(null);
        service.saveCharm(charm);
        assertEquals("Edited", service.loadCharmsForAbility("Melee").getFirst().getName());
        assertNull(service.findCharmById("test").getPrerequisites());
        assertEquals(ExaltType.SOLAR, service.findCharmById("test").getExaltType());
        assertEquals("core", service.findCharmById("test").getSourceId());
        service.deleteCustomCharm(charm);
        assertEquals("Original", service.findCharmById("test").getName());
    }

    @Test
    void customEvocationsRetainTheirOwnSourceAlongsideEditedOfficialOnes() throws Exception {
        write(
                "official/core/charms/evocations/artifact.json",
                CharmFixtures.evocations(
                        "core", "artifact", "[{\"id\":\"official\",\"name\":\"Official\"}]"));
        CharmDataService service = new CharmDataService();
        Charm official = service.findCharmById("official");
        official.setName("Edited Official");
        service.saveEvocation("artifact", "Artifact", official);
        Evocation custom = new Evocation();
        custom.setName("Custom");
        custom.setArtifactId("artifact");
        service.saveEvocation("artifact", "Artifact", custom);
        assertEquals(2, service.loadEvocations("artifact").evocations.size());
        assertEquals(
                java.util.Set.of("artifact"), service.getArtifactIdsWithEvocationCollections());
        assertEquals("core", service.findCharmById("official").getSourceId());
        assertEquals("custom", service.findCharmById(custom.getId()).getSourceId());
    }

    @Test
    void pdfExportResolvesAllKindsByIdentity() throws Exception {
        write(
                "official/core/charms/solar/melee.json",
                CharmFixtures.solar(
                        "core", "Melee", "[{\"id\":\"solar\",\"name\":\"Synthetic Solar\"}]"));
        write(
                "official/core/charms/martial_arts/style.json",
                CharmFixtures.martialArts(
                        "core",
                        "Test Style",
                        "[{\"id\":\"martial\",\"name\":\"Synthetic Martial\"}]"));
        write(
                "official/core/charms/evocations/artifact.json",
                CharmFixtures.evocations(
                        "core",
                        "artifact",
                        "[{\"id\":\"evocation\",\"name\":\"Synthetic Evocation\"}]"));
        var character = new com.vibethema.model.CharacterData();
        var service = new CharmDataService();
        for (String id : java.util.List.of("solar", "martial", "evocation")) {
            Charm charm = service.findCharmById(id);
            character.addCharm(new PurchasedCharm(id, charm.getName(), charm.getAbility()));
        }
        Path pdf = directory.resolve("charms.pdf");
        new PdfExportService().exportCharmsToPdf(character, pdf.toFile(), service);
        try (var document = org.apache.pdfbox.Loader.loadPDF(pdf.toFile())) {
            String text = new org.apache.pdfbox.text.PDFTextStripper().getText(document);
            assertTrue(text.contains("Synthetic Solar"));
            assertTrue(text.contains("Synthetic Martial"));
            assertTrue(text.contains("Synthetic Evocation"));
        }
    }
}
