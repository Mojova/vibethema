package com.vibethema.service;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SystemDataServiceTest {

    @TempDir Path dataDirectory;

    @BeforeEach
    void setUp() throws Exception {
        System.setProperty("vibethema.data.dir", dataDirectory.toString());
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("vibethema.data.dir");
    }

    @Test
    void testIsCoreDataImported() {
        // This test verifies the service logic doesn't crash.
        // It relies on CharmDataService.getUserCharmsPath() which is hardcoded.
        SystemDataService service = new SystemDataService();
        assertDoesNotThrow(service::isCoreDataImported);
    }

    @Test
    void recognizesCoreImportInOfficialSourceLayout() throws Exception {
        Path keywords =
                PathService.getOfficialSourcePath("exalted-3e-core").resolve("charms/keywords.json");
        Files.createDirectories(keywords.getParent());
        Files.writeString(keywords, "[]", StandardCharsets.UTF_8);

        assertTrue(new SystemDataService().isCoreDataImported());
    }
}
