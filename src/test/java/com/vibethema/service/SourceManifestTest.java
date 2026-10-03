package com.vibethema.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceManifestTest {
    @Test
    void coreManifestRoundTrips(@TempDir Path tempDir) throws Exception {
        SourceManifest manifest = SourceManifest.coreBook();
        manifest.writeTo(tempDir);

        assertTrue(Files.exists(tempDir.resolve("manifest.json")));
        SourceManifest loaded = SourceManifest.readFrom(tempDir);

        assertEquals("exalted-3e-core", loaded.sourceId);
        assertEquals("Exalted Third Edition Core", loaded.displayName);
        assertEquals("3e", loaded.edition);
        assertEquals("core", loaded.version);
        assertEquals(java.util.List.of("SOLAR"), loaded.exaltTypes);
    }
}
