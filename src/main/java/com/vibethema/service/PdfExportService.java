package com.vibethema.service;

import com.vibethema.model.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.traits.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;

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

    public void exportCharmsToPdf(
            CharacterData data, File outputFile, CharmDataService charmService) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            ExportContext ctx = new ExportContext(doc);

            // 1. Resolve and Sort Charms
            List<Charm> allPurchased = new ArrayList<>();
            Map<String, List<Charm>> abilityCache = new HashMap<>();

            for (PurchasedCharm pc : data.getUnlockedCharms()) {
                String ability = pc.ability();
                List<Charm> charms =
                        abilityCache.computeIfAbsent(
                                ability, k -> charmService.loadCharmsForAbility(ability));
                charms.stream()
                        .filter(c -> c.getId().equals(pc.id()))
                        .findFirst()
                        .ifPresent(allPurchased::add);
            }

            // Grouping: Charms vs Martial Arts
            List<Charm> normalCharms = new ArrayList<>();
            List<Charm> martialArtsCharms = new ArrayList<>();
            for (Charm c : allPurchased) {
                if ("martialArts".equalsIgnoreCase(c.getCategory())) {
                    martialArtsCharms.add(c);
                } else {
                    normalCharms.add(c);
                }
            }

            Comparator<Charm> charmComparator =
                    (c1, c2) -> {
                        int abilityComp = c1.getAbility().compareTo(c2.getAbility());
                        if (abilityComp != 0) return abilityComp;
                        int essenceComp = Integer.compare(c1.getMinEssence(), c2.getMinEssence());
                        if (essenceComp != 0) return essenceComp;
                        int minAbComp = Integer.compare(c1.getMinAbility(), c2.getMinAbility());
                        if (minAbComp != 0) return minAbComp;
                        return c1.getName().compareTo(c2.getName());
                    };

            normalCharms.sort(charmComparator);
            martialArtsCharms.sort(charmComparator);

            // 2. Resolve and Sort Spells
            List<Spell> spells = new ArrayList<>(data.getSpells());
            spells.sort(
                    (s1, s2) -> {
                        int circleComp =
                                Spell.Circle.valueOf(s1.getCircle()).ordinal()
                                        - Spell.Circle.valueOf(s2.getCircle()).ordinal();
                        if (circleComp != 0) return circleComp;
                        return s1.getName().compareTo(s2.getName());
                    });

            // 3. Rendering
            ctx.drawMainTitle(data.nameProperty().get() + " - Charms & Spells");

            if (!normalCharms.isEmpty()) {
                ctx.drawSectionHeader("Charms", ctx.getCharmHeaderHeight(normalCharms.get(0)));
                String currentAbility = "";
                for (Charm c : normalCharms) {
                    if (!c.getAbility().equals(currentAbility)) {
                        currentAbility = c.getAbility();
                        ctx.drawSubsectionHeader(currentAbility, ctx.getCharmHeaderHeight(c));
                    }
                    ctx.drawCharm(c);
                }
            }

            if (!martialArtsCharms.isEmpty()) {
                ctx.drawSectionHeader(
                        "Martial Arts", ctx.getCharmHeaderHeight(martialArtsCharms.get(0)));
                String currentStyle = "";
                for (Charm c : martialArtsCharms) {
                    if (!c.getAbility().equals(currentStyle)) {
                        currentStyle = c.getAbility();
                        ctx.drawSubsectionHeader(currentStyle, ctx.getCharmHeaderHeight(c));
                    }
                    ctx.drawCharm(c);
                }
            }

            if (!spells.isEmpty()) {
                ctx.drawSectionHeader("Spells", ctx.getSpellHeaderHeight(spells.get(0)));
                String currentCircle = "";
                for (Spell s : spells) {
                    if (!s.getCircle().equals(currentCircle)) {
                        currentCircle = s.getCircle();
                        ctx.drawSubsectionHeader(
                                currentCircle + " Circle", ctx.getSpellHeaderHeight(s));
                    }
                    ctx.drawSpell(s);
                }
            }

            ctx.close();
            doc.save(outputFile);
        }
    }

    private static class ExportContext {
        private final PDDocument doc;
        private PDPage currentPage;
        private PDPageContentStream contentStream;
        private float currentY;
        private float columnTopY;
        private int currentColumn = 0; // 0 or 1

        private final float margin = 50;
        private final float columnSpacing = 30;
        private final float pageWidth = PDRectangle.A4.getWidth();
        private final float pageHeight = PDRectangle.A4.getHeight();
        private final float columnWidth = (pageWidth - (2 * margin) - columnSpacing) / 2;

        private final PDFont fontBold;
        private final PDFont fontItalic;
        private final PDFont fontRegular;

        public ExportContext(PDDocument doc) throws IOException {
            this.doc = doc;
            this.fontBold = loadFont("/fonts/LibertinusSerif-Bold.ttf", Standard14Fonts.FontName.TIMES_BOLD);
            this.fontItalic =
                    loadFont("/fonts/LibertinusSerif-Italic.ttf", Standard14Fonts.FontName.TIMES_ITALIC);
            this.fontRegular =
                    loadFont("/fonts/LibertinusSerif-Regular.ttf", Standard14Fonts.FontName.TIMES_ROMAN);
            addNewPage();
        }

        private PDFont loadFont(String resourcePath, Standard14Fonts.FontName fallback)
                throws IOException {
            try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    return new PDType1Font(fallback);
                }
                return PDType0Font.load(doc, is);
            }
        }

        private void addNewPage() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
            currentPage = new PDPage(PDRectangle.A4);
            doc.addPage(currentPage);
            contentStream = new PDPageContentStream(doc, currentPage);
            currentY = pageHeight - margin;
            columnTopY = currentY;
            currentColumn = 0;
        }

        private void ensureSpace(float spaceNeeded) throws IOException {
            if (currentY - spaceNeeded < margin) {
                if (currentColumn == 0) {
                    currentColumn = 1;
                    currentY = columnTopY;
                } else {
                    addNewPage();
                }
            }
        }

        public void drawMainTitle(String title) throws IOException {
            ensureSpace(30);
            contentStream.beginText();
            contentStream.setFont(fontBold, 18);
            contentStream.newLineAtOffset(margin, currentY);
            contentStream.showText(title);
            contentStream.endText();
            currentY -= 40;
            columnTopY = currentY;
        }

        public void drawSectionHeader(String title, float lookaheadHeight) throws IOException {
            ensureSpace(50 + lookaheadHeight);
            if (currentY < columnTopY) {
                contentStream.setLineWidth(1.5f);
                contentStream.moveTo(margin, currentY);
                contentStream.lineTo(pageWidth - margin, currentY);
                contentStream.stroke();
            }
            currentY -= 20;

            contentStream.beginText();
            contentStream.setFont(fontBold, 14);
            contentStream.newLineAtOffset(margin, currentY);
            contentStream.showText(title.toUpperCase());
            contentStream.endText();
            currentY -= 20;

            // Reset column state
            currentColumn = 0;
            columnTopY = currentY;
        }

        public void drawSubsectionHeader(String title, float lookaheadHeight) throws IOException {
            ensureSpace(40 + lookaheadHeight);
            float x = margin + (currentColumn * (columnWidth + columnSpacing));

            if (currentY < columnTopY) {
                contentStream.setLineWidth(1.0f);
                contentStream.moveTo(x, currentY);
                contentStream.lineTo(x + columnWidth, currentY);
                contentStream.stroke();
            }
            currentY -= 15;

            contentStream.beginText();
            contentStream.setFont(fontBold, 12);
            contentStream.newLineAtOffset(x, currentY);
            contentStream.showText(title);
            contentStream.endText();
            currentY -= 15;
        }

        public void drawCharm(Charm c) throws IOException {
            float headerHeight = getCharmHeaderHeight(c);
            ensureSpace(headerHeight);
            float x = margin + (currentColumn * (columnWidth + columnSpacing));

            contentStream.beginText();
            contentStream.setFont(fontBold, 10);
            contentStream.newLineAtOffset(x, currentY);
            contentStream.showText(sanitizeText(c.getName()));
            contentStream.endText();
            currentY -= 12;

            drawField("Cost:", c.getCost(), x);
            drawField("Mins:", getCharmMinsString(c), x);
            drawField("Type:", c.getType(), x);
            drawField("Keywords:", getCharmKeywordsString(c), x);
            drawField("Duration:", c.getDuration(), x);

            currentY -= 5; // Spacer
            if (c.getFullText() != null && !c.getFullText().isEmpty()) {
                drawWrappedText(c.getFullText(), 8, x);
            }
            currentY -= 15;
        }

        public void drawSpell(Spell s) throws IOException {
            float headerHeight = getSpellHeaderHeight(s);
            ensureSpace(headerHeight);
            float x = margin + (currentColumn * (columnWidth + columnSpacing));

            contentStream.beginText();
            contentStream.setFont(fontBold, 10);
            contentStream.newLineAtOffset(x, currentY);
            contentStream.showText(sanitizeText(s.getName()));
            contentStream.endText();
            currentY -= 12;

            drawField("Circle:", s.getCircle(), x);
            drawField("Cost:", s.getCost(), x);
            drawField("Keywords:", getSpellKeywordsString(s), x);
            drawField("Duration:", s.getDuration(), x);

            currentY -= 5; // Spacer
            if (s.getDescription() != null && !s.getDescription().isEmpty()) {
                drawWrappedText(s.getDescription(), 8, x);
            }
            currentY -= 15;
        }

        private void drawField(String label, String value, float x) throws IOException {
            if (!isFieldVisible(value)) return;

            ensureSpace(11);
            float currentX = margin + (currentColumn * (columnWidth + columnSpacing));

            contentStream.beginText();
            contentStream.setFont(fontBold, 9);
            contentStream.newLineAtOffset(currentX, currentY);
            contentStream.showText(sanitizeText(label));

            float labelWidth = fontBold.getStringWidth(sanitizeText(label)) / 1000 * 9;
            contentStream.setFont(fontRegular, 9);
            contentStream.newLineAtOffset(labelWidth, 0);
            contentStream.showText(sanitizeText(" " + value));
            contentStream.endText();

            currentY -= 11;
        }

        private boolean isFieldVisible(String value) {
            return value != null
                    && !value.isEmpty()
                    && !value.equalsIgnoreCase("None")
                    && !value.equals("0");
        }

        public float getCharmHeaderHeight(Charm c) {
            float h = 12 + 5;
            if (isFieldVisible(c.getCost())) h += 11;
            if (isFieldVisible(getCharmMinsString(c))) h += 11;
            if (isFieldVisible(c.getType())) h += 11;
            if (isFieldVisible(getCharmKeywordsString(c))) h += 11;
            if (isFieldVisible(c.getDuration())) h += 11;
            return h;
        }

        private String getCharmMinsString(Charm c) {
            return String.format(
                    "%s %d, Essence %d", c.getAbility(), c.getMinAbility(), c.getMinEssence());
        }

        private String getCharmKeywordsString(Charm c) {
            return (c.getKeywords() != null && !c.getKeywords().isEmpty()
                    ? String.join(", ", c.getKeywords())
                    : null);
        }

        public float getSpellHeaderHeight(Spell s) {
            float h = 12 + 5;
            if (isFieldVisible(s.getCircle())) h += 11;
            if (isFieldVisible(s.getCost())) h += 11;
            if (isFieldVisible(getSpellKeywordsString(s))) h += 11;
            if (isFieldVisible(s.getDuration())) h += 11;
            return h;
        }

        private String getSpellKeywordsString(Spell s) {
            return (s.getKeywords() != null && !s.getKeywords().isEmpty()
                    ? String.join(", ", s.getKeywords())
                    : null);
        }

        private void drawWrappedText(String text, float fontSize, float x) throws IOException {
            if (text == null || text.isEmpty()) return;

            // Split by newline to preserve paragraph markers
            String[] paragraphs = text.split("\\r?\\n", -1);

            for (String paragraph : paragraphs) {
                if (paragraph.trim().isEmpty()) {
                    // Handle blank lines for double newlines
                    currentY -= (fontSize + 2);
                    continue;
                }

                List<String> lines = new ArrayList<>();
                String[] words = paragraph.split("\\s+");
                StringBuilder line = new StringBuilder();

                for (String word : words) {
                    if (word.isEmpty()) continue;
                    float width = fontRegular.getStringWidth(line + " " + word) / 1000 * fontSize;
                    if (width > columnWidth) {
                        lines.add(line.toString().trim());
                        line = new StringBuilder(word).append(" ");
                    } else {
                        line.append(word).append(" ");
                    }
                }
                lines.add(line.toString().trim());

                for (String l : lines) {
                    ensureSpace(fontSize + 2);
                    float currentX = margin + (currentColumn * (columnWidth + columnSpacing));
                    contentStream.beginText();
                    contentStream.setFont(fontRegular, fontSize);
                    contentStream.newLineAtOffset(currentX, currentY);
                    contentStream.showText(sanitizeText(l));
                    contentStream.endText();
                    currentY -= (fontSize + 2);
                }
            }
        }

        private String sanitizeText(String text) {
            if (text == null) return "";
            // PDFBox showText doesn't support newlines; replace them with spaces for safety
            return text.replace("\n", " ").replace("\r", "");
        }

        public void close() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
        }
    }

    private void fillForm(CharacterData data, PDAcroForm acroForm) throws IOException {
        // Identity
        setField(acroForm, "name", data.nameProperty().get());
        setField(
                acroForm,
                "caste",
                data.casteProperty().get() != null ? data.casteProperty().get().toString() : "");
        setField(acroForm, "SA", data.supernalAbilityProperty().get());

        // 1. Physical Attributes (Column 1)
        setDotRating(acroForm, "dot", data.getAttribute(Attribute.STRENGTH).get(), 1);
        setDotRating(acroForm, "dot", data.getAttribute(Attribute.DEXTERITY).get(), 9);
        setDotRating(acroForm, "dot", data.getAttribute(Attribute.STAMINA).get(), 17);

        // 2. Social Attributes (Column 2 - Irregular indices)
        setIrregularDotRating(
                acroForm,
                new String[] {"dot6", "dot7", "dot8", "dot8a", "dot8az"},
                data.getAttribute(Attribute.CHARISMA).get());
        setIrregularDotRating(
                acroForm,
                new String[] {"dot14", "dot15", "dot16", "dot16a", "dot16az"},
                data.getAttribute(Attribute.MANIPULATION).get());
        setIrregularDotRating(
                acroForm,
                new String[] {"dot22", "dot23", "dot24", "dot24a", "dot24az"},
                data.getAttribute(Attribute.APPEARANCE).get());

        // 3. Mental Attributes (Column 3)
        setDotRating(acroForm, "dot", data.getAttribute(Attribute.PERCEPTION).get(), 25);
        setDotRating(acroForm, "dot", data.getAttribute(Attribute.INTELLIGENCE).get(), 33);
        setDotRating(acroForm, "dot", data.getAttribute(Attribute.WITS).get(), 41);

        // 4. Specialties (Middle Column, merits1-9 - No dots)
        int specIndex = 1;
        for (Specialty s : data.getSpecialties()) {
            if (s.getName() == null || s.getName().trim().isEmpty()) continue;
            String text = s.getAbility() + ": " + s.getName();
            setField(acroForm, "merits" + specIndex, text);
            specIndex++;
            if (specIndex > 9) break;
        }

        // 5. Merits (Right Column, merits10-18 - With dots)
        String[][] meritDots = {
            {"dot225q", "dot226q", "dot227q", "dot228q", "dot229q"},
            {"dot158", "dot159", "dot160", "dot160a", "dot160az"},
            {"dot166", "dot167", "dot168", "dot168a", "dot168az"},
            {"dot174", "dot175", "dot176", "dot176a", "dot176az"},
            {"dot182", "dot183", "dot184", "dot184a", "dot184az"},
            {"dot190", "dot191", "dot192", "dot192a", "dot192az"},
            {"dot198", "dot199", "dot200", "dot200a", "dot200az"},
            {"dot206", "dot207", "dot208", "dot208a", "dot208az"},
            {"dot214", "dot215", "dot216", "dot216a", "dot216az"}
        };

        int meritSlot = 0;
        for (Merit m : data.getMerits()) {
            if (m.getName() == null || m.getName().trim().isEmpty()) continue;
            setField(acroForm, "merits" + (10 + meritSlot), m.getName());
            if (meritSlot < meritDots.length) {
                setIrregularDotRating(acroForm, meritDots[meritSlot], m.getRating());
            }
            meritSlot++;
            if (meritSlot >= 9) break;
        }

        // 6. Core Trackers (Essence, Willpower, Limit)
        setDotRating(acroForm, "essdot", data.essenceProperty().get(), 1);
        setDotRating(acroForm, "willdot", data.willpowerProperty().get(), 1);
        setTrackRating(acroForm, "check", data.limitProperty().get(), 11, 10);
        setWrappedText(
                acroForm, "limitt", data.limitTriggerProperty().get(), 4, 45); // ~45 chars per line

        // 7. Weapons (8x7 grid)
        fillWeapons(data, acroForm);

        // 8. Protection and Derived Stats (HD1-19)
        fillProtection(data, acroForm);

        // 9. Specialized Abilities (Craft & Martial Arts)
        fillSpecializedAbilities(data, acroForm);

        // 10. Health Section
        fillHealth(data, acroForm);

        // Abilities (Physical mapping to the 26 standard rows Ab1-26)
        String[][] abilityDots = {
            {"dot30", "dot31", "dot32", "dot32a", "dot32az"}, // Row 1: Archery
            {"dot38", "dot39", "dot40", "dot40a", "dot40az"}, // Row 2: Athletics
            {"dot46", "dot47", "dot48", "dot48a", "dot48az"}, // Row 3: Awareness
            {"dot49", "dot50", "dot51", "dot52", "dot53"}, // Row 4: Brawl
            {"dot57", "dot58", "dot59", "dot60", "dot61"}, // Row 5: Bureaucracy
            {"dot65", "dot66", "dot67", "dot68", "dot69"}, // Row 6: CRAFT (Specialized)
            {"dot54", "dot55", "dot56", "dot56a", "dot56az"}, // Row 7: Dodge
            {"dot62", "dot63", "dot64", "dot64a", "dot64az"}, // Row 8: Integrity
            {"dot70", "dot71", "dot72", "dot72a", "dot72az"}, // Row 9: Investigation
            {"dot73", "dot74", "dot75", "dot76", "dot77"}, // Row 10: Larceny
            {"dot81", "dot82", "dot83", "dot84", "dot85"}, // Row 11: Linguistics
            {"dot89", "dot90", "dot91", "dot92", "dot93"}, // Row 12: Lore
            {"dot97", "dot98", "dot99", "dot100", "dot101"}, // Row 13: MARTIAL ARTS (Specialized)
            {"dot105", "dot106", "dot107", "dot108", "dot109"}, // Row 14: Medicine
            {"dot113", "dot114", "dot115", "dot116", "dot117"}, // Row 15: Melee
            {"dot121", "dot122", "dot123", "dot124", "dot125"}, // Row 16: Occult
            {"dot129", "dot130", "dot131", "dot132", "dot133"}, // Row 17: Performance
            {"dot137", "dot138", "dot139", "dot140", "dot141"}, // Row 18: Presence
            {"dot145", "dot146", "dot147", "dot148", "dot149"}, // Row 19: Resistance
            {"dot145q", "dot146q", "dot147q", "dot148q", "dot149q"}, // Row 20: Ride
            {"dot78", "dot79", "dot80", "dot80a", "dot80az"}, // Row 21: Sail
            {"dot86", "dot87", "dot88", "dot88a", "dot88az"}, // Row 22: Socialize
            {"dot94", "dot95", "dot96", "dot96a", "dot96az"}, // Row 23: Stealth
            {"dot102", "dot103", "dot104", "dot104a", "dot104az"}, // Row 24: Survival
            {"dot110", "dot111", "dot112", "dot112a", "dot112az"}, // Row 25: Thrown
            {"dot118", "dot119", "dot120", "dot120a", "dot120az"} // Row 26: War
        };

        Ability[] abilityList = Ability.values();
        int rowIndex = 0;
        for (Ability ability : abilityList) {
            if (ability == Ability.CRAFT || ability == Ability.MARTIAL_ARTS) {
                // Skips both because they are specialized (Craft) or separate (Martial Arts
                // style mapping)
                // However, we still increment rowIndex for both because they HAVE a row on the
                // PDF (Row 6 and Row 13)
                rowIndex++;
                continue;
            }

            // Map remaining standard abilities (Dot Tracks and Caste/Favored Checkboxes
            // only)
            if (rowIndex < abilityDots.length) {
                int rating = data.getAbilityRating(ability);
                setTrackRating(acroForm, abilityDots[rowIndex], rating);
                setCheckbox(
                        acroForm,
                        "abcheck" + (rowIndex + 1),
                        data.getCasteAbility(ability).get()
                                || data.getFavoredAbility(ability).get());
            }
            rowIndex++;
        }
    }

    private void fillSpecializedAbilities(CharacterData data, PDAcroForm form) throws IOException {
        List<CraftAbility> crafts = data.getCrafts();
        List<MartialArtsStyle> styles = data.getMartialArtsStyles();

        // 1. Principal Slots
        // Row 6: Main Craft
        if (!crafts.isEmpty()) {
            CraftAbility main = crafts.get(0);
            setField(form, "ablities6", main.getExpertise());
            String[] mainDots = {"dot65", "dot66", "dot67", "dot68", "dot69"};
            setTrackRating(form, mainDots, main.getRating());
            setCheckbox(form, "abcheck6", main.isCaste() || main.isFavored());
        }

        // Row 13: Main Martial Arts
        if (!styles.isEmpty()) {
            MartialArtsStyle mainStyle = styles.get(0);
            String name = mainStyle.getStyleName();
            if (name != null && name.length() > 5) {
                if (name.charAt(5) == ' ') {
                    name = name.substring(0, 5);
                } else {
                    name = name.substring(0, 5) + ".";
                }
            }
            setField(form, "ablities13", name);
            String[] styleDots = {"dot97", "dot98", "dot99", "dot100", "dot101"};
            setTrackRating(form, styleDots, mainStyle.getRating());
            setCheckbox(form, "abcheck13", mainStyle.isCaste() || mainStyle.isFavored());
        }

        // 2. Additional Slots (verified starting at Ab27 through Ab32)
        String[] addAbLabels = {
            "ablities27", "ablities28", "ablities29", "ablities30", "ablities31", "ablities32"
        };
        String[][] addAbDots = {
            {"dot126", "dot127", "dot128", "dot128a", "dot128az"}, // Ab27
            {"dot134", "dot135", "dot136", "dot136a", "dot136az"}, // Ab28
            {"dot142", "dot143", "dot144", "dot144a", "dot144az"}, // Ab29
            {"dot150", "dot151", "dot152", "dot152a", "dot152az"}, // Ab30
            {"dot150q", "dot151q", "dot152q", "dot152qa", "dot152qaz"}, // Ab31
            {"dot153", "dot154", "dot155", "dot156", "dot157"} // Ab32
        };

        int addSlotIdx = 0;

        // Surplus Crafts
        for (int i = 1; i < crafts.size() && addSlotIdx < addAbLabels.length; i++) {
            CraftAbility extra = crafts.get(i);
            setField(form, addAbLabels[addSlotIdx], "Craft: " + extra.getExpertise());
            setTrackRating(form, addAbDots[addSlotIdx], extra.getRating());
            setCheckbox(form, "abcheck" + (27 + addSlotIdx), extra.isCaste() || extra.isFavored());
            addSlotIdx++;
        }

        // Martial Arts Styles
        for (int i = 1; i < styles.size() && addSlotIdx < addAbLabels.length; i++) {
            MartialArtsStyle extraStyle = styles.get(i);
            setField(form, addAbLabels[addSlotIdx], "MA: " + extraStyle.getStyleName());
            setTrackRating(form, addAbDots[addSlotIdx], extraStyle.getRating());
            setCheckbox(
                    form,
                    "abcheck" + (27 + addSlotIdx),
                    extraStyle.isCaste() || extraStyle.isFavored());
            addSlotIdx++;
        }
    }

    private void fillProtection(CharacterData data, PDAcroForm form) throws IOException {
        // 1. Armor Slots (HD1-HD10)
        List<Armor> armors = data.getArmors();
        for (int i = 0; i < Math.min(2, armors.size()); i++) {
            Armor a = armors.get(i);
            int nameIdx = i + 1; // HD1, HD2
            int soakIdx = i + 3; // HD3, HD4
            int hardnessIdx = i + 5; // HD5, HD6
            int mobilityIdx = i + 7; // HD7, HD8
            int tagsIdx = i + 9; // HD9, HD10

            setField(form, "HD" + nameIdx, a.getName());
            setField(form, "HD" + soakIdx, String.valueOf(a.getSoak()));
            setField(form, "HD" + hardnessIdx, String.valueOf(a.getHardness()));
            setField(form, "HD" + mobilityIdx, String.valueOf(a.getMobilityPenalty()));
            setField(form, "HD" + tagsIdx, String.join(", ", a.getTags()));
        }

        // 2. Derived Stats (HD15-19)
        setField(form, "HD15", String.valueOf(data.evasionProperty().get()));
        setField(form, "HD16", String.valueOf(data.guileProperty().get()));

        int dex = data.getAttribute(Attribute.DEXTERITY).get();
        setField(form, "HD17", String.valueOf(dex + data.getAbilityRating(Ability.ATHLETICS)));
        setField(form, "HD18", String.valueOf(dex + data.getAbilityRating(Ability.DODGE)));
        setField(form, "HD19", String.valueOf(data.joinBattleProperty().get()));

        // 3. Final Protection Mapping
        int stamina = data.getAttribute(Attribute.STAMINA).get();
        setField(form, "HD11", String.valueOf(stamina));
        setField(form, "HD12", String.valueOf(data.totalSoakProperty().get()));
        setField(form, "HD13", ""); // Parry - Leave empty
        setField(form, "HD14", String.valueOf(data.resolveProperty().get()));

        // Mote Pools (Below Essence Dots)
        setField(form, "essence1", ""); // Leave current blank for player use
        setField(form, "essence2", String.valueOf(data.getPersonalMotes()));

        setField(form, "essence3", ""); // Leave current blank for player use
        setField(form, "essence4", String.valueOf(data.getPeripheralMotes()));
    }

    private void fillWeapons(CharacterData data, PDAcroForm form) throws IOException {
        int rowCursor = 1;
        for (AttackPoolData apd : data.getAttackPools()) {
            Weapon w = apd.getWeapon();
            boolean isRanged = w.getRange() != Weapon.WeaponRange.CLOSE;
            List<String> tags = w.getTags();
            int rowsNeeded = isRanged ? 3 : (tags.size() > 2 ? 2 : 1);

            if (rowCursor + rowsNeeded - 1 > 8) break;

            // --- Row 1 ---
            setField(form, "weapons" + rowCursor, w.getName());

            // Accuracy Row 1
            if (!isRanged) {
                setField(form, "weapons" + (rowCursor + 8), String.valueOf(w.getAccuracy()));
            } else {
                setField(
                        form,
                        "weapons" + (rowCursor + 8),
                        String.format("%d/%d", w.getCloseRangeBonus(), w.getShortRangeBonus()));
            }

            // Damage (always Row 1)
            boolean isBashing = tags.contains("Bashing");
            setField(form, "weapons" + (rowCursor + 16), apd.getDamage() + (isBashing ? "B" : "L"));

            // Defense Row 1
            setField(form, "weapons" + (rowCursor + 24), String.valueOf(w.getDefense()));

            // Overwhelming Row 1
            setField(form, "weapons" + (rowCursor + 32), String.valueOf(w.getOverwhelming()));

            // Tags Row 1
            setField(
                    form,
                    "weapons" + (rowCursor + 40),
                    String.join(", ", tags.subList(0, Math.min(2, tags.size()))));

            // Dice Pool Row 1 (Decisive only)
            setField(form, "weapons" + (rowCursor + 48), String.valueOf(apd.getDecisivePool()));

            // --- Row 2 ---
            if (rowsNeeded >= 2) {
                int row2 = rowCursor + 1;
                if (isRanged) {
                    setField(
                            form,
                            "weapons" + (row2 + 8),
                            String.format("%d/%d", w.getMediumRangeBonus(), w.getLongRangeBonus()));
                }
                if (tags.size() > 2) {
                    setField(
                            form,
                            "weapons" + (row2 + 40),
                            String.join(", ", tags.subList(2, Math.min(4, tags.size()))));
                }
            }

            // --- Row 3 ---
            if (rowsNeeded == 3) {
                int row3 = rowCursor + 2;
                setField(form, "weapons" + (row3 + 8), String.valueOf(w.getExtremeRangeBonus()));
                if (tags.size() > 4) {
                    setField(
                            form,
                            "weapons" + (row3 + 40),
                            String.join(", ", tags.subList(4, tags.size())));
                }
            }

            rowCursor += rowsNeeded;
        }
    }

    private void fillHealth(CharacterData data, PDAcroForm form) throws IOException {
        List<String> levels = data.getHealthLevels();
        // Skip the first level as it's hardcoded as -0 on the PDF
        for (int i = 1; i < levels.size(); i++) {
            String level = levels.get(i);
            String label;
            if (level.equals("Incap")) {
                label = "I";
            } else {
                // Use absolute value (e.g., "-1" -> "1", "-0" -> "0")
                label = level.replace("-", "");
            }
            if (i <= 31) {
                setField(form, "hl" + i, label);
            }
        }
        // hbox fields are left empty as per user request
    }

    private void setTrackRating(
            PDAcroForm form, String prefix, int rating, int startIndex, int count)
            throws IOException {
        for (int i = 0; i < count; i++) {
            setCheckbox(form, prefix + (startIndex + i), i < rating);
        }
    }

    private void setTrackRating(PDAcroForm form, String[] dots, int rating) throws IOException {
        for (int i = 0; i < dots.length; i++) {
            setCheckbox(form, dots[i], i < rating);
        }
    }

    private void setWrappedText(
            PDAcroForm form, String prefix, String text, int maxLines, int charsPerLine)
            throws IOException {
        if (text == null) return;

        // Simple word wrap
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        int lineIndex = 1;

        for (String word : words) {
            if (lineIndex > maxLines) break;

            if (currentLine.length() + word.length() + 1 > charsPerLine) {
                setField(form, prefix + lineIndex, currentLine.toString().trim());
                currentLine = new StringBuilder(word).append(" ");
                lineIndex++;
            } else {
                currentLine.append(word).append(" ");
            }
        }

        if (lineIndex <= maxLines && currentLine.length() > 0) {
            setField(form, prefix + lineIndex, currentLine.toString().trim());
        }
    }

    private void setIrregularDotRating(PDAcroForm form, String[] fieldNames, int rating)
            throws IOException {
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

    private void setCheckbox(PDAcroForm acroForm, String fieldName, boolean value)
            throws IOException {
        PDField field = acroForm.getField(fieldName);
        if (field instanceof PDCheckBox cb) {
            if (value) cb.check();
            else cb.unCheck();
        }
    }

    private void setDotRating(PDAcroForm form, String baseName, int rating, int startIndex)
            throws IOException {
        for (int i = 0; i < 5; i++) {
            PDField field = form.getField(baseName + (startIndex + i));
            if (field != null) {
                field.setValue(i < rating ? "Yes" : "Off");
            }
        }
    }
}
