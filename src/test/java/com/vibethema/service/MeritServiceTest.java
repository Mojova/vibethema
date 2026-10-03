package com.vibethema.service;

import static org.junit.jupiter.api.Assertions.*;

import com.vibethema.model.traits.MeritReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MeritServiceTest {

    @TempDir Path dataDirectory;

    @BeforeEach
    void setUp() throws Exception {
        System.setProperty("vibethema.data.dir", dataDirectory.toString());
        Path file = dataDirectory.resolve("official/exalted-3e-core/merits/merits.json");
        Files.createDirectories(file.getParent());
        Files.writeString(
                file,
                "{\"merits\":[{\"name\":\"Artifact\",\"category\":\"Story\",\"ratings\":[1]}]}",
                StandardCharsets.UTF_8);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("vibethema.data.dir");
    }

    @Test
    public void testLoadMerits() {
        MeritService service = new MeritService();
        List<MeritReference> merits = service.getAvailableMerits();

        assertNotNull(merits);
        assertFalse(merits.isEmpty(), "Merits list should not be empty");

        // Check for legendary merits like "Allies" or "Artifact"
        boolean hasArtifact = merits.stream().anyMatch(m -> "Artifact".equals(m.getName()));
        assertTrue(hasArtifact, "Should contain 'Artifact' merit");

        // Check structure of one merit
        MeritReference artifact =
                merits.stream()
                        .filter(m -> "Artifact".equals(m.getName()))
                        .findFirst()
                        .orElseThrow();

        assertEquals("Story", artifact.getCategory());
        assertNotNull(artifact.getRatings());
        assertFalse(artifact.getRatings().isEmpty());
    }
}
