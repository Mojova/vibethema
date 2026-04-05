package com.vibethema.service;

import com.vibethema.model.CharacterData;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class PdfExportService {
    private static final String TEMPLATE_PATH = "/interactive_sheet.pdf";

    public void exportToPdf(CharacterData data, File outputFile) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(TEMPLATE_PATH)) {
            if (is == null) {
                throw new IOException("Template not found: " + TEMPLATE_PATH);
            }
            try (PDDocument doc = Loader.loadPDF(is.readAllBytes())) {
                PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
                if (acroForm != null) {
                    fillForm(data, acroForm);
                }
                doc.save(outputFile);
            }
        }
    }

    private void fillForm(CharacterData data, PDAcroForm acroForm) throws IOException {
        // Phase 1: Basic setup, no data mapping yet.
        // We'll add mappings in Phase 2-4.
    }
}
