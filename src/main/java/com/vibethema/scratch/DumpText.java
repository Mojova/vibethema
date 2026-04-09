package com.vibethema.scratch;

import java.io.File;
import java.io.FileWriter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class DumpText {
    public static void main(String[] args) throws Exception {
        try (PDDocument document = Loader.loadPDF(new File("data_source/core_book.pdf"))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(300);
            stripper.setEndPage(305);
            String text = stripper.getText(document);
            try (FileWriter writer = new FileWriter("raw_text_300_305.txt")) {
                writer.write(text);
            }
        }
        System.out.println("Dumped pages 300-305 to raw_text_300_305.txt");
    }
}
