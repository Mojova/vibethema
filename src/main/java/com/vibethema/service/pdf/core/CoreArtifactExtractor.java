package com.vibethema.service.pdf.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vibethema.model.equipment.Armor;
import com.vibethema.model.equipment.Weapon;
import com.vibethema.model.mystic.CharmKind;
import com.vibethema.model.mystic.Evocation;
import com.vibethema.model.mystic.PrerequisiteRule;
import com.vibethema.service.CharmCollection;
import com.vibethema.service.CharmJson;
import com.vibethema.service.DefinitionId;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Extracts the named artifact examples in the Core Book's Grand Panoply chapter. */
public class CoreArtifactExtractor {
    // PDFBox starts a new paragraph on every page, including pages that continue a sentence.
    private static final Pattern PAGE_FURNITURE =
            Pattern.compile(
                    "(?m)^(?:\\h*\\n)*(?:C H A P T E R\\h+9|T H E\\h+G R A N D\\h+P A N O P L"
                            + " ?Y)\\h*\\n(?:\\h*\\n)*EX3\\h*\\n[0-9]{3}\\h*\\n(?:\\h*\\n)*");
    private static final Pattern ARTIFACT =
            Pattern.compile("(?m)^([^\\n]+)\\n\\(([^,\\n]+),\\h*Artifact (•+)\\)\\h*$");
    private static final Pattern EVOCATION =
            Pattern.compile(
                    "(?m)^([^:\\n]+)\\n(?:\\h*\\n)*Cost:\\h*(.+?);\\h*Mins:\\h*Essence\\h+(\\d+)(.*)$");
    private static final Pattern METADATA =
            Pattern.compile(
                    "(?m)^(Type|Keywords|Duration|Prerequisites|Prerequisite"
                            + " Charms):\\h*([^\\n]*)$");
    private static final Pattern SIDEBAR =
            Pattern.compile("(?ms)^THE QUEST TO RESURRECT BELOVED ADOREI\\h*\\n.*?(?=^Keywords:)");
    // A column break can produce the same marker in the middle of a sentence.
    private static final Pattern DESCRIPTION_PARAGRAPH =
            Pattern.compile("(?<=[.!?:;…’”)])\\h*\\n(?:\\h*\\n)+");

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final CharmJson charmJson = new CharmJson();

    private record Artifact(String category, Object equipment, CharmCollection tree) {}

    private record Node(Evocation evocation, String metadataAndDescription) {}

    public void extractAndSave(String text, String sourceId, Path output) throws IOException {
        String content = text.replace("\r\n", "\n");
        content = PAGE_FURNITURE.matcher(content).replaceAll("");
        content = SIDEBAR.matcher(content).replaceAll("");
        Map<String, String> solarCharms = solarCharmIds(output.resolve("charms/solar"));
        List<Artifact> artifacts = new ArrayList<>();
        Matcher heading = ARTIFACT.matcher(content);
        boolean found = heading.find();
        while (found) {
            String name = heading.group(1).strip();
            String form = clean(heading.group(2));
            int rating = heading.group(3).length();
            int start = heading.end();
            found = heading.find();
            String body = content.substring(start, found ? heading.start() : content.length());
            artifacts.add(parseArtifact(sourceId, name, form, rating, body, solarCharms));
        }
        if (artifacts.size() != 10) throw new IOException("Expected ten named Core artifacts");

        for (Artifact artifact : artifacts) {
            String id = artifact.tree().artifactId;
            Path equipment =
                    output.resolve("equipment").resolve(artifact.category()).resolve(id + ".json");
            Files.createDirectories(equipment.getParent());
            Files.writeString(equipment, gson.toJson(artifact.equipment()));
            charmJson.write(
                    output.resolve("charms/evocations").resolve(id + ".json"), artifact.tree());
        }
    }

    private Artifact parseArtifact(
            String sourceId,
            String name,
            String form,
            int rating,
            String body,
            Map<String, String> solarCharms)
            throws IOException {
        var matches = EVOCATION.matcher(body);
        if (!matches.find()) throw new IOException("Missing Evocations for " + name);
        int firstNode = matches.start();
        String description =
                form
                        + ", Artifact "
                        + rating
                        + "\n\n"
                        + clean(
                                body.substring(0, firstNode)
                                        .replaceAll("(?m)^Evocations of [^\\n]+$", ""));
        boolean armor =
                form.endsWith("Heavy Plate")
                        || form.endsWith("Lamellar Armor")
                        || form.endsWith("Chain Shirt");
        String category = armor ? "armors" : "weapons";
        String id = DefinitionId.forEquipment(sourceId, category, name);
        var tree = new CharmCollection();
        tree.kind = CharmKind.EVOCATION;
        tree.artifactId = id;
        tree.artifactName = name;
        tree.sourceId = sourceId;
        Map<String, String> ids = new LinkedHashMap<>(solarCharms);
        List<Node> nodes = new ArrayList<>();
        boolean found = true;
        while (found) {
            var evocation = new Evocation();
            evocation.setName(clean(matches.group(1)));
            evocation.setSourceId(sourceId);
            evocation.setArtifactId(id);
            evocation.setId(
                    DefinitionId.forCharm(
                            sourceId, CharmKind.EVOCATION, null, evocation.getName(), id));
            evocation.setCost(clean(matches.group(2)));
            evocation.setMinEssence(Integer.parseInt(matches.group(3)));
            String inline = matches.group(4);
            int start = matches.end();
            found = matches.find();
            nodes.add(
                    new Node(
                            evocation,
                            inline
                                    + body.substring(
                                            start, found ? matches.start() : body.length())));
            ids.put(normalize(evocation.getName()), evocation.getId());
        }
        for (Node node : nodes) {
            parseNode(node, ids);
            tree.charms.add(node.evocation());
        }
        return new Artifact(
                category,
                armor
                        ? armor(id, sourceId, name, form, description)
                        : weapon(id, sourceId, name, form, description),
                tree);
    }

    private void parseNode(Node node, Map<String, String> ids) throws IOException {
        String text =
                node.metadataAndDescription()
                        .replaceAll(";\\h*(?=(?:Type|Keywords|Duration):)", "\n");
        var matcher = METADATA.matcher(text);
        Evocation evocation = node.evocation();
        while (matcher.find()) {
            String value = clean(matcher.group(2));
            switch (matcher.group(1)) {
                case "Type" -> evocation.setType(value);
                case "Duration" -> evocation.setDuration(value);
                case "Keywords" ->
                        evocation.setKeywords(
                                value.equalsIgnoreCase("none")
                                        ? List.of()
                                        : Arrays.stream(value.split(","))
                                                .map(String::strip)
                                                .toList());
                default -> {
                    var remaining =
                            new ArrayList<>(text.substring(matcher.end()).strip().lines().toList());
                    List<PrerequisiteRule> requirements = prerequisites(value, ids);
                    while (requirements == null && !remaining.isEmpty() && value.length() < 220) {
                        value = clean(value + "\n" + remaining.removeFirst());
                        requirements = prerequisites(value, ids);
                    }
                    if (requirements == null
                            || evocation.getType() == null
                            || evocation.getDuration() == null
                            || evocation.getKeywords() == null
                            || remaining.isEmpty()) {
                        throw new IOException("Incomplete Evocation: " + evocation.getName());
                    }
                    evocation.setPrerequisites(PrerequisiteRule.allOf(requirements));
                    evocation.setFullText(cleanParagraphs(String.join("\n", remaining)));
                    return;
                }
            }
        }
        throw new IOException("Missing prerequisites for " + evocation.getName());
    }

    private static String cleanParagraphs(String text) {
        return DESCRIPTION_PARAGRAPH
                .splitAsStream(text)
                .map(CoreArtifactExtractor::clean)
                .filter(paragraph -> !paragraph.isEmpty())
                .collect(Collectors.joining("\n\n"));
    }

    private static List<PrerequisiteRule> prerequisites(String names, Map<String, String> ids) {
        if (names.equalsIgnoreCase("none")) return List.of();
        var result = new ArrayList<PrerequisiteRule>();
        for (String name : names.split(",", -1)) {
            String id = ids.get(normalize(name));
            if (id == null) return null;
            result.add(PrerequisiteRule.charm(id));
        }
        return result;
    }

    private Map<String, String> solarCharmIds(Path directory) throws IOException {
        Map<String, String> ids = new LinkedHashMap<>();
        try (var files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                for (var charm : charmJson.read(file).charms) {
                    ids.put(normalize(charm.getName()), charm.getId());
                }
            }
        }
        return ids;
    }

    private static Armor.ArmorData armor(
            String id, String source, String name, String form, String description) {
        var data = new Armor.ArmorData();
        data.id = id;
        data.sourceId = source;
        data.name = name;
        data.description = description;
        data.type = Armor.ArmorType.ARTIFACT;
        data.weight =
                form.endsWith("Heavy Plate")
                        ? Armor.ArmorWeight.HEAVY
                        : form.endsWith("Chain Shirt")
                                ? Armor.ArmorWeight.LIGHT
                                : Armor.ArmorWeight.MEDIUM;
        data.tags = data.weight == Armor.ArmorWeight.LIGHT ? List.of("Concealable") : List.of();
        return data;
    }

    private static Weapon.WeaponData weapon(
            String id, String source, String name, String form, String description)
            throws IOException {
        var data = new Weapon.WeaponData();
        data.id = id;
        data.sourceId = source;
        data.name = name;
        data.description = description;
        data.type = Weapon.WeaponType.ARTIFACT;
        data.range = Weapon.WeaponRange.CLOSE;
        data.category = Weapon.WeaponCategory.MEDIUM;
        if (form.endsWith("Powerbow")) {
            data.range = Weapon.WeaponRange.ARCHERY;
            data.tags = List.of("Lethal", "Archery (Long)");
        } else if (form.endsWith("Skycutter")) {
            data.range = Weapon.WeaponRange.THROWN;
            data.tags = List.of("Lethal", "Thrown (Long)", "Cutting", "Special");
        } else if (form.endsWith("Grand Daiklave")) {
            data.category = Weapon.WeaponCategory.HEAVY;
            data.tags = List.of("Lethal", "Melee", "Balanced", "Reaching", "Two-Handed");
        } else if (form.endsWith("Daiklave")) {
            data.tags = List.of("Lethal", "Melee", "Balanced");
        } else {
            throw new IOException("Unknown Core artifact weapon: " + form);
        }
        return data;
    }

    private static String clean(String text) {
        return text.replaceAll("-\\h*\\n\\h*", "-").replaceAll("(?U)\\s+", " ").strip();
    }

    private static String normalize(String name) {
        return clean(name).replace('’', '\'').toLowerCase(Locale.ROOT);
    }
}
