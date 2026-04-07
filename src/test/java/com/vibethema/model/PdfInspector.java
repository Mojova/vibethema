package com.vibethema.model;

import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;
import com.vibethema.model.traits.*;
import java.io.File;
import java.io.PrintWriter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdfInspector {
    private static final Logger logger = LoggerFactory.getLogger(PdfInspector.class);

    public static void main(String[] args) {
        try (PDDocument doc =
                Loader.loadPDF(new File("src/main/resources/interactive_sheet.pdf"))) {
            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
            if (acroForm != null) {
                try (PrintWriter pw = new PrintWriter("pdf_fields.txt")) {
                    for (PDField field : acroForm.getFields()) {
                        pw.println(
                                field.getFullyQualifiedName()
                                        + " | "
                                        + field.getFieldType()
                                        + " | Value: "
                                        + field.getValueAsString());
                    }
                }
                logger.info("Fields written to pdf_fields.txt");
            } else {
                logger.warn("NO ACROFORM FOUND.");
            }
        } catch (Exception e) {
            logger.error("Failed to inspect PDF fields", e);
        }
    }
}
