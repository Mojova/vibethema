package com.vibethema.service;

import static org.junit.jupiter.api.Assertions.*;

import com.vibethema.model.traits.MeritReference;
import java.util.List;
import org.junit.jupiter.api.Test;

public class MeritServiceTest {

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
        MeritReference artifact = merits.stream()
                .filter(m -> "Artifact".equals(m.getName()))
                .findFirst()
                .orElseThrow();
        
        assertEquals("Story", artifact.getCategory());
        assertNotNull(artifact.getRatings());
        assertFalse(artifact.getRatings().isEmpty());
    }
}
