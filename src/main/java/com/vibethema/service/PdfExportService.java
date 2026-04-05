package com.vibethema.service;

import com.vibethema.model.Ability;
import com.vibethema.model.Attribute;
import com.vibethema.model.CharacterData;
import com.vibethema.model.SystemData;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class PdfExportService {
    private static final String TEMPLATE_PATH = "/interactive_sheet.pdf";

    public void exportToPdf(CharacterData data, File outputFile) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(TEMPLATE_PATH)) {
            if (is == null) {
                throw new IOException("Template not found: " + TEMPLATE_PATH);
            }
            try (PDDocument doc = Loader.loadPDF(is.readAllBytes())) {
                PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
                if (acroForm != null) {
                    fillForm(data, acroForm);
                }
                doc.save(outputFile);
            }
        }
    }

    private void fillForm(CharacterData data, PDAcroForm acroForm) throws IOException {
        // Identity
        setField(acroForm, "name", data.nameProperty().get());
        setField(acroForm, "caste", data.casteProperty().get() != null ? data.casteProperty().get().toString() : "");
        setField(acroForm, "SA", data.supernalAbilityProperty().get());

        // 1. Physical Attributes (Column 1)
        setDotRating(acroForm, "dot", data.getAttribute(Attribute.STRENGTH).get(), 1);
        setDotRating(acroForm, "dot", data.getAttribute(Attribute.DEXTERITY).get(), 9);
        setDotRating(acroForm, "dot", data.getAttribute(Attribute.STAMINA).get(), 17);

        // 2. Social Attributes (Column 2 - Irregular indices)
        setIrregularDotRating(acroForm, new String[]{"dot6", "dot7", "dot8", "dot8a", "dot8az"}, data.getAttribute(Attribute.CHARISMA).get());
        setIrregularDotRating(acroForm, new String[]{"dot14", "dot15", "dot16", "dot16a", "dot16az"}, data.getAttribute(Attribute.MANIPULATION).get());
        setIrregularDotRating(acroForm, new String[]{"dot22", "dot23", "dot24", "dot24a", "dot24az"}, data.getAttribute(Attribute.APPEARANCE).get());

        // 3. Mental Attributes (Column 3)
        setDotRating(acroForm, "dot", data.getAttribute(Attribute.PERCEPTION).get(), 25);
        setDotRating(acroForm, "dot", data.getAttribute(Attribute.INTELLIGENCE).get(), 33);
        setDotRating(acroForm, "dot", data.getAttribute(Attribute.WITS).get(), 41);

        // Abilities (Alphabetical based on enum order, explicitly mapped due to scrambled PDF indices)
        String[][] abilityDots = {
            {"dot30", "dot31", "dot32", "dot32a", "dot32az"}, // Y=582, Ability 1 (Archery)
            {"dot38", "dot39", "dot40", "dot40a", "dot40az"}, // Y=569, Ability 2
            {"dot46", "dot47", "dot48", "dot48a", "dot48az"}, // Y=556, Ability 3
            {"dot49", "dot50", "dot51", "dot52", "dot53"},    // Y=543, Ability 4
            {"dot57", "dot58", "dot59", "dot60", "dot61"},    // Y=530, Ability 5
            {"dot65", "dot66", "dot67", "dot68", "dot69"},    // Y=517, Ability 6
            {"dot54", "dot55", "dot56", "dot56a", "dot56az"}, // Y=504, Ability 7
            {"dot62", "dot63", "dot64", "dot64a", "dot64az"}, // Y=491, Ability 8
            {"dot70", "dot71", "dot72", "dot72a", "dot72az"}, // Y=478, Ability 9
            {"dot73", "dot74", "dot75", "dot76", "dot77"},    // Y=465, Ability 10
            {"dot81", "dot82", "dot83", "dot84", "dot85"},    // Y=452, Ability 11
            {"dot89", "dot90", "dot91", "dot92", "dot93"},    // Y=439, Ability 12
            {"dot97", "dot98", "dot99", "dot100", "dot101"},  // Y=426, Ability 13
            {"dot105", "dot106", "dot107", "dot108", "dot109"},// Y=413, Ability 14
            {"dot113", "dot114", "dot115", "dot116", "dot117"},// Y=400, Ability 15
            {"dot121", "dot122", "dot123", "dot124", "dot125"},// Y=387, Ability 16
            {"dot129", "dot130", "dot131", "dot132", "dot133"},// Y=374, Ability 17
            {"dot137", "dot138", "dot139", "dot140", "dot141"},// Y=361, Ability 18
            {"dot145", "dot146", "dot147", "dot148", "dot149"},// Y=348, Ability 19
            {"dot145q", "dot146q", "dot147q", "dot148q", "dot149q"}, // Y=335, Ability 20
            {"dot78", "dot79", "dot80", "dot80a", "dot80az"}, // Y=322, Ability 21
            {"dot86", "dot87", "dot88", "dot88a", "dot88az"}, // Y=309, Ability 22
            {"dot94", "dot95", "dot96", "dot96a", "dot96az"}, // Y=296, Ability 23
            {"dot102", "dot103", "dot104", "dot104a", "dot104az"},// Y=283, Ability 24
            {"dot110", "dot111", "dot112", "dot112a", "dot112az"},// Y=270, Ability 25
            {"dot118", "dot119", "dot120", "dot120a", "dot120az"} // Y=257, Ability 26
        };

        int checkIndex = 1;
        for (Ability abil : SystemData.ABILITIES) {
            int rating = data.getAbility(abil).get();
            if (checkIndex - 1 < abilityDots.length) {
                setIrregularDotRating(acroForm, abilityDots[checkIndex - 1], rating);
            }
            setCheckbox(acroForm, "abcheck" + checkIndex, data.getFavoredAbility(abil).get() || data.getCasteAbility(abil).get());
            
            checkIndex++;
            if (checkIndex > 32) break; // PDF has limited rows
        }
    }

    private void setIrregularDotRating(PDAcroForm form, String[] fieldNames, int rating) throws IOException {
        for (int i = 0; i < fieldNames.length; i++) {
            PDField field = form.getField(fieldNames[i]);
            if (field != null) {
                field.setValue(i < rating ? "Yes" : "Off");
            }
        }
    }

    private void setField(PDAcroForm acroForm, String fieldName, String value) throws IOException {
        PDField field = acroForm.getField(fieldName);
        if (field != null) {
            field.setValue(value != null ? value : "");
        }
    }

    private void setCheckbox(PDAcroForm acroForm, String fieldName, boolean value) throws IOException {
        PDField field = acroForm.getField(fieldName);
        if (field instanceof PDCheckBox cb) {
            if (value) cb.check();
            else cb.unCheck();
        }
    }

    private void setDotRating(PDAcroForm form, String baseName, int rating, int startIndex) throws IOException {
        for (int i = 0; i < 5; i++) {
            PDField field = form.getField(baseName + (startIndex + i));
            if (field != null) {
                field.setValue(i < rating ? "Yes" : "Off");
            }
        }
    }
}
