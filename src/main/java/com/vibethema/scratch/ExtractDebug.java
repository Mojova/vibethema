package com.vibethema.scratch;

import com.vibethema.service.PdfExtractor;
import java.io.File;
import java.nio.file.Paths;

public class ExtractDebug {
    public static void main(String[] args) throws Exception {
        PdfExtractor extractor = new PdfExtractor();
        File pdfFile = new File("data_source/core_book.pdf");
        extractor.extractAll(
                pdfFile,
                "",
                true,
                PdfExtractor.PdfSource.CORE,
                Paths.get("debug_output"),
                progress -> {
                    System.out.println("Progress: " + (int) (progress * 100) + "%");
                });
        System.out.println("Extraction complete. Files saved to debug_output.");
    }
}
