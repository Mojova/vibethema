package com.vibethema.service;

import com.vibethema.model.Ability;
import com.vibethema.model.Armor;
import com.vibethema.model.AttackPoolData;
import com.vibethema.model.Attribute;
import com.vibethema.model.CharacterData;
import com.vibethema.model.CraftAbility;
import com.vibethema.model.Intimacy;
import com.vibethema.model.MartialArtsStyle;
import com.vibethema.model.Merit;
import com.vibethema.model.OtherEquipment;
import com.vibethema.model.Specialty;
import com.vibethema.model.Weapon;
import com.vibethema.model.PurchasedCharm;
import com.vibethema.model.Spell;
import com.vibethema.model.Charm;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PdfExportService {
    private static final String TEMPLATE_PATH = "/interactive_sheet.pdf";
    private final CharmDataService charmDataService = new CharmDataService();

    public void exportToPdf(CharacterData data, File outputFile) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(TEMPLATE_PATH)) {
            if (is == null) {
                throw new IOException("Template not found: " + TEMPLATE_PATH);
            }
            try (PDDocument doc = Loader.loadPDF(is.readAllBytes())) {
                PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
                if (acroForm != null) {
                    Set<String> filledFields = new HashSet<>();
                    fillForm(data, acroForm, filledFields);
                }
                doc.save(outputFile);
            }
        }
    }

    private void fillForm(CharacterData data, PDAcroForm acroForm, Set<String> filled) throws IOException {
        // Identity
        setField(acroForm, "name", data.nameProperty().get(), filled);
        setField(acroForm, "caste", data.casteProperty().get() != null ? data.casteProperty().get().toString() : "", filled);
        setField(acroForm, "SA", data.supernalAbilityProperty().get(), filled);

        // 1. Physical Attributes (Column 1)
        setDotRating(acroForm, "dot", data.getAttribute(Attribute.STRENGTH).get(), 1, filled);
        setDotRating(acroForm, "dot", data.getAttribute(Attribute.DEXTERITY).get(), 9, filled);
        setDotRating(acroForm, "dot", data.getAttribute(Attribute.STAMINA).get(), 17, filled);

        // 2. Social Attributes (Column 2 - Irregular indices)
        setIrregularDotRating(acroForm, new String[] { "dot6", "dot7", "dot8", "dot8a", "dot8az" },
                data.getAttribute(Attribute.CHARISMA).get(), filled);
        setIrregularDotRating(acroForm, new String[] { "dot14", "dot15", "dot16", "dot16a", "dot16az" },
                data.getAttribute(Attribute.MANIPULATION).get(), filled);
        setIrregularDotRating(acroForm, new String[] { "dot22", "dot23", "dot24", "dot24a", "dot24az" },
                data.getAttribute(Attribute.APPEARANCE).get(), filled);

        // 3. Mental Attributes (Column 3)
        setDotRating(acroForm, "dot", data.getAttribute(Attribute.PERCEPTION).get(), 25, filled);
        setDotRating(acroForm, "dot", data.getAttribute(Attribute.INTELLIGENCE).get(), 33, filled);
        setDotRating(acroForm, "dot", data.getAttribute(Attribute.WITS).get(), 41, filled);

        // 4. Specialties (Middle Column, merits1-9 - No dots)
        int specIndex = 1;
        for (Specialty s : data.getSpecialties()) {
            if (s.getName() == null || s.getName().trim().isEmpty())
                continue;
            String text = s.getAbility() + ": " + s.getName();
            setField(acroForm, "merits" + specIndex, text, filled);
            specIndex++;
            if (specIndex > 9)
                break;
        }

        // 5. Merits (Right Column, merits10-18 - With dots)
        String[][] meritDots = {
                { "dot225q", "dot226q", "dot227q", "dot228q", "dot229q" },
                { "dot158", "dot159", "dot160", "dot160a", "dot160az" },
                { "dot166", "dot167", "dot168", "dot168a", "dot168az" },
                { "dot174", "dot175", "dot176", "dot176a", "dot176az" },
                { "dot182", "dot183", "dot184", "dot184a", "dot184az" },
                { "dot190", "dot191", "dot192", "dot192a", "dot192az" },
                { "dot198", "dot199", "dot200", "dot200a", "dot200az" },
                { "dot206", "dot207", "dot208", "dot208a", "dot208az" },
                { "dot214", "dot215", "dot216", "dot216a", "dot216az" }
        };

        int meritSlot = 0;
        for (Merit m : data.getMerits()) {
            if (m.getName() == null || m.getName().trim().isEmpty())
                continue;
            setField(acroForm, "merits" + (10 + meritSlot), m.getName(), filled);
            if (meritSlot < meritDots.length) {
                setIrregularDotRating(acroForm, meritDots[meritSlot], m.getRating(), filled);
            }
            meritSlot++;
            if (meritSlot >= 9)
                break;
        }

        // 6. Core Trackers (Essence, Willpower, Limit)
        setDotRating(acroForm, "essdot", data.essenceProperty().get(), 1, filled);
        setDotRating(acroForm, "willdot", data.willpowerProperty().get(), 1, filled);
        setTrackRating(acroForm, "check", data.limitProperty().get(), 11, 10, filled);
        setWrappedText(acroForm, "limitt", data.limitTriggerProperty().get(), 4, 45, filled); // ~45 chars per line

        // 7. Weapons (8x7 grid)
        fillWeapons(data, acroForm, filled);

        // 8. Protection and Derived Stats (HD1-19)
        fillProtection(data, acroForm, filled);

        // 9. Specialized Abilities (Craft & Martial Arts)
        fillSpecializedAbilities(data, acroForm, filled);

        // 10. Health Section
        fillHealth(data, acroForm, filled);

        // 11. Intimacies
        fillIntimacies(data, acroForm, filled);

        // 12. Other Equipment
        fillOtherEquipment(data, acroForm, filled);

        // 13. Charms and Spells
        fillCharms(data, acroForm, filled);

        // Abilities (Physical mapping to the 26 standard rows Ab1-26)
        String[][] abilityDots = {
                { "dot30", "dot31", "dot32", "dot32a", "dot32az" }, // Row 1: Archery
                { "dot38", "dot39", "dot40", "dot40a", "dot40az" }, // Row 2: Athletics
                { "dot46", "dot47", "dot48", "dot48a", "dot48az" }, // Row 3: Awareness
                { "dot49", "dot50", "dot51", "dot52", "dot53" }, // Row 4: Brawl
                { "dot57", "dot58", "dot59", "dot60", "dot61" }, // Row 5: Bureaucracy
                { "dot65", "dot66", "dot67", "dot68", "dot69" }, // Row 6: CRAFT (Specialized)
                { "dot54", "dot55", "dot56", "dot56a", "dot56az" }, // Row 7: Dodge
                { "dot62", "dot63", "dot64", "dot64a", "dot64az" }, // Row 8: Integrity
                { "dot70", "dot71", "dot72", "dot72a", "dot72az" }, // Row 9: Investigation
                { "dot73", "dot74", "dot75", "dot76", "dot77" }, // Row 10: Larceny
                { "dot81", "dot82", "dot83", "dot84", "dot85" }, // Row 11: Linguistics
                { "dot89", "dot90", "dot91", "dot92", "dot93" }, // Row 12: Lore
                { "dot97", "dot98", "dot99", "dot100", "dot101" }, // Row 13: MARTIAL ARTS (Specialized)
                { "dot105", "dot106", "dot107", "dot108", "dot109" }, // Row 14: Medicine
                { "dot113", "dot114", "dot115", "dot116", "dot117" }, // Row 15: Melee
                { "dot121", "dot122", "dot123", "dot124", "dot125" }, // Row 16: Occult
                { "dot129", "dot130", "dot131", "dot132", "dot133" }, // Row 17: Performance
                { "dot137", "dot138", "dot139", "dot140", "dot141" }, // Row 18: Presence
                { "dot145", "dot146", "dot147", "dot148", "dot149" }, // Row 19: Resistance
                { "dot145q", "dot146q", "dot147q", "dot148q", "dot149q" }, // Row 20: Ride
                { "dot78", "dot79", "dot80", "dot80a", "dot80az" }, // Row 21: Sail
                { "dot86", "dot87", "dot88", "dot88a", "dot88az" }, // Row 22: Socialize
                { "dot94", "dot95", "dot96", "dot96a", "dot96az" }, // Row 23: Stealth
                { "dot102", "dot103", "dot104", "dot104a", "dot104az" }, // Row 24: Survival
                { "dot110", "dot111", "dot112", "dot112a", "dot112az" }, // Row 25: Thrown
                { "dot118", "dot119", "dot120", "dot120a", "dot120az" } // Row 26: War
        };

        Ability[] abilityList = Ability.values();
        int rowIndex = 0;
        for (Ability ability : abilityList) {
            if (ability == Ability.CRAFT || ability == Ability.MARTIAL_ARTS) {
                rowIndex++;
                continue;
            }

            if (rowIndex < abilityDots.length) {
                int rating = data.getAbilityRating(ability);
                setTrackRating(acroForm, abilityDots[rowIndex], rating, filled);
                setCheckbox(acroForm, "abcheck" + (rowIndex + 1),
                        data.getCasteAbility(ability).get() || data.getFavoredAbility(ability).get(), filled);
            }
            rowIndex++;
        }
    }

    private void fillSpecializedAbilities(CharacterData data, PDAcroForm form, Set<String> filled) throws IOException {
        List<CraftAbility> crafts = data.getCrafts();
        List<MartialArtsStyle> styles = data.getMartialArtsStyles();

        // 1. Principal Slots
        // Row 6: Main Craft
        if (!crafts.isEmpty()) {
            CraftAbility main = crafts.get(0);
            setField(form, "ablities6", main.getExpertise(), filled);
            String[] mainDots = { "dot65", "dot66", "dot67", "dot68", "dot69" };
            setTrackRating(form, mainDots, main.getRating(), filled);
            setCheckbox(form, "abcheck6", main.isCaste() || main.isFavored(), filled);
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
            setField(form, "ablities13", name, filled);
            String[] styleDots = { "dot97", "dot98", "dot99", "dot100", "dot101" };
            setTrackRating(form, styleDots, mainStyle.getRating(), filled);
            setCheckbox(form, "abcheck13", mainStyle.isCaste() || mainStyle.isFavored(), filled);
        }

        // 2. Additional Slots (verified starting at Ab27 through Ab32)
        String[] addAbLabels = { "ablities27", "ablities28", "ablities29", "ablities30", "ablities31", "ablities32" };
        String[][] addAbDots = {
                { "dot126", "dot127", "dot128", "dot128a", "dot128az" }, // Ab27
                { "dot134", "dot135", "dot136", "dot136a", "dot136az" }, // Ab28
                { "dot142", "dot143", "dot144", "dot144a", "dot144az" }, // Ab29
                { "dot150", "dot151", "dot152", "dot152a", "dot152az" }, // Ab30
                { "dot150q", "dot151q", "dot152q", "dot152qa", "dot152qaz" }, // Ab31
                { "dot153", "dot154", "dot155", "dot156", "dot157" } // Ab32
        };

        int addSlotIdx = 0;

        // Surplus Crafts
        for (int i = 1; i < crafts.size() && addSlotIdx < addAbLabels.length; i++) {
            CraftAbility extra = crafts.get(i);
            setField(form, addAbLabels[addSlotIdx], "Craft: " + extra.getExpertise(), filled);
            setTrackRating(form, addAbDots[addSlotIdx], extra.getRating(), filled);
            setCheckbox(form, "abcheck" + (27 + addSlotIdx), extra.isCaste() || extra.isFavored(), filled);
            addSlotIdx++;
        }

        // Martial Arts Styles
        for (int i = 1; i < styles.size() && addSlotIdx < addAbLabels.length; i++) {
            MartialArtsStyle extraStyle = styles.get(i);
            setField(form, addAbLabels[addSlotIdx], "MA: " + extraStyle.getStyleName(), filled);
            setTrackRating(form, addAbDots[addSlotIdx], extraStyle.getRating(), filled);
            setCheckbox(form, "abcheck" + (27 + addSlotIdx), extraStyle.isCaste() || extraStyle.isFavored(), filled);
            addSlotIdx++;
        }
    }

    private void fillProtection(CharacterData data, PDAcroForm form, Set<String> filled) throws IOException {
        // 1. Armor Slots (HD1-HD10)
        List<Armor> armors = data.getArmors();
        for (int i = 0; i < Math.min(2, armors.size()); i++) {
            Armor a = armors.get(i);
            int nameIdx = i + 1; // HD1, HD2
            int soakIdx = i + 3; // HD3, HD4
            int hardnessIdx = i + 5; // HD5, HD6
            int mobilityIdx = i + 7; // HD7, HD8
            int tagsIdx = i + 9; // HD9, HD10

            setField(form, "HD" + nameIdx, a.getName(), filled);
            setField(form, "HD" + soakIdx, String.valueOf(a.getSoak()), filled);
            setField(form, "HD" + hardnessIdx, String.valueOf(a.getHardness()), filled);
            setField(form, "HD" + mobilityIdx, String.valueOf(a.getMobilityPenalty()), filled);
            setField(form, "HD" + tagsIdx, String.join(", ", a.getTags()), filled);
        }

        // 2. Derived Stats (HD15-19)
        setField(form, "HD15", String.valueOf(data.evasionProperty().get()), filled);
        setField(form, "HD16", String.valueOf(data.guileProperty().get()), filled);

        int dex = data.getAttribute(Attribute.DEXTERITY).get();
        setField(form, "HD17", String.valueOf(dex + data.getAbilityRating(Ability.ATHLETICS)), filled);
        setField(form, "HD18", String.valueOf(dex + data.getAbilityRating(Ability.DODGE)), filled);
        setField(form, "HD19", String.valueOf(data.joinBattleProperty().get()), filled);

        // 3. Final Protection Mapping
        int stamina = data.getAttribute(Attribute.STAMINA).get();
        setField(form, "HD11", String.valueOf(stamina), filled);
        setField(form, "HD12", String.valueOf(data.totalSoakProperty().get()), filled);
        setField(form, "HD13", "", filled); // Parry - Leave empty
        setField(form, "HD14", String.valueOf(data.resolveProperty().get()), filled);

        // Mote Pools (Below Essence Dots)
        setField(form, "essence1", "", filled); // Leave current blank for player use
        setField(form, "essence2", String.valueOf(data.getPersonalMotes()), filled);

        setField(form, "essence3", "", filled); // Leave current blank for player use
        setField(form, "essence4", String.valueOf(data.getPeripheralMotes()), filled);
    }

    private void fillWeapons(CharacterData data, PDAcroForm form, Set<String> filled) throws IOException {
        int rowCursor = 1;
        for (AttackPoolData apd : data.getAttackPools()) {
            Weapon w = apd.getWeapon();
            boolean isRanged = w.getRange() != Weapon.WeaponRange.CLOSE;
            List<String> tags = w.getTags();
            int rowsNeeded = isRanged ? 3 : (tags.size() > 2 ? 2 : 1);

            if (rowCursor + rowsNeeded - 1 > 8)
                break;

            // --- Row 1 ---
            setField(form, "weapons" + rowCursor, w.getName(), filled);

            // Accuracy Row 1
            if (!isRanged) {
                setField(form, "weapons" + (rowCursor + 8), String.valueOf(w.getAccuracy()), filled);
            } else {
                setField(form, "weapons" + (rowCursor + 8),
                        String.format("%d/%d", w.getCloseRangeBonus(), w.getShortRangeBonus()), filled);
            }

            // Damage (always Row 1)
            boolean isBashing = tags.contains("Bashing");
            setField(form, "weapons" + (rowCursor + 16), apd.getDamage() + (isBashing ? "B" : "L"), filled);

            // Defense Row 1
            setField(form, "weapons" + (rowCursor + 24), String.valueOf(w.getDefense()), filled);

            // Overwhelming Row 1
            setField(form, "weapons" + (rowCursor + 32), String.valueOf(w.getOverwhelming()), filled);

            // Tags Row 1
            setField(form, "weapons" + (rowCursor + 40), String.join(", ", tags.subList(0, Math.min(2, tags.size()))), filled);

            // Dice Pool Row 1 (Decisive only)
            setField(form, "weapons" + (rowCursor + 48), String.valueOf(apd.getDecisivePool()), filled);

            // --- Row 2 ---
            if (rowsNeeded >= 2) {
                int row2 = rowCursor + 1;
                if (isRanged) {
                    setField(form, "weapons" + (row2 + 8),
                            String.format("%d/%d", w.getMediumRangeBonus(), w.getLongRangeBonus()), filled);
                }
                if (tags.size() > 2) {
                    setField(form, "weapons" + (row2 + 40),
                            String.join(", ", tags.subList(2, Math.min(4, tags.size()))), filled);
                }
            }

            // --- Row 3 ---
            if (rowsNeeded == 3) {
                int row3 = rowCursor + 2;
                setField(form, "weapons" + (row3 + 8), String.valueOf(w.getExtremeRangeBonus()), filled);
                if (tags.size() > 4) {
                    setField(form, "weapons" + (row3 + 40), String.join(", ", tags.subList(4, tags.size())), filled);
                }
            }

            rowCursor += rowsNeeded;
        }
    }

    private void fillHealth(CharacterData data, PDAcroForm form, Set<String> filled) throws IOException {
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
                setField(form, "hl" + i, label, filled);
            }
        }
        // hbox fields are left empty as per user request
    }

    private void fillIntimacies(CharacterData data, PDAcroForm form, Set<String> filled) throws IOException {
        List<Intimacy> items = data.getIntimacies();
        for (int i = 0; i < Math.min(20, items.size()); i++) {
            Intimacy intimacy = items.get(i);
            int nameIdx, intensityIdx;
            if (i < 10) {
                nameIdx = i + 1;
                intensityIdx = i + 11;
            } else {
                // i >= 10, so i=10 -> nameIdx=21, intensityIdx=31
                nameIdx = i + 11;
                intensityIdx = i + 21;
            }

            String typeStr = (intimacy.getType() == Intimacy.Type.PRINCIPLE) ? "Principle" : "Tie";
            String title = typeStr + ": " + intimacy.getName();
            setField(form, "intimacies" + nameIdx, title, filled);
            
            // Format intensity with normal casing (e.g. Major)
            String intensityStr = intimacy.getIntensity().toString().toLowerCase();
            intensityStr = intensityStr.substring(0, 1).toUpperCase() + intensityStr.substring(1);
            setField(form, "intimacies" + intensityIdx, intensityStr, filled);
        }
    }

    private void fillOtherEquipment(CharacterData data, PDAcroForm form, Set<String> filled) throws IOException {
        int i = 1;
        for (OtherEquipment equip : data.getOtherEquipment()) {
            if (i > 10) break;
            String text = equip.getName() + (equip.isArtifact() ? " (Artifact)" : "");
            setField(form, "notes" + i, text, filled);
            i++;
        }
    }

    private void fillCharms(CharacterData data, PDAcroForm form, Set<String> filled) throws IOException {
        List<PurchasedCharm> charms = data.getUnlockedCharms();
        List<Spell> spells = data.getSpells();
        
        int slot = 1;
        // Combine charms and spells
        for (PurchasedCharm pc : charms) {
            if (slot > 26) break;
            Charm metadata = charmDataService.findCharmMetadata(pc);
            if (metadata != null) {
                fillCharmRow(form, slot, metadata.getName(), metadata.getCategory(), 
                             metadata.getDuration(), metadata.getCost(), metadata.getSource(), filled);
                slot++;
            }
        }
        
        for (Spell s : spells) {
            if (slot > 26) break;
            fillCharmRow(form, slot, s.getName(), "spell", s.getDuration(), s.getCost(), "Core", filled);
            slot++;
        }
    }

    private void fillCharmRow(PDAcroForm form, int slot, String name, String category, 
                              String duration, String cost, String book, Set<String> filled) throws IOException {
        // Name: 1-26
        setField(form, "charms" + slot, name, filled);
        
        // Type: 34-59
        String typeStr = category;
        if ("martialArts".equalsIgnoreCase(category)) typeStr = "MA";
        setField(form, "charms" + (slot + 33), typeStr, filled);
        
        // Duration: 67-92
        setField(form, "charms" + (slot + 66), duration, filled);
        
        // Cost: 100-125
        setField(form, "charms" + (slot + 99), cost, filled);
        
        // Book: 133-158
        setField(form, "charms" + (slot + 132), book, filled);
        
        // Page: 167-192 (Leave blank/Clear debug)
        setField(form, "charms" + (slot + 166), "", filled);
        
        // Effect: 201-226 (Leave blank/Clear debug)
        setField(form, "charms" + (slot + 200), "", filled);
    }

    private void setTrackRating(PDAcroForm form, String prefix, int rating, int startIndex, int count, Set<String> filled)
            throws IOException {
        for (int i = 0; i < count; i++) {
            setCheckbox(form, prefix + (startIndex + i), i < rating, filled);
        }
    }

    private void setTrackRating(PDAcroForm form, String[] dots, int rating, Set<String> filled) throws IOException {
        for (int i = 0; i < dots.length; i++) {
            setCheckbox(form, dots[i], i < rating, filled);
        }
    }

    private void setWrappedText(PDAcroForm form, String prefix, String text, int maxLines, int charsPerLine, Set<String> filled)
            throws IOException {
        if (text == null)
            return;

        // Simple word wrap
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        int lineIndex = 1;

        for (String word : words) {
            if (lineIndex > maxLines)
                break;

            if (currentLine.length() + word.length() + 1 > charsPerLine) {
                setField(form, prefix + lineIndex, currentLine.toString().trim(), filled);
                currentLine = new StringBuilder(word).append(" ");
                lineIndex++;
            } else {
                currentLine.append(word).append(" ");
            }
        }

        if (lineIndex <= maxLines && currentLine.length() > 0) {
            setField(form, prefix + lineIndex, currentLine.toString().trim(), filled);
        }
    }

    private void setIrregularDotRating(PDAcroForm form, String[] fieldNames, int rating, Set<String> filled) throws IOException {
        for (int i = 0; i < fieldNames.length; i++) {
            PDField field = form.getField(fieldNames[i]);
            if (field != null) {
                field.setValue(i < rating ? "Yes" : "Off");
                if (filled != null) filled.add(fieldNames[i]);
            }
        }
    }

    private void setField(PDAcroForm acroForm, String fieldName, String value, Set<String> filled) throws IOException {
        PDField field = acroForm.getField(fieldName);
        if (field != null) {
            field.setValue(value != null ? value : "");
            if (filled != null) filled.add(fieldName);
        }
    }

    private void setCheckbox(PDAcroForm acroForm, String fieldName, boolean value, Set<String> filled) throws IOException {
        PDField field = acroForm.getField(fieldName);
        if (field instanceof PDCheckBox cb) {
            if (value)
                cb.check();
            else
                cb.unCheck();
            if (filled != null) filled.add(fieldName);
        }
    }

    private void setDotRating(PDAcroForm form, String baseName, int rating, int startIndex, Set<String> filled) throws IOException {
        for (int i = 0; i < 5; i++) {
            String fieldName = baseName + (startIndex + i);
            PDField field = form.getField(fieldName);
            if (field != null) {
                field.setValue(i < rating ? "Yes" : "Off");
                if (filled != null) filled.add(fieldName);
            }
        }
    }
}
