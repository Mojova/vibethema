package com.vibethema.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vibethema.service.pdf.core.CoreCharmExtractor;
import com.vibethema.service.pdf.core.CoreSpellExtractor;
import com.vibethema.service.pdf.core.CoreEquipmentExtractor;

public class PdfExtractor {
    public enum PdfSource {
        CORE,
        MOSE
    }

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void extractAll(File pdfFile, Consumer<Double> progressCallback) throws IOException {
        extractAll(pdfFile, "", true, PdfSource.CORE, null, progressCallback);
    }

    public void extractAll(File pdfFile, String suffix, boolean extractKeywords, PdfSource source, Path outputBasePath, Consumer<Double> progressCallback) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            
            if (source == PdfSource.CORE) {
                // 1. Solar Charms (Pages 250 - 466)
                if (progressCallback != null) progressCallback.accept(0.1);
                stripper.setStartPage(250);
                stripper.setEndPage(466);
                String charmText = stripper.getText(document);
                CoreCharmExtractor charmExtractor = new CoreCharmExtractor();
                if (outputBasePath != null) charmExtractor.setOverrideOutputPath(outputBasePath.resolve("charms"));
                charmExtractor.extractAndSave(charmText, suffix);

                // 2. Sorcery (Pages 470 - 495)
                if (progressCallback != null) progressCallback.accept(0.5);
                stripper.setStartPage(470);
                stripper.setEndPage(495);
                String spellText = stripper.getText(document);
                CoreSpellExtractor spellExtractor = new CoreSpellExtractor();
                if (outputBasePath != null) spellExtractor.setOverrideOutputPath(outputBasePath.resolve("spells"));
                spellExtractor.extractAndSave(spellText, suffix);

                // 3. Equipment Tags & Gear (Pages 585 - 610)
                if (progressCallback != null) progressCallback.accept(0.8);
                stripper.setStartPage(585);
                stripper.setEndPage(610);
                String equipText = stripper.getText(document);
                CoreEquipmentExtractor equipExtractor = new CoreEquipmentExtractor();
                if (outputBasePath != null) equipExtractor.setOverrideOutputPath(outputBasePath);
                equipExtractor.extractAndSaveTags(equipText);

                // 4. Keywords (Page range overlap with charms)
                if (extractKeywords) {
                    stripper.setStartPage(250);
                    stripper.setEndPage(260);
                    extractAndSaveKeywords(stripper.getText(document), outputBasePath);
                }

                saveDefaultUnarmedWeapon(outputBasePath);
            } else if (source == PdfSource.MOSE) {
                // MoSE specifically - keep simple whole-text for now or add MoSEExtractor
                if (progressCallback != null) progressCallback.accept(0.1);
                String fullText = stripper.getText(document);
                CoreCharmExtractor charmExtractor = new CoreCharmExtractor();
                if (outputBasePath != null) charmExtractor.setOverrideOutputPath(outputBasePath.resolve("charms"));
                charmExtractor.extractAndSave(fullText, suffix); // Fallback to Core extractor for MoSE
            }
            
            if (progressCallback != null) progressCallback.accept(1.0);
        }
    }

    private void saveDefaultUnarmedWeapon(Path outputBasePath) throws IOException {
        Map<String, Object> unarmed = new LinkedHashMap<>();
        unarmed.put("id", com.vibethema.model.Weapon.UNARMED_ID);
        unarmed.put("name", "Unarmed");
        unarmed.put("range", "CLOSE");
        unarmed.put("type", "MORTAL");
        unarmed.put("category", "LIGHT");
        unarmed.put("accuracy", 0);
        unarmed.put("damage", 0);
        unarmed.put("defense", 0);
        unarmed.put("overwhelming", 1);
        unarmed.put("attunement", 0);
        unarmed.put("equipped", false);
        unarmed.put("tags", Arrays.asList("Bashing", "Brawl", "Grappling", "Natural"));

        Path outDir = outputBasePath != null ? outputBasePath.resolve("equipment").resolve("weapons") : com.vibethema.service.EquipmentDataService.getWeaponsPath();
        if (outDir == null) {
            outDir = java.nio.file.Paths.get(System.getProperty("user.home"), ".vibethema", "equipment", "weapons");
        }
        
        if (!Files.exists(outDir)) Files.createDirectories(outDir);
        Path filePath = outDir.resolve(com.vibethema.model.Weapon.UNARMED_ID + ".json");
        try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            gson.toJson(unarmed, writer);
        }
    }

    private void extractAndSaveKeywords(String text, Path outputBasePath) throws IOException {
        // Keyword list is relatively stable across books
        List<String> keywordsToFind = Arrays.asList(
            "Aggravated", "Bridge", "Clash", "Counterattack", "Decisive-only",
            "Dual", "Form", "Mastery", "Mute", "Perilous", "Pilot",
            "Psyche", "Salient", "Stackable", "Terrestrial", "Uniform",
            "Withering-only", "Written-only"
        );

        List<Map<String, String>> outputKeywords = new ArrayList<>();
        StringBuilder sb = new StringBuilder("•[^A-Z]*?(");
        for (int i = 0; i < keywordsToFind.size(); i++) {
            sb.append(Pattern.quote(keywordsToFind.get(i)));
            if (i < keywordsToFind.size() - 1) sb.append("|");
        }
        sb.append("):");
        
        Pattern pattern = Pattern.compile(sb.toString());
        Matcher matcher = pattern.matcher(text);
        
        int lastEnd = 0;
        String currentKeyword = null;
        
        while (matcher.find()) {
            if (currentKeyword != null) {
                String desc = text.substring(lastEnd, matcher.start()).trim();
                addKeyword(outputKeywords, currentKeyword, desc);
            }
            currentKeyword = matcher.group(1);
            lastEnd = matcher.end();
        }
        
        if (currentKeyword != null) {
            addKeyword(outputKeywords, currentKeyword, text.substring(lastEnd));
        }

        Path outDir = outputBasePath != null ? outputBasePath.resolve("charms") : CharmDataService.getUserCharmsPath();
        if (!Files.exists(outDir)) Files.createDirectories(outDir);
        Path filePath = outDir.resolve("keywords.json");
        try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            gson.toJson(outputKeywords, writer);
        }
    }

    private void addKeyword(List<Map<String, String>> list, String name, String rawDesc) {
        String cleaned = rawDesc.split("Duration:|Prerequisite Charms:|EX3|CHAPTER 6")[0].trim();
        Map<String, String> kw = new HashMap<>();
        kw.put("name", name);
        kw.put("description", cleaned);
        list.add(kw);
    }
}
