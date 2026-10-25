package com.vibethema.service.pdf;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.vibethema.service.PdfExtractor;
import com.vibethema.service.ImportContext;
import com.vibethema.service.SourceManifest;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdfImportRegressionTest {
    private static final Logger logger = LoggerFactory.getLogger(PdfImportRegressionTest.class);
    private static final Path DATA_SOURCE = Paths.get("data_source");
    private static final Path CORE_PDF = DATA_SOURCE.resolve("core_book.pdf");
    private static final String SOURCE_ID = "exalted-3e-core";
    private static final Path REFERENCE_DIR = DATA_SOURCE.resolve("reference").resolve(SOURCE_ID);

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Test
    public void testCoreImportRegression(@TempDir Path tempDir) throws IOException {
        if (!Files.exists(CORE_PDF)) {
            logger.info("Skipping PDF regression test: {} not found.", CORE_PDF);
            return;
        }

        if (!Files.exists(REFERENCE_DIR)) {
            logger.info(
                    "Skipping PDF regression test: Reference directory {} not found.",
                    REFERENCE_DIR);
            return;
        }

        logger.info("Running PDF import regression test...");
        PdfExtractor extractor = new PdfExtractor();

        // Run full extraction to temp directory
        extractor.extractAll(
                CORE_PDF.toFile(),
                new ImportContext(
                        SourceManifest.coreBook(), tempDir.resolve(SOURCE_ID)),
                progress -> {
                    // Log progress occasionally
                    if (progress % 0.2 < 0.01)
                        logger.info("Extraction progress: {}%", (int) (progress * 100));
                });

        assertEyeOfTheUnconqueredSunPrerequisite(tempDir.resolve(SOURCE_ID));

        // Verify all files in reference directory
        try (Stream<Path> stream = Files.walk(REFERENCE_DIR)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(
                            refPath -> {
                                Path relative = REFERENCE_DIR.relativize(refPath);
                                Path actualPath = tempDir.resolve(SOURCE_ID).resolve(relative);

                                assertTrue(
                                        Files.exists(actualPath),
                                        "Expected file missing in output: " + relative);

                                logger.info("Comparing: {}", relative);
                                assertJsonEquals(refPath, actualPath);
                            });
        }

        // Warning for unverified files in output
        try (Stream<Path> stream = Files.walk(tempDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(
                            actualPath -> {
                                Path relative =
                                        tempDir.resolve(SOURCE_ID).relativize(actualPath);
                                Path refPath = REFERENCE_DIR.resolve(relative);
                                if (!Files.exists(refPath)) {
                                    logger.warn(
                                            "UNVERIFIED: File present in output but missing from"
                                                    + " reference: {}",
                                            relative);
                                }
                            });
        }
    }

    private void assertEyeOfTheUnconqueredSunPrerequisite(Path importedSource) throws IOException {
        Path awarenessFile = importedSource.resolve("charms/awareness.json");
        JsonElement awareness = JsonParser.parseString(Files.readString(awarenessFile));
        JsonElement eye =
                awareness.getAsJsonObject().getAsJsonArray("charms").asList().stream()
                        .filter(charm -> "Eye of the Unconquered Sun".equals(
                                charm.getAsJsonObject().get("name").getAsString()))
                        .findFirst()
                        .orElseThrow();
        JsonElement prerequisites = eye.getAsJsonObject().get("prerequisites");
        assertFalse(eye.getAsJsonObject().has("importWarnings"));
        assertTrue(hasCharmPrerequisite(prerequisites, "Awakening Eye"));
        JsonElement countRule = findRule(prerequisites, "countFromAbilities");
        assertNotNull(countRule, "The non-Excellency Awareness count should be structured");
        assertEquals(3, countRule.getAsJsonObject().get("count").getAsInt());
        assertEquals(
                "Awareness",
                countRule.getAsJsonObject().getAsJsonArray("abilities").get(0).getAsString());
        assertTrue(countRule.getAsJsonObject().get("excludeExcellencies").getAsBoolean());
        java.util.Set<String> excludedIds = new java.util.HashSet<>();
        countRule.getAsJsonObject()
                .getAsJsonArray("excludedCharmIds")
                .forEach(id -> excludedIds.add(id.getAsString()));
        assertEquals(java.util.Set.of(charmId("Awakening Eye")), excludedIds);

        java.util.Set<java.util.Set<String>> prerequisitePaths = new java.util.HashSet<>();
        countRule.getAsJsonObject()
                .getAsJsonArray("excludedPrerequisitePaths")
                .forEach(
                        path -> {
                            java.util.Set<String> ids = new java.util.HashSet<>();
                            path.getAsJsonArray().forEach(id -> ids.add(id.getAsString()));
                            prerequisitePaths.add(ids);
                        });
        String sensoryAcuity = charmId("Sensory Acuity Prana");
        assertEquals(
                java.util.Set.of(
                        java.util.Set.of(
                                sensoryAcuity,
                                charmId("Keen Sight Technique"),
                                charmId("Keen Hearing and Touch Technique")),
                        java.util.Set.of(
                                sensoryAcuity,
                                charmId("Keen Sight Technique"),
                                charmId("Keen Taste and Smell Technique")),
                        java.util.Set.of(
                                sensoryAcuity,
                                charmId("Keen Hearing and Touch Technique"),
                                charmId("Keen Taste and Smell Technique"))),
                prerequisitePaths,
                "Each valid two-of-three Awakening Eye path should be an alternative exclusion");
    }

    private String charmId(String name) {
        return com.vibethema.service.DefinitionId.forCharm(SOURCE_ID, name, "Awareness");
    }

    private boolean hasCharmPrerequisite(JsonElement rule, String charmName) {
        if (rule == null || !rule.isJsonObject()) return false;
        JsonElement charmId = rule.getAsJsonObject().get("charmId");
        if ("charm".equals(rule.getAsJsonObject().get("type").getAsString())
                && charmId != null
                && charmId.getAsString().equals(
                        com.vibethema.service.DefinitionId.forCharm(
                                SOURCE_ID, charmName, "Awareness"))) return true;
        return hasRuleInChildren(rule, "requirements", charmName)
                || hasRuleInChildren(rule, "options", charmName);
    }

    private boolean hasRuleInChildren(JsonElement rule, String key, String charmName) {
        JsonElement children = rule.getAsJsonObject().get(key);
        if (children == null || !children.isJsonArray()) return false;
        for (JsonElement child : children.getAsJsonArray()) {
            if (hasCharmPrerequisite(child, charmName)) return true;
        }
        return false;
    }

    private JsonElement findRule(JsonElement rule, String type) {
        if (rule == null || !rule.isJsonObject()) return null;
        JsonElement ruleType = rule.getAsJsonObject().get("type");
        if (ruleType != null && type.equals(ruleType.getAsString())) return rule;
        for (String key : new String[] {"requirements", "options"}) {
            JsonElement children = rule.getAsJsonObject().get(key);
            if (children != null && children.isJsonArray()) {
                for (JsonElement child : children.getAsJsonArray()) {
                    JsonElement found = findRule(child, type);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    private void assertJsonEquals(Path expected, Path actual) {
        try (Reader refReader = Files.newBufferedReader(expected, StandardCharsets.UTF_8);
                Reader actReader = Files.newBufferedReader(actual, StandardCharsets.UTF_8)) {

            JsonElement refJson = JsonParser.parseReader(refReader);
            JsonElement actJson = JsonParser.parseReader(actReader);

            if (expected.getFileName().toString().equals("manifest.json")) {
                refJson.getAsJsonObject().remove("importedAt");
                actJson.getAsJsonObject().remove("importedAt");
            }

            // Using Gson's built-in equality which is content-based and formatting-agnostic
            if (!refJson.equals(actJson)) {
                // If they don't match, print them for debugging (trimmed)
                String expectedStr = gson.toJson(refJson);
                String actualStr = gson.toJson(actJson);
                assertEquals(
                        expectedStr, actualStr, "Content mismatch in " + expected.getFileName());
            }
        } catch (IOException e) {
            fail("Failed to read JSON files for comparison: " + e.getMessage());
        }
    }
}
