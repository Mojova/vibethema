package com.vibethema.scratch;

import com.vibethema.service.PdfExtractor;
import com.vibethema.service.ImportContext;
import com.vibethema.service.SourceManifest;
import java.io.File;
import java.nio.file.Paths;

public class ExtractDebug {
    public static void main(String[] args) throws Exception {
        PdfExtractor extractor = new PdfExtractor();
        File pdfFile = new File("data_source/core_book.pdf");
        extractor.extractAll(
                pdfFile,
                new ImportContext(
                        SourceManifest.coreBook(), Paths.get("debug_output/exalted-3e-core")),
                progress -> {
                    System.out.println("Progress: " + (int) (progress * 100) + "%");
                });
        System.out.println("Extraction complete. Files saved to debug_output.");
    }
}
