package com.vibethema.scratch;

import java.io.File;
import java.io.FileWriter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class DumpText {
    public static void main(String[] args) throws Exception {
        int startPage = args.length > 0 ? Integer.parseInt(args[0]) : 300;
        int endPage = args.length > 1 ? Integer.parseInt(args[1]) : 305;

        try (PDDocument document = Loader.loadPDF(new File("data_source/core_book.pdf"))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(startPage);
            stripper.setEndPage(endPage);
            String text = stripper.getText(document);
            String filename = "raw_text_" + startPage + "_" + endPage + ".txt";
            try (FileWriter writer = new FileWriter(filename)) {
                writer.write(text);
            }
            System.out.println("Dumped pages " + startPage + "-" + endPage + " to " + filename);
        }
    }
}
