package com.vibethema.service.pdf.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vibethema.service.DefinitionId;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CoreAnimaExtractorTest {
    @TempDir Path output;

    @Test
    void importsTextMetadataAndContinuationWithoutSurroundingSections() throws Exception {
        var extractor = extractor("test-source");
        extractor.extractAndSave(sample(), "");
        JsonObject result = read();
        assertEquals("test-source", result.get("sourceId").getAsString());
        var abilities = result.getAsJsonArray("animaAbilities");
        assertEquals(7, abilities.size());
        var universal = abilities.get(0).getAsJsonObject();
        assertTrue(universal.getAsJsonArray("castes").isEmpty());
        assertEquals("SOLAR", universal.getAsJsonArray("exaltTypes").get(0).getAsString());
        assertTrue(universal.get("description").getAsString().contains("one mote"));
        assertEquals(2, universal.getAsJsonArray("effects").size());
        var levels = abilities.get(1).getAsJsonObject();
        assertEquals("Anima Levels", levels.get("name").getAsString());
        assertEquals(4, levels.getAsJsonArray("levels").size());
        assertEquals(
                "Bonfire/Iconic",
                levels.getAsJsonArray("levels").get(3).getAsJsonObject().get("name").getAsString());
        assertEquals(
                "A brilliant display continues.",
                levels.getAsJsonArray("levels")
                        .get(3)
                        .getAsJsonObject()
                        .get("description")
                        .getAsString());
        assertTrue(levels.get("description").getAsString().contains("When your character spends"));
        assertTrue(
                levels.getAsJsonArray("effects")
                        .get(0)
                        .getAsString()
                        .contains("It takes about an hour"));
        var dawn = abilities.get(2).getAsJsonObject();
        assertEquals("DAWN", dawn.getAsJsonArray("castes").get(0).getAsString());
        assertEquals("A continued effect.", dawn.getAsJsonArray("effects").get(0).getAsString());
        var eclipse = abilities.get(6).getAsJsonObject();
        assertEquals(3, eclipse.getAsJsonArray("effects").size());
        assertEquals(
                "First effect. Additional paragraph.",
                eclipse.getAsJsonArray("effects").get(0).getAsString());
        assertEquals("Second effect.", eclipse.getAsJsonArray("effects").get(1).getAsString());
        String json = result.toString();
        for (String excluded :
                new String[] {"Advancement", "Banner", "TRAITS", "EX3", "In addition"}) {
            assertFalse(json.contains(excluded), excluded);
        }
        assertEquals(
                DefinitionId.from("test-source", "anima", "SOLAR", "Eclipse"),
                eclipse.get("id").getAsString());
        extractor.extractAndSave(sample(), "");
        assertEquals(result, read(), "Reimports must produce identical data");
        extractor("another-source").extractAndSave(sample(), "");
        assertNotEquals(
                universal.get("id"),
                read().getAsJsonArray("animaAbilities").get(0).getAsJsonObject().get("id"));
    }

    @Test
    void rejectsMissingOrIncompleteSectionsBeforeWriting() {
        for (String text :
                new String[] {
                    "Unrelated text",
                    sample().replace("Night Anima Effects", "Missing"),
                    sample().replace("• Second effect.", ""),
                    sample().replace("Character Advancement", "Missing"),
                    sample().replace("Glowing", "Missing"),
                    sample().replace("It takes about an hour", "Missing")
                }) {
            assertThrows(
                    IOException.class, () -> extractor("test-source").extractAndSave(text, ""));
            assertFalse(Files.exists(output.resolve("anima/anima.json")));
        }
    }

    @Test
    void importsAllSeventeenEffectsFromLocalCorePdf() throws Exception {
        Path pdf = Path.of("data_source/core_book.pdf");
        assumeTrue(Files.exists(pdf), "Local Core PDF is optional");
        try (var document = Loader.loadPDF(pdf.toFile())) {
            var stripper = new PDFTextStripper();
            stripper.setStartPage(176);
            stripper.setEndPage(178);
            extractor("exalted-3e-core").extractAndSave(stripper.getText(document), "");
        }
        var abilities = read().getAsJsonArray("animaAbilities");
        assertEquals(7, abilities.size());
        int total = 0;
        for (var entry : abilities) {
            if (entry.getAsJsonObject().has("levels")) continue;
            var effects = entry.getAsJsonObject().getAsJsonArray("effects");
            total += effects.size();
            for (var effect : effects) {
                assertFalse(effect.getAsString().isBlank());
                assertFalse(effect.getAsString().contains("EX3"));
                assertFalse(effect.getAsString().contains("Character Advancement"));
            }
        }
        assertEquals(17, total);
        assertTrue(
                abilities
                        .get(0)
                        .getAsJsonObject()
                        .get("description")
                        .getAsString()
                        .contains("do not count as dice added by a Charm"));
        var levels = abilities.get(1).getAsJsonObject();
        var entries = levels.getAsJsonArray("levels");
        assertEquals(
                java.util.List.of("Dim", "Glowing", "Burning", "Bonfire/Iconic"),
                entries.asList().stream()
                        .map(level -> level.getAsJsonObject().get("name").getAsString())
                        .toList());
        assertTrue(
                entries.get(0)
                        .getAsJsonObject()
                        .get("description")
                        .getAsString()
                        .contains("invisible to all senses"));
        assertTrue(
                entries.get(1)
                        .getAsJsonObject()
                        .get("description")
                        .getAsString()
                        .contains("-3 penalty"));
        assertTrue(
                entries.get(2)
                        .getAsJsonObject()
                        .get("description")
                        .getAsString()
                        .endsWith("Stealth becomes impossible."));
        assertTrue(
                entries.get(3)
                        .getAsJsonObject()
                        .get("description")
                        .getAsString()
                        .endsWith("reappear at dramatic moments."));
        assertFalse(levels.toString().contains("EX3"));
        assertTrue(levels.get("description").getAsString().contains("five or more motes"));
        assertTrue(
                levels.getAsJsonArray("effects")
                        .get(0)
                        .getAsString()
                        .endsWith("dims and vanishes."));
        var twilight = abilities.get(4).getAsJsonObject().getAsJsonArray("effects");
        assertTrue(twilight.get(1).getAsString().contains("precise center of a city"));
        var eclipse = abilities.get(6).getAsJsonObject().getAsJsonArray("effects");
        assertTrue(eclipse.get(0).getAsString().contains("for his life."));
        assertTrue(eclipse.get(1).getAsString().contains("diplomatic immunity"));
        assertTrue(eclipse.get(2).getAsString().endsWith("eight experience points each."));
    }

    private CoreAnimaExtractor extractor(String sourceId) {
        var extractor = new CoreAnimaExtractor();
        extractor.setSourceId(sourceId);
        extractor.setOverrideOutputPath(output);
        return extractor;
    }

    private JsonObject read() throws IOException {
        return JsonParser.parseString(Files.readString(output.resolve("anima/anima.json")))
                .getAsJsonObject();
    }

    private String sample() {
        return """
        The Anima Banner
        Unrelated banner text.
        When your character spends Essence, the display intensifies.
        Anima Level\t Effects
        Dim\t Hidden display.
        Glowing\t A faint display.
        Burning\t A bright display.
        Bonfire/\t A brilliant display
        Iconic\t continues.
        It takes about an hour for the display to fade.
        Anima Effects
        Shared rules. Spend one mote.
        • Shared first.
        • Shared second.
        In addition to these universal effects, caste benefits follow.
        Dawn Anima Effects
        • A con-
          tinued effect.
        • Another effect.
        • A third effect.
        Zenith Anima Effects
        • A.
        • B.
        • C.
        Twilight Anima Effects
        • D.
        • E.
        • F.
        Night Anima Effects
        • G.
        • H.
        • I.
        Eclipse Anima Effects
        • First effect.
          Additional paragraph.
        T R A I T S
        EX3
        177
        • Second effect.
        • Third effect.
        Character Advancement
        Other rules.
        • Not an anima ability.
        """;
    }
}
