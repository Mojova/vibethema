package com.vibethema.model;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;

import java.io.File;
import java.io.PrintWriter;

public class PdfInspector {
    public static void main(String[] args) {
        try (PDDocument doc = Loader.loadPDF(new File("src/main/resources/interactive_sheet.pdf"))) {
            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
            if (acroForm != null) {
                try (PrintWriter pw = new PrintWriter("pdf_fields.txt")) {
                    for (PDField field : acroForm.getFields()) {
                        pw.println(field.getFullyQualifiedName() + " | " + field.getFieldType() + " | Value: " + field.getValueAsString());
                    }
                }
                System.out.println("Fields written to pdf_fields.txt");
            } else {
                System.out.println("NO ACROFORM FOUND.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
