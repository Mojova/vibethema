package com.vibethema.util;

import com.vibethema.service.ImportContext;
import com.vibethema.service.PathService;
import com.vibethema.service.PdfExtractor;
import com.vibethema.service.SourceManifest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * CLI utility to run the PDF extraction process and inspect the results. Usage: mvn compile
 * exec:java -Dexec.mainClass="com.vibethema.util.PdfImportTool" -Dexec.args="<pdf-path>
 * <source-id> <output-root>"
 */
public class PdfImportTool {

    public static void main(String[] args) {
        if (args.length < 1 || args.length == 2) {
            System.err.println(
                    "Usage: PdfImportTool <pdf-path> [<source-id> <output-root>]");
            System.exit(1);
        }

        String pdfPath = args[0];
        String sourceId = args.length > 1 ? args[1] : SourceManifest.coreBook().sourceId;
        Path outputDir =
                args.length > 1
                        ? Paths.get(args[2]).resolve(sourceId)
                        : PathService.getOfficialSourcePath(sourceId);

        SourceManifest manifest =
                sourceId.equals(SourceManifest.coreBook().sourceId)
                        ? SourceManifest.coreBook()
                        : new SourceManifest(sourceId, sourceId, "3e", "imported");
        ImportContext context = new ImportContext(manifest, outputDir);

        System.out.println("Using source directory: " + outputDir);

        File pdfFile = new File(pdfPath);
        if (!pdfFile.exists()) {
            System.err.println("Error: PDF file not found at " + pdfPath);
            System.exit(1);
        }

        PdfExtractor extractor = new PdfExtractor();
        System.out.println("Starting extraction from: " + pdfFile.getAbsolutePath());

        try {
            extractor.extractAll(
                    pdfFile,
                    context,
                    progress -> System.out.printf("Progress: %.1f%%\r", progress * 100));
            System.out.println("\nExtraction Complete!");

            inspectResults(outputDir);

        } catch (IOException e) {
            System.err.println("\nError during extraction: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void inspectResults(Path dataPath) {
        System.out.println("\n--- Post-Import Inspection ---");
        System.out.println("Data Path: " + dataPath);

        System.out.println("\n[Charms]");
        Path charmsPath = dataPath.resolve("charms");
        if (Files.exists(charmsPath)) {
            try (Stream<Path> files = Files.list(charmsPath)) {
                files.filter(f -> f.toString().endsWith(".json"))
                        .forEach(
                                f -> {
                                    try {
                                        long count = countCharmsInFile(f);
                                        System.out.printf(
                                                "  - %-20s: %d charms\n", f.getFileName(), count);
                                    } catch (IOException e) {
                                        System.err.println("Error reading " + f.getFileName());
                                    }
                                });
            } catch (IOException e) {
                System.err.println("Could not list charms directory.");
            }
        } else {
            System.out.println("  No charms directory found.");
        }

        System.out.println("\n[Spells]");
        Path spellsPath = dataPath.resolve("spells");
        if (Files.exists(spellsPath)) {
            try (Stream<Path> files = Files.list(spellsPath)) {
                files.filter(f -> f.toString().endsWith(".json"))
                        .forEach(f -> System.out.println("  - " + f.getFileName()));
            } catch (IOException e) {
                System.err.println("Could not list spells directory.");
            }
        } else {
            System.out.println("  No spells directory found.");
        }

        System.out.println("\n[Equipment]");
        Path equipPath = dataPath.resolve("equipment");
        if (Files.exists(equipPath)) {
            try (Stream<Path> files = Files.list(equipPath)) {
                files.filter(f -> f.toString().endsWith(".json"))
                        .forEach(f -> System.out.println("  - " + f.getFileName()));
            } catch (IOException e) {
                System.err.println("Could not list equipment directory.");
            }
        } else {
            System.out.println("  No equipment directory found.");
        }

        System.out.println("\n--- Inspection Finished ---");
    }

    private static long countCharmsInFile(Path file) throws IOException {
        String content = Files.readString(file);
        // Simple heuristic: count occurrences of "\"id\":"
        long count = 0;
        int lastIndex = 0;
        while ((lastIndex = content.indexOf("\"id\":", lastIndex)) != -1) {
            count++;
            lastIndex += 5;
        }
        // Subtract 1 if the file itself has an ID (like a wrapper object),
        // but our JSON schema has "id" for each charm.
        // Actually, checking for 'name' might be safer for charm count.
        return count;
    }
}
