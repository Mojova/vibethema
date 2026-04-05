package com.vibethema.model;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;

import java.io.File;

public class PdfInspector {
    public static void main(String[] args) {
        try (PDDocument doc = org.apache.pdfbox.Loader.loadPDF(new File("src/main/resources/interactive_sheet.pdf"))) {
            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
            if (acroForm != null) {
                System.out.println("FOUND ACROFORM. Listed fields:");
                for (PDField field : acroForm.getFields()) {
                    System.out.println("- " + field.getFullyQualifiedName() + " (Type: " + field.getFieldType() + ")");
                }
            } else {
                System.out.println("NO ACROFORM FOUND IN PDF.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
