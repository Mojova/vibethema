package com.vibethema.service.pdf;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import com.vibethema.model.ExaltType;
import com.vibethema.model.mystic.CharmKind;
import com.vibethema.service.*;
import com.vibethema.service.pdf.base.BaseCharmExtractor;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class CharmImportFormatTest {
    @TempDir Path directory;

    @Test
    void extractorWritesNativeFamilyFoldersAndUnownedMartialArts() throws Exception {
        class Extractor extends BaseCharmExtractor {
            @Override
            public void extractAndSave(String text, String suffix) {}

            void write() throws Exception {
                exaltType = ExaltType.SOLAR;
                setOverrideOutputPath(directory);
                saveCharms(
                        Map.of(
                                "Melee",
                                List.of(
                                        new LinkedHashMap<>(
                                                Map.of(
                                                        "id",
                                                        "solar",
                                                        "name",
                                                        "Test Solar",
                                                        "ability",
                                                        "Melee")))),
                        "",
                        false);
                saveCharms(
                        Map.of(
                                "Test Style",
                                List.of(
                                        new LinkedHashMap<>(
                                                Map.of(
                                                        "id",
                                                        "ma",
                                                        "name",
                                                        "Test Technique",
                                                        "ability",
                                                        "Test Style")))),
                        "",
                        true);
            }
        }
        new Extractor().write();
        Path solar = directory.resolve("charms/solar/melee.json");
        Path martial = directory.resolve("charms/martial_arts/test-style.json");
        assertTrue(Files.exists(solar));
        assertTrue(Files.exists(martial));
        assertTrue(Files.exists(directory.resolve("charms/charm-schema.json")));
        CharmJson codec = new CharmJson();
        assertEquals(ExaltType.SOLAR, codec.read(solar).charms.getFirst().getExaltType());
        assertEquals(CharmKind.MARTIAL_ARTS, codec.read(martial).charms.getFirst().getKind());
        assertNull(codec.read(martial).charms.getFirst().getExaltType());
        assertFalse(
                JsonParser.parseString(Files.readString(martial))
                        .getAsJsonObject()
                        .has("exaltType"));
        assertFalse(Files.exists(directory.resolve("charms/melee.json")));
    }

    @Test
    void localCorePdfProducesLoadableCurrentFormatWithoutChangingReferenceFixtures()
            throws Exception {
        Path pdf = Path.of("data_source/core_book.pdf");
        Assumptions.assumeTrue(Files.exists(pdf), "Local Core PDF is unavailable");
        String previous = System.getProperty("vibethema.data.dir");
        System.setProperty("vibethema.data.dir", directory.toString());
        try {
            Path source = directory.resolve("official/exalted-3e-core");
            new PdfExtractor()
                    .extractAll(
                            pdf.toFile(),
                            new ImportContext(SourceManifest.coreBook(), source),
                            null);
            CharmDataService service = new CharmDataService();
            assertTrue(new SystemDataService().isCoreDataImported());
            var solar = service.loadCharmsForAbility("Archery", ExaltType.SOLAR);
            assertFalse(solar.isEmpty());
            assertTrue(
                    solar.stream()
                            .allMatch(c ->
                                    c.getKind() == CharmKind.EXALTED
                                            && c.getExaltType() == ExaltType.SOLAR));
            assertTrue(service.getGlobalCharmCount() > solar.size());
            String parentId =
                    DefinitionId.forCharm(
                            "exalted-3e-core",
                            CharmKind.EXALTED,
                            ExaltType.SOLAR,
                            "Awakening Eye",
                            "Awareness");
            String childId =
                    DefinitionId.forCharm(
                            "exalted-3e-core",
                            CharmKind.EXALTED,
                            ExaltType.SOLAR,
                            "Eye of the Unconquered Sun",
                            "Awareness");
            assertNotNull(service.findCharmById(parentId));
            assertTrue(
                    new com.google.gson.Gson()
                            .toJson(service.findCharmById(childId).getPrerequisites())
                            .contains(parentId));
            assertFalse(service.getAvailableMartialArtsStyles().isEmpty());
            for (String style : service.getAvailableMartialArtsStyles()) {
                var charms = service.loadCharmsForAbility(style, ExaltType.SOLAR);
                assertFalse(charms.isEmpty());
                assertTrue(
                        charms.stream()
                                .allMatch(c ->
                                        c.getKind() == CharmKind.MARTIAL_ARTS
                                                && c.getExaltType() == null));
            }
        } finally {
            if (previous == null) System.clearProperty("vibethema.data.dir");
            else System.setProperty("vibethema.data.dir", previous);
        }
    }
}
