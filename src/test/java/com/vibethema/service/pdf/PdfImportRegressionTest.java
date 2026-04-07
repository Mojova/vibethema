package com.vibethema.service.pdf;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.vibethema.service.PdfExtractor;
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
    private static final Path REFERENCE_DIR = DATA_SOURCE.resolve("reference");

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
                "",
                true,
                PdfExtractor.PdfSource.CORE,
                tempDir,
                progress -> {
                    // Log progress occasionally
                    if (progress % 0.2 < 0.01)
                        logger.info("Extraction progress: {}%", (int) (progress * 100));
                });

        // Verify all files in reference directory
        try (Stream<Path> stream = Files.walk(REFERENCE_DIR)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(
                            refPath -> {
                                Path relative = REFERENCE_DIR.relativize(refPath);
                                Path actualPath = tempDir.resolve(relative);

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
                                Path relative = tempDir.relativize(actualPath);
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

    private void assertJsonEquals(Path expected, Path actual) {
        try (Reader refReader = Files.newBufferedReader(expected, StandardCharsets.UTF_8);
                Reader actReader = Files.newBufferedReader(actual, StandardCharsets.UTF_8)) {

            JsonElement refJson = JsonParser.parseReader(refReader);
            JsonElement actJson = JsonParser.parseReader(actReader);

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
