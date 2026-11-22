package com.vibethema.service.pdf.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vibethema.model.equipment.Armor;
import com.vibethema.model.equipment.Weapon;
import com.vibethema.service.DefinitionId;
import com.vibethema.service.EquipmentDataService;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoreEquipmentExtractor {
    private static final Pattern PAGE_FURNITURE =
            Pattern.compile(
                    "(?m)^\\h*(?:C H A P T E R\\h+9|T H E\\h+G R A N D\\h+P A N O P L ?Y|"
                            + "E\\h*X\\h*3|[0-9]{3})\\h*\\R");
    private static final String TAG_LABEL = "^\\h*([A-Z][A-Za-z -]*):";
    private static final Pattern TAG_ENTRY =
            Pattern.compile("(?ms)" + TAG_LABEL + "\\h*(.*?)(?=" + TAG_LABEL + "|\\z)");
    private static final Pattern WEAPON_WEIGHT =
            Pattern.compile("(?m)^(Light|Medium|Heavy) (Melee|Thrown|Archery) Weapons\\h*$");
    private static final Pattern ARMOR_WEIGHT =
            Pattern.compile("(?m)^(?:Artifact )?(Light|Medium|Heavy) Armor\\h*$");
    private static final Pattern ITEM_NAME =
            Pattern.compile("[A-Z][A-Za-z-]*(?:[ /](?:[A-Z][A-Za-z-]*|and|of|the))*");
    // The artifact melee traits table appears between two light weapon entries in PDF text order.
    private static final Pattern ARTIFACT_MELEE_TABLE =
            Pattern.compile(
                    "(?m)^Artifact Melee Weapons\\h*\\RCategory\\h+Accuracy[^\\r\\n]*\\R"
                        + "Light\\h+[^\\r\\n]*\\RMedium\\h+[^\\r\\n]*\\RHeavy\\h+[^\\r\\n]*\\R");

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Path overrideOutputPath;
    private String sourceId = "exalted-3e-core";

    private record EquipmentEntry(String name, String description, List<String> tags) {}

    public void setOverrideOutputPath(Path path) {
        this.overrideOutputPath = path;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public void extractAndSaveTags(String text) throws IOException {
        // A tag description can continue after a running header on the next page.
        String content = PAGE_FURNITURE.matcher(text).replaceAll("");
        Map<String, List<Map<String, String>>> output = new LinkedHashMap<>();
        output.put("melee", parseTags(content, "melee", null, "Dual Wielding"));
        output.put("thrown", parseTags(content, "thrown", "Thrown Weapons", "Archery Weapons"));
        output.put(
                "archery", parseTags(content, "archery", "Archery Weapons", "Special Materials"));
        output.put("armor", parseTags(content, "armor", "Mundane Armor", "Artifacts"));

        Path filePath =
                overrideOutputPath != null
                        ? overrideOutputPath.resolve("equipment/equipment_tags.json")
                        : EquipmentDataService.getEquipmentTagsPath();
        Files.createDirectories(filePath.getParent());

        try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            gson.toJson(output, writer);
        }
    }

    public void extractAndSaveWeapons(String text) throws IOException {
        String content = PAGE_FURNITURE.matcher(text).replaceAll("").replace("\r\n", "\n");
        content = ARTIFACT_MELEE_TABLE.matcher(content).replaceAll("");
        Map<String, Weapon.WeaponData> weapons = new LinkedHashMap<>();
        parseWeaponSection(content, "Melee Weapons", "Tags", Weapon.WeaponType.MORTAL, weapons);
        parseWeaponSection(content, "Thrown Weapons", "Tags", Weapon.WeaponType.MORTAL, weapons);
        parseWeaponSection(
                content,
                "Archery Weapons",
                "Fowling Arrows and Bolts",
                Weapon.WeaponType.MORTAL,
                weapons);
        parseWeaponSection(
                content,
                "Artifact Melee Weapons",
                "Artifact Thrown Weapons",
                Weapon.WeaponType.ARTIFACT,
                weapons);
        parseWeaponSection(
                content,
                "Artifact Thrown Weapons",
                "Artifact Archery Weapons",
                Weapon.WeaponType.ARTIFACT,
                weapons);
        parseWeaponSection(
                content,
                "Artifact Archery Weapons",
                "Artifact Armor",
                Weapon.WeaponType.ARTIFACT,
                weapons);

        Path output =
                overrideOutputPath == null
                        ? EquipmentDataService.getWeaponsPath()
                        : overrideOutputPath.resolve("equipment/weapons");
        Files.createDirectories(output);
        for (Weapon.WeaponData weapon : weapons.values()) {
            // The importer writes Unarmed with the permanent ID used by new characters.
            if (!weapon.name.equals("Unarmed")) {
                Files.writeString(output.resolve(weapon.id + ".json"), gson.toJson(weapon));
            }
        }
    }

    private void parseWeaponSection(
            String content,
            String heading,
            String endHeading,
            Weapon.WeaponType type,
            Map<String, Weapon.WeaponData> weapons)
            throws IOException {
        int start = findHeading(content, heading, 0).end();
        int end = findHeading(content, endHeading, start).start();
        String section = content.substring(start, end);
        Matcher groups = WEAPON_WEIGHT.matcher(section);
        if (!groups.find()) throw new IOException("Missing weapon categories for " + heading);
        boolean hasGroup = true;
        while (hasGroup) {
            Weapon.WeaponCategory category =
                    Weapon.WeaponCategory.valueOf(groups.group(1).toUpperCase(Locale.ROOT));
            Weapon.WeaponRange range =
                    switch (groups.group(2)) {
                        case "Thrown" -> Weapon.WeaponRange.THROWN;
                        case "Archery" -> Weapon.WeaponRange.ARCHERY;
                        default -> Weapon.WeaponRange.CLOSE;
                    };
            int entriesStart = groups.end();
            hasGroup = groups.find();
            String entries =
                    section.substring(entriesStart, hasGroup ? groups.start() : section.length());
            for (EquipmentEntry entry : parseEntries(entries)) {
                var weapon = new Weapon.WeaponData();
                weapon.id = DefinitionId.forEquipment(sourceId, "weapons", entry.name());
                weapon.sourceId = sourceId;
                weapon.name = entry.name();
                weapon.description = entry.description();
                weapon.type = type;
                weapon.range = range;
                weapon.category = category;
                weapon.tags = entry.tags();
                if (weapons.putIfAbsent(weapon.id, weapon) != null) {
                    throw new IOException("Duplicate Core weapon template: " + entry.name());
                }
            }
        }
    }

    public void extractAndSaveArmor(String text) throws IOException {
        String content = PAGE_FURNITURE.matcher(text).replaceAll("").replace("\r\n", "\n");
        Map<String, Armor.ArmorData> armors = new LinkedHashMap<>();
        parseArmorSection(content, "Mundane Armor", "Tags", Armor.ArmorType.MORTAL, armors);
        parseArmorSection(
                content,
                "Artifact Armor",
                "Additional Artifacts",
                Armor.ArmorType.ARTIFACT,
                armors);

        Path output =
                overrideOutputPath == null
                        ? EquipmentDataService.getArmorsPath()
                        : overrideOutputPath.resolve("equipment/armors");
        Files.createDirectories(output);
        for (Armor.ArmorData armor : armors.values()) {
            Files.writeString(output.resolve(armor.id + ".json"), gson.toJson(armor));
        }
    }

    private void parseArmorSection(
            String text,
            String heading,
            String endHeading,
            Armor.ArmorType type,
            Map<String, Armor.ArmorData> armors)
            throws IOException {
        int start = findHeading(text, heading, 0).end();
        int end = findHeading(text, endHeading, start).start();
        String section = text.substring(start, end);
        Matcher groups = ARMOR_WEIGHT.matcher(section);
        if (!groups.find()) throw new IOException("Missing armor weights for " + heading);
        boolean hasGroup = true;
        while (hasGroup) {
            Armor.ArmorWeight weight =
                    Armor.ArmorWeight.valueOf(groups.group(1).toUpperCase(Locale.ROOT));
            int entriesStart = groups.end();
            hasGroup = groups.find();
            String entries =
                    section.substring(entriesStart, hasGroup ? groups.start() : section.length());
            for (EquipmentEntry entry : parseEntries(entries)) {
                var armor = new Armor.ArmorData();
                // The book lists both mundane and artifact versions of several armor names.
                armor.id =
                        DefinitionId.from(
                                sourceId, "equipment", "armors", type.name(), entry.name());
                armor.sourceId = sourceId;
                armor.name = entry.name();
                armor.description = entry.description();
                armor.type = type;
                armor.weight = weight;
                armor.tags = entry.tags();
                if (armors.putIfAbsent(armor.id, armor) != null) {
                    throw new IOException("Duplicate Core armor template: " + entry.name());
                }
            }
        }
    }

    private static List<EquipmentEntry> parseEntries(String block) throws IOException {
        List<String> lines =
                block.lines().map(String::strip).filter(line -> !line.isEmpty()).toList();
        List<EquipmentEntry> entries = new ArrayList<>();
        int index = 0;
        while (index < lines.size() && !ITEM_NAME.matcher(lines.get(index)).matches()) index++;
        while (index < lines.size()) {
            String name = lines.get(index++);
            if (!ITEM_NAME.matcher(name).matches()) {
                throw new IOException("Unexpected equipment heading: " + name);
            }
            int descriptionStart = index;
            while (index < lines.size() && !lines.get(index).startsWith("Tags:")) index++;
            if (index == lines.size() || index == descriptionStart) {
                throw new IOException("Missing description or tags for equipment: " + name);
            }
            String description = String.join("\n", lines.subList(descriptionStart, index));
            String tags = lines.get(index++).substring("Tags:".length()).strip();
            while (index < lines.size()
                    && (tags.endsWith(",")
                            || tags.endsWith("-")
                            || lines.get(index).startsWith("(")
                            || lines.get(index).equals("when on foot"))) {
                tags += (tags.endsWith("-") ? "" : " ") + lines.get(index++);
            }

            List<String> tagList =
                    tags.equals("None")
                            ? List.of()
                            : Arrays.stream(tags.split("[,;]")).map(String::strip).toList();
            entries.add(
                    new EquipmentEntry(
                            name,
                            description.replaceAll("-\\h*\\n\\h*", "-").replaceAll("\\s+", " "),
                            tagList));
        }
        return entries;
    }

    private List<Map<String, String>> parseTags(
            String text, String category, String startHeading, String endHeading)
            throws IOException {
        int start = startHeading == null ? 0 : findHeading(text, startHeading, 0).end();
        int end = findHeading(text, endHeading, start).start();
        String section = text.substring(start, end);
        int tagsStart = findHeading(section, "Tags", 0).end();
        Matcher matcher = TAG_ENTRY.matcher(section.substring(tagsStart));
        List<Map<String, String>> list = new ArrayList<>();
        while (matcher.find()) {
            String name = matcher.group(1).trim();
            String desc =
                    matcher.group(2).replaceAll("-\\h*\\R\\h*", "-").replaceAll("\\s+", " ").trim();

            Map<String, String> tag = new LinkedHashMap<>();
            String id = DefinitionId.forEquipment(sourceId, category, name);
            tag.put("id", id);
            tag.put("sourceId", sourceId);
            tag.put("exaltTypes", "SOLAR");
            tag.put("name", name);
            tag.put("description", desc);
            list.add(tag);
        }
        if (list.isEmpty()) throw new IOException("No Core equipment tags found for " + category);
        return list;
    }

    private static Matcher findHeading(String text, String heading, int start) throws IOException {
        Matcher matcher =
                Pattern.compile("(?m)^\\h*" + Pattern.quote(heading) + "\\h*$").matcher(text);
        if (!matcher.find(start)) {
            throw new IOException("Missing Core equipment heading: " + heading);
        }
        return matcher;
    }
}
