package com.vibethema.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

class PdfExtractorValidationTest {
    @Test
    void rejectsPdfTooShortToBeCoreBook() throws Exception {
        try (PDDocument document = new PDDocument()) {
            for (int i = 0; i < 10; i++) document.addPage(new PDPage());

            assertThrows(IOException.class, () -> PdfExtractor.validateCorePdf(document));
        }
    }

    @Test
    void rejectsLongPdfWithoutCoreSectionMarkers() throws Exception {
        try (PDDocument document = new PDDocument()) {
            addPages(document, 610);

            assertThrows(IOException.class, () -> PdfExtractor.validateCorePdf(document));
        }
    }

    @Test
    void acceptsLongPdfWithMarkersAcrossExpectedSections() throws Exception {
        try (PDDocument document = new PDDocument()) {
            addPages(document, 610);
            addText(document, 249, "Charms");
            addText(document, 469, "Sorcery");
            addText(document, 584, "weapons");
            addText(document, 156, "Merits");

            assertDoesNotThrow(() -> PdfExtractor.validateCorePdf(document));
        }
    }

    private static void addPages(PDDocument document, int count) {
        for (int i = 0; i < count; i++) document.addPage(new PDPage());
    }

    private static void addText(PDDocument document, int pageIndex, String text) throws IOException {
        try (PDPageContentStream stream =
                new PDPageContentStream(document, document.getPage(pageIndex))) {
            stream.beginText();
            stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            stream.newLineAtOffset(50, 700);
            stream.showText(text);
            stream.endText();
        }
    }
}
