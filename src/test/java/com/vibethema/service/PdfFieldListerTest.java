package com.vibethema.service;

import com.vibethema.model.CharacterData;
import com.vibethema.model.PurchasedCharm;
import com.vibethema.model.Spell;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PdfFieldListerTest {
    @Test
    public void listFields() throws IOException {
        InputStream is = getClass().getResourceAsStream("/interactive_sheet.pdf");
        if (is == null) {
            System.out.println("Template not found!");
            return;
        }
        try (PDDocument doc = Loader.loadPDF(is.readAllBytes())) {
            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
            if (acroForm != null) {
                List<PDField> fields = acroForm.getFields();
                for (PDField field : fields) {
                    int page = field.getWidgets().isEmpty() ? -1 : doc.getPages().indexOf(field.getWidgets().get(0).getPage());
                    System.out.println("Field: " + field.getFullyQualifiedName() + " type: " + field.getFieldType() + " Page: " + (page + 1));
                }
            }
        }
    }

    @Test
    public void testExportCharms() throws IOException {
        // 1. Prepare an Evocation file in the expected path
        String artifactId = "soul-cleaver";
        java.nio.file.Path evocationDir = CharmDataService.getUserEvocationsPath();
        if (!java.nio.file.Files.exists(evocationDir)) {
            java.nio.file.Files.createDirectories(evocationDir);
        }
        java.nio.file.Path evocationPath = evocationDir.resolve(artifactId + ".json");
        String evocationJson = "{\"artifactId\":\"soul-cleaver\",\"artifactName\":\"Soul Cleaver\",\"evocations\":[{\"id\":\"evoc-1\",\"name\":\"Death-Cutting Strike\",\"category\":\"evocation\",\"cost\":\"3m\",\"duration\":\"Instant\",\"source\":\"Core\"}]}";
        java.nio.file.Files.writeString(evocationPath, evocationJson);

        CharacterData data = new CharacterData();
        data.nameProperty().set("Charm Test Hero");
        
        // Add a spell
        data.getSpells().add(new Spell("Death of Obsidian Butterflies", Spell.Circle.TERRESTRIAL));
        data.getSpells().get(0).setCost("15sm");
        data.getSpells().get(0).setDuration("Instant");

        // Add an evocation
        data.getUnlockedCharms().add(new PurchasedCharm("evoc-1", "Death-Cutting Strike", artifactId));

        PdfExportService service = new PdfExportService();
        File tempFile = File.createTempFile("charm_test", ".pdf");
        System.out.println("Exporting to: " + tempFile.getAbsolutePath());
        service.exportToPdf(data, tempFile);
        
        assertTrue(tempFile.exists());
        
        // Cleanup temp evocation file
        java.nio.file.Files.deleteIfExists(evocationPath);
        tempFile.deleteOnExit();
    }
    @Test
    public void testExportOtherEquipmentEvocations() throws IOException {
        String uuidId = java.util.UUID.randomUUID().toString();
        java.nio.file.Path evocationDir = CharmDataService.getUserEvocationsPath();
        if (!java.nio.file.Files.exists(evocationDir)) {
            java.nio.file.Files.createDirectories(evocationDir);
        }
        java.nio.file.Path evocationPath = evocationDir.resolve(uuidId + ".json");
        
        // Simulating the "old format" or just a basic evocation file
        String evocationJson = "{\"artifactName\":\"Mysterious Amulet\",\"evocations\":[{\"id\":\"amulet-1\",\"name\":\"Eye of the Unseen\",\"category\":\"evocation\",\"cost\":\"5m\",\"duration\":\"One Scene\",\"source\":\"Custom\"}]}";
        java.nio.file.Files.writeString(evocationPath, evocationJson);

        CharacterData data = new CharacterData();
        data.getUnlockedCharms().add(new PurchasedCharm("amulet-1", "Eye of the Unseen", uuidId));

        CharmDataService dataService = new CharmDataService();
        com.vibethema.model.Charm metadata = dataService.findCharmMetadata(data.getUnlockedCharms().get(0));
        org.junit.jupiter.api.Assertions.assertNotNull(metadata, "Metadata should be found for UUID-linked evocation");
        org.junit.jupiter.api.Assertions.assertEquals("Eye of the Unseen", metadata.getName());

        PdfExportService service = new PdfExportService();
        File tempFile = File.createTempFile("uuid_evoc_test", ".pdf");
        service.exportToPdf(data, tempFile);
        
        assertTrue(tempFile.exists());
        System.out.println("UUID Evocation Exported to: " + tempFile.getAbsolutePath());
        
        // Cleanup
        java.nio.file.Files.deleteIfExists(evocationPath);
        tempFile.deleteOnExit();
    }
}
