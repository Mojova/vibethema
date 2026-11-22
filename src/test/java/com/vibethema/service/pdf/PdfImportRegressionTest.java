package com.vibethema.service.pdf;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vibethema.model.ExaltType;
import com.vibethema.model.equipment.Armor;
import com.vibethema.model.equipment.Weapon;
import com.vibethema.model.mystic.Charm;
import com.vibethema.model.mystic.CharmKind;
import com.vibethema.service.CharmDataService;
import com.vibethema.service.CharmJson;
import com.vibethema.service.DefinitionId;
import com.vibethema.service.EquipmentDataService;
import com.vibethema.service.ImportContext;
import com.vibethema.service.PdfExtractor;
import com.vibethema.service.SourceManifest;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdfImportRegressionTest {
    private static final Logger logger = LoggerFactory.getLogger(PdfImportRegressionTest.class);
    private static final Path DATA_SOURCE = Paths.get("data_source");
    private static final Path CORE_PDF = DATA_SOURCE.resolve("core_book.pdf");
    private static final String SOURCE_ID = "exalted-3e-core";
    private static final Path REFERENCE_DIR =
            DATA_SOURCE
                    .resolve("reference")
                    .resolve(SOURCE_ID)
                    .resolve(com.vibethema.service.CharmCollection.VERSION);

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Test
    public void testCoreImportRegression(@TempDir Path tempDir) throws IOException {
        assumeTrue(Files.isRegularFile(CORE_PDF), () -> "Core PDF is unavailable at " + CORE_PDF);

        logger.info("Running PDF import regression test...");
        PdfExtractor extractor = new PdfExtractor();
        Path source = tempDir.resolve("official").resolve(SOURCE_ID);

        // Run full extraction to temp directory
        extractor.extractAll(
                CORE_PDF.toFile(),
                new ImportContext(SourceManifest.coreBook(), source),
                progress -> {
                    // Log progress occasionally
                    if (progress % 0.2 < 0.01)
                        logger.info("Extraction progress: {}%", (int) (progress * 100));
                });

        assertEyeOfTheUnconqueredSunPrerequisite(source);
        assertCharmDescriptionCleanup(source);
        assertEquipmentTags(source);
        assertWeaponTemplates(source);
        assertArmorTemplates(source);
        assertNamedArtifacts(source);
        assertEquipmentCatalogLoads(tempDir);
        var anima =
                JsonParser.parseString(Files.readString(source.resolve("anima/anima.json")))
                        .getAsJsonObject();
        assertEquals(SOURCE_ID, anima.get("sourceId").getAsString());
        assertEquals(7, anima.getAsJsonArray("animaAbilities").size());
        assertEquals(
                4,
                anima.getAsJsonArray("animaAbilities")
                        .get(1)
                        .getAsJsonObject()
                        .getAsJsonArray("levels")
                        .size());
        assertTrue(
                charmDescription(source, "martial_arts/snake-style", "Essence Venom Strike")
                        .startsWith("Distilling her killing intent"));
        assertFalse(
                charmDescription(source, "martial_arts/snake-style", "Essence Venom Strike")
                        .contains("Armor:"));
        assertNotNull(
                charmDescription(
                        source,
                        "martial_arts/silver-voiced-nightingale-style",
                        "Shattering Discord Cacophony"));
        assertNotNull(
                charmDescription(
                        source,
                        "martial_arts/dreaming-pearl-courtesan-style",
                        "Invoking the Chimera’s Coils"));
        assertTrue(
                charmDescription(source, "resistance", "Diamond-Body Prana")
                        .contains("(Stamina + Resistance)"));
        assertTrue(
                charmDescription(source, "resistance", "Spirit Strengthens the Skin")
                        .contains("withering-only magic"));
        String keywordText = Files.readString(source.resolve("charms/keywords.json"));
        assertFalse(keywordText.contains("pre- requisites"));
        assertFalse(keywordText.contains("non-Integri- ty"));

        // Exact snapshots are versioned independently of earlier, incompatible output layouts.
        // Always run the semantic extraction checks above, even without current-format snapshots.
        if (!Files.isDirectory(REFERENCE_DIR)) {
            logger.warn(
                    "Current-format reference snapshots are unavailable at {}; semantic import"
                            + " checks passed, exact snapshot comparison was not run.",
                    REFERENCE_DIR);
            return;
        }

        // Verify all files in reference directory
        try (Stream<Path> stream = Files.walk(REFERENCE_DIR)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(refPath -> {
                        Path relative = REFERENCE_DIR.relativize(refPath);
                        Path actualPath = source.resolve(relative);

                        assertTrue(
                                Files.exists(actualPath),
                                "Expected file missing in output: " + relative);

                        logger.info("Comparing: {}", relative);
                        assertJsonEquals(refPath, actualPath);
                    });
        }

        // Semantic assertions also cover data without a local exact snapshot.
        try (Stream<Path> stream = Files.walk(source)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(actualPath -> {
                        Path relative = source.relativize(actualPath);
                        Path refPath = REFERENCE_DIR.resolve(relative);
                        if (!Files.exists(refPath)) {
                            logger.warn("No local exact snapshot for {}", relative);
                        }
                    });
        }
    }

    private void assertEquipmentCatalogLoads(Path dataDirectory) {
        String property = "vibethema.data.dir";
        String previousDirectory = System.getProperty(property);
        System.setProperty(property, dataDirectory.toString());
        try {
            var equipment = new EquipmentDataService();
            var weapons = equipment.getGlobalWeapons();
            var armors = equipment.getGlobalArmors();
            assertEquals(76, weapons.size());
            assertEquals(19, armors.size());
            assertNotNull(equipment.loadWeapon(Weapon.UNARMED_ID));
            assertEquals(
                    43, equipment.loadEquipmentTags().values().stream().mapToInt(List::size).sum());

            var equipmentIds = new HashSet<String>();
            weapons.forEach(
                    weapon -> assertTrue(equipmentIds.add(weapon.getId()), weapon.getName()));
            armors.forEach(armor -> assertTrue(equipmentIds.add(armor.getId()), armor.getName()));

            var charms = new CharmDataService();
            var artifactIds = charms.getArtifactIdsWithEvocationCollections();
            assertEquals(10, artifactIds.size());
            assertTrue(equipmentIds.containsAll(artifactIds));
            int evocationCount = 0;
            for (String artifactId : artifactIds) {
                var collection = charms.loadEvocations(artifactId);
                assertFalse(collection.evocations.isEmpty(), collection.artifactName);
                evocationCount += collection.evocations.size();
                for (Charm evocation : collection.evocations) {
                    var prerequisiteIds = new HashSet<String>();
                    evocation.getPrerequisites().collectCharmIds(prerequisiteIds);
                    for (String id : prerequisiteIds) {
                        assertNotNull(
                                charms.findCharmById(id),
                                evocation.getName() + " prerequisite " + id);
                    }
                }
            }
            assertEquals(42, evocationCount);
        } finally {
            if (previousDirectory == null) System.clearProperty(property);
            else System.setProperty(property, previousDirectory);
        }
    }

    private void assertWeaponTemplates(Path source) throws IOException {
        var weapons = new java.util.HashMap<String, Weapon.WeaponData>();
        var counts = new java.util.HashMap<String, Integer>();
        try (var files = Files.list(source.resolve("equipment/weapons"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                var weapon = gson.fromJson(Files.readString(file), Weapon.WeaponData.class);
                assertNull(weapons.put(weapon.name, weapon), weapon.name);
                assertEquals(weapon.id + ".json", file.getFileName().toString());
                assertNull(weapon.instanceId, weapon.name);
                if (!Weapon.UNARMED_ID.equals(weapon.id)) {
                    assertEquals(SOURCE_ID, weapon.sourceId);
                    assertEquals(
                            DefinitionId.forEquipment(SOURCE_ID, "weapons", weapon.name),
                            weapon.id);
                    assertFalse(weapon.description.isBlank(), weapon.name);
                }
                counts.merge(weapon.type.name() + "/" + weapon.range.name(), 1, Integer::sum);
            }
        }
        assertEquals(
                Map.of(
                        "MORTAL/CLOSE",
                        28,
                        "MORTAL/THROWN",
                        7,
                        "MORTAL/ARCHERY",
                        7,
                        "ARTIFACT/CLOSE",
                        23,
                        "ARTIFACT/THROWN",
                        5,
                        "ARTIFACT/ARCHERY",
                        6),
                counts);
        assertEquals(Weapon.UNARMED_ID, weapons.get("Unarmed").id);
        assertEquals(Weapon.WeaponCategory.HEAVY, weapons.get("Great Sword").category);
        assertTrue(weapons.get("Great Sword").description.endsWith("Cost: ••"));
        assertEquals(Weapon.WeaponRange.THROWN, weapons.get("Shuriken").range);
        assertTrue(weapons.get("Shuriken").tags.contains("Poisonable"));
        assertTrue(weapons.get("Flame Piece").tags.contains("One-Handed"));
        assertEquals(Weapon.WeaponCategory.MEDIUM, weapons.get("Powerbow").category);
        assertEquals(Weapon.WeaponType.ARTIFACT, weapons.get("Slayer Khatar").type);
        assertTrue(weapons.get("Direlance").tags.contains("Two-Handed when on foot"));
    }

    private void assertArmorTemplates(Path source) throws IOException {
        var armors = new java.util.HashMap<String, Armor.ArmorData>();
        var counts = new java.util.HashMap<String, Integer>();
        try (var files = Files.list(source.resolve("equipment/armors"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                var armor = gson.fromJson(Files.readString(file), Armor.ArmorData.class);
                assertNull(armors.put(armor.type.name() + "/" + armor.name, armor), armor.name);
                assertEquals(armor.id + ".json", file.getFileName().toString());
                assertEquals(SOURCE_ID, armor.sourceId);
                assertNull(armor.instanceId, armor.name);
                assertFalse(armor.description.isBlank(), armor.name);
                assertFalse(armor.tags.contains("None"), armor.name);
                counts.merge(armor.type.name() + "/" + armor.weight.name(), 1, Integer::sum);
            }
        }
        assertEquals(
                Map.of(
                        "MORTAL/LIGHT",
                        3,
                        "MORTAL/MEDIUM",
                        4,
                        "MORTAL/HEAVY",
                        2,
                        "ARTIFACT/LIGHT",
                        4,
                        "ARTIFACT/MEDIUM",
                        3,
                        "ARTIFACT/HEAVY",
                        3),
                counts);

        var mundane = armors.get("MORTAL/Reinforced Breastplate");
        var artifact = armors.get("ARTIFACT/Reinforced Breastplate");
        assertNotEquals(mundane.id, artifact.id);
        assertEquals(Armor.ArmorWeight.MEDIUM, mundane.weight);
        assertEquals(Armor.ArmorWeight.HEAVY, artifact.weight);
        assertEquals(List.of("Concealable"), armors.get("MORTAL/Chain Shirt").tags);
        assertEquals(List.of("Silent", "Special"), armors.get("ARTIFACT/Silken Armor").tags);
        assertTrue(armors.get("MORTAL/Buff Jacket").description.endsWith("Cost: •"));
        assertTrue(armors.get("ARTIFACT/Articulated Plate").description.endsWith("divine patron."));
    }

    private void assertNamedArtifacts(Path source) throws IOException {
        Map<String, Integer> counts =
                Map.of(
                        "Beloved Adorei",
                        5,
                        "Brilliant Sentinel",
                        3,
                        "Moonlit Huntress",
                        1,
                        "Shining Ice Mirror",
                        4,
                        "Spring Razor",
                        5,
                        "Freedom’s Cadence",
                        5,
                        "Orichalcum Hunting Hawk",
                        5,
                        "Dauntless",
                        4,
                        "Black Wind",
                        5,
                        "Volcano Cutter",
                        5);
        String monkeyLeapId =
                DefinitionId.forCharm(
                        SOURCE_ID,
                        CharmKind.EXALTED,
                        ExaltType.SOLAR,
                        "Monkey Leap Technique",
                        "Athletics");
        var actual = new java.util.HashMap<String, Integer>();
        var codec = new CharmJson();
        try (var files = Files.list(source.resolve("charms/evocations"))) {
            for (Path path : files.toList()) {
                var tree = codec.read(path);
                assertEquals(CharmKind.EVOCATION, tree.kind);
                assertEquals(SOURCE_ID, tree.sourceId);
                actual.put(tree.artifactName, tree.charms.size());
                Path weapon = source.resolve("equipment/weapons").resolve(path.getFileName());
                Path armor = source.resolve("equipment/armors").resolve(path.getFileName());
                assertNotEquals(Files.exists(weapon), Files.exists(armor));
                var equipment =
                        JsonParser.parseString(
                                        Files.readString(Files.exists(weapon) ? weapon : armor))
                                .getAsJsonObject();
                assertEquals(tree.artifactId, equipment.get("id").getAsString());
                assertEquals(tree.artifactName, equipment.get("name").getAsString());
                assertEquals("ARTIFACT", equipment.get("type").getAsString());
                var ids = new java.util.HashSet<String>();
                tree.charms.forEach(charm -> assertTrue(ids.add(charm.getId())));
                for (var charm : tree.charms) {
                    assertFalse(charm.getFullText().isBlank());
                    assertFalse(charm.getFullText().contains("THE QUEST TO RESURRECT"));
                    if (charm.getName().equals("Dawn of a Hundred Rebukes")) {
                        String[] paragraphs = charm.getFullText().split("\n\n");
                        assertEquals(4, paragraphs.length);
                        assertTrue(paragraphs[1].startsWith("If the gambit is successful"));
                        assertTrue(paragraphs[2].startsWith("This effect only lasts"));
                        assertTrue(paragraphs[3].startsWith("Using this Charm"));
                    }
                    if (charm.getName().equals("Life-Drinking Resurgence")) {
                        assertTrue(charm.getFullText().contains("creates two blood blades"));
                    }
                    var references = new java.util.HashSet<String>();
                    charm.getPrerequisites().collectCharmIds(references);
                    if (charm.getName().equals("Orichalcum Wings Elevation")) {
                        assertEquals(java.util.Set.of(monkeyLeapId), references);
                    } else {
                        assertTrue(ids.containsAll(references), charm.getName());
                    }
                }
            }
        }
        assertEquals(counts, actual);
    }

    private void assertEquipmentTags(Path source) throws IOException {
        var tags =
                JsonParser.parseString(
                                Files.readString(source.resolve("equipment/equipment_tags.json")))
                        .getAsJsonObject();
        assertTagNames(
                tags,
                "melee",
                "Balanced, Bashing, Brawl, Chopping, Disarming, Flexible, Improvised, Grappling,"
                        + " Lethal, Martial Arts, Melee, Natural, Piercing, Reaching, Shield,"
                        + " Smashing, Thrown, Two-Handed, Worn");
        assertTagNames(
                tags,
                "thrown",
                "Bashing, Concealable, Cutting, Lethal, Mounted, Poisonable, Special, Subtle,"
                        + " Thrown");
        assertTagNames(
                tags,
                "archery",
                "Archery, Bashing, Concealable, Crossbow, Flame, Lethal, Mounted, One-Handed,"
                        + " Piercing, Powerful, Slow, Special");
        assertTagNames(tags, "armor", "Buoyant, Concealable, Silent");

        var melee = tags.getAsJsonArray("melee");
        String chopping = melee.get(3).getAsJsonObject().get("description").getAsString();
        assertTrue(chopping.contains("Defense until the attacker’s next turn"));
        assertTrue(chopping.endsWith("such as axes and chopping swords."));
        String worn = melee.get(18).getAsJsonObject().get("description").getAsString();
        assertTrue(worn.endsWith("weapon with the worn tag takes an action."));
    }

    private void assertTagNames(JsonObject tags, String category, String expectedNames) {
        assertEquals(
                List.of(expectedNames.split(", ")),
                tags.getAsJsonArray(category).asList().stream()
                        .map(tag -> tag.getAsJsonObject().get("name").getAsString())
                        .toList(),
                category);
    }

    private void assertCharmDescriptionCleanup(Path importedSource) throws IOException {
        String whirlingBrush =
                charmDescription(importedSource, "linguistics", "Whirling Brush Method");
        assertFalse(whirlingBrush.contains("WRITTEN VS. SPOKEN EFFECTS"));
        assertTrue(whirlingBrush.contains("perfectly transcribe a rapid conversation"));

        assertTrue(
                charmDescription(importedSource, "stealth", "Perfect Shadow Stillness")
                        .contains("Stealth-based action"));
        assertTrue(
                charmDescription(importedSource, "performance", "Impassioned Orator Technique")
                        .contains("Performance-based speech"));
        assertTrue(
                charmDescription(importedSource, "survival", "Eye-Deceiving Camouflage")
                        .contains("Awareness-based attempts"));
        assertTrue(
                charmDescription(importedSource, "socialize", "Face-Charming Prana")
                        .contains("Socialize-based persuade action"));
        assertTrue(
                charmDescription(importedSource, "presence", "Blazing Glorious Icon")
                        .contains("Presence-based attempts"));
        assertTrue(
                charmDescription(importedSource, "larceny", "Perfect Mirror")
                        .contains("Essence-based visual dynamics"));

        String flowingHand =
                charmDescription(importedSource, "linguistics", "Flowing Elegant Hand");
        assertFalse(flowingHand.contains("ARTISTIC TALENT: LINGUISTICS OR CRAFT"));
        assertTrue(flowingHand.contains("At Linguistics 5+"));

        String singleVoice = charmDescription(importedSource, "linguistics", "Single Voice Kata");
        assertFalse(singleVoice.contains("THAT BURNING QUESTION"));
        assertTrue(singleVoice.contains("no intelligible words, letters or mechanics"));

        String divineInspiration =
                charmDescription(importedSource, "craft", "Divine Inspiration Technique");
        assertFalse(divineInspiration.startsWith("Supreme Masterwork Focus"));

        String investigation =
                charmDescription(importedSource, "investigation", "Inquisitor’s Unfailing Notice");
        assertFalse(investigation.contains("but those powers aren’t necessarily called out"));
        assertFalse(investigation.contains("Divine Mantle exists"));

        String melee =
                charmDescription(importedSource, "melee", "Ready in Eight Directions Stance");
        assertFalse(melee.contains("Fivefold Bulwark Stance ignores"));
        assertTrue(melee.contains("counter an opponent in the first round"));

        String performance =
                charmDescription(importedSource, "performance", "Infectious Zealotry Approach");
        assertFalse(performance.contains("mentarily blanks out"));

        String war = charmDescription(importedSource, "war", "Battle-Visionary’s Foresight");
        assertFalse(war.contains("The neighborhood had once been an affluent district"));
        assertTrue(war.contains("two prepared tactics to counter one of these reflexively"));

        String powerAwarding = charmDescription(importedSource, "lore", "Power-Awarding Prana");
        assertFalse(powerAwarding.contains("FATE OF THE MIDDLEMARCHES"));

        String stageFright =
                charmDescription(importedSource, "performance", "Stillness-Drawing Meditation");
        assertFalse(stageFright.contains("DON’T GET STAGE FRIGHT"));

        String redRule = charmDescription(importedSource, "presence", "Crowned King of Eternity");
        assertFalse(redRule.contains("REMEMBER THE RED RULE"));

        String mariner = charmDescription(importedSource, "sail", "Immortal Mariner’s Advantage");
        assertFalse(mariner.contains("EXAMPLE HAZARD"));
        assertTrue(mariner.contains("decisive marine gambits"));

        String heartEclipsing =
                charmDescription(importedSource, "socialize", "Heart-Eclipsing Shroud");
        assertFalse(heartEclipsing.contains("SOCIETAL INFLUENCE AND GROUP DYNAMICS"));
        assertTrue(heartEclipsing.contains("Special rules:"));

        String legendMask =
                charmDescription(importedSource, "socialize", "Legend Mask Methodology");
        assertFalse(legendMask.contains("PERSONAS AND LIMIT BREAK"));

        String stalkingWolf = charmDescription(importedSource, "stealth", "Stalking Wolf Attitude");
        assertFalse(stalkingWolf.contains("PERSONAS: RULES AND LIMITATIONS"));

        String shadowReplacement =
                charmDescription(importedSource, "stealth", "Shadow Replacement Technique");
        assertFalse(
                shadowReplacement.contains("crown by vanishing from the minds of her subjects"));
        assertTrue(shadowReplacement.contains("requires the Lawgiver be in control of a grapple"));

        String camouflage =
                charmDescription(importedSource, "survival", "Eye-Deceiving Camouflage");
        assertFalse(camouflage.contains("A FAMILIAR INVESTMENT"));
        assertTrue(camouflage.contains("These successes directly counter"));

        String mistOnWater = charmDescription(importedSource, "thrown", "Mist on Water Attack");
        assertFalse(mistOnWater.contains("ELECTED TARGETS"));
        assertTrue(mistOnWater.contains("they may die standing up"));

        String destiny =
                charmDescription(importedSource, "integrity", "Destiny-Manifesting Method");
        assertEquals(1, occurrences(destiny, "which she derived this Charm."));
        assertFalse(destiny.contains("This means, literally"));

        String measurement = charmDescription(importedSource, "bureaucracy", "Measuring Glance");
        assertTrue(measurement.contains("([Social or Mental Attribute] + Bureaucracy)"));

        String bodyRestoring =
                charmDescription(importedSource, "integrity", "Body-Restoring Benison");
        assertFalse(bodyRestoring.contains("use this Divine Mantle"));
        String divineMantle = charmDescription(importedSource, "integrity", "Divine Mantle");
        assertTrue(divineMantle.contains("If a spirit recognizes the Lawgiver as king"));
        assertTrue(divineMantle.contains("use this Divine Mantle to gain the powers"));

        String repeatedFury = charmDescription(importedSource, "brawl", "Rampage-Berserker Attack");
        assertTrue(repeatedFury.contains("but can be reset if"));
        assertFalse(repeatedFury.contains("but if can be reset if"));

        String firstMovement = charmDescription(importedSource, "brawl", "Fivefold Fury Onslaught");
        assertTrue(firstMovement.contains("each successful strike"));

        String dragonSoul = charmDescription(importedSource, "integrity", "Undying Solar Resolve");
        assertTrue(dragonSoul.contains("the initiator’s roll"));

        String oxStunning = charmDescription(importedSource, "brawl", "Ox-Stunning Blow");
        assertTrue(oxStunning.contains("for every two successes"));

        String forceRending = charmDescription(importedSource, "brawl", "Force-Rending Strike");
        assertTrue(forceRending.contains("Brawl-based"));
        String rampage = charmDescription(importedSource, "brawl", "Rampage-Berserker Attack");
        assertTrue(rampage.contains("Brawl-based withering attack"));
        assertFalse(rampage.contains("Brawlbased"));

        assertFalse(whirlingBrush.contains("\u0007"));
        assertAbilityBasedTermsAreHyphenated(importedSource);
    }

    private void assertAbilityBasedTermsAreHyphenated(Path importedSource) throws IOException {
        java.util.regex.Pattern unhyphenatedAbilityBased =
                java.util.regex.Pattern.compile(
                        "(?i)\\b(?:Archery|Athletics|Awareness|Brawl|Bureaucracy|Craft|Dodge|"
                                + "Integrity|Investigation|Larceny|Linguistics|Lore|Medicine|"
                                + "Melee|Occult|Performance|Presence|Resistance|Ride|Sail|"
                                + "Socialize|Stealth|Survival|Thrown|War|Essence)based\\b");
        try (Stream<Path> charmFiles = Files.walk(importedSource.resolve("charms"))) {
            for (Path charmFile : charmFiles.filter(p -> p.toString().endsWith(".json")).toList()) {
                JsonElement file = JsonParser.parseString(Files.readString(charmFile));
                if (!file.isJsonObject() || !file.getAsJsonObject().has("charms")) continue;
                for (JsonElement charm : file.getAsJsonObject().getAsJsonArray("charms")) {
                    String description = charm.getAsJsonObject().get("fullText").getAsString();
                    assertFalse(
                            unhyphenatedAbilityBased.matcher(description).find(),
                            () -> "Found an unhyphenated Ability-based term in " + charmFile);
                }
            }
        }
    }

    private String charmDescription(Path importedSource, String ability, String charmName)
            throws IOException {
        Path charmFile =
                importedSource
                        .resolve("charms")
                        .resolve(
                                ability.startsWith("martial_arts/")
                                        ? ability + ".json"
                                        : "solar/" + ability + ".json");
        JsonElement charms = JsonParser.parseString(Files.readString(charmFile));
        return charms.getAsJsonObject().getAsJsonArray("charms").asList().stream()
                .filter(charm ->
                        charmName.equals(charm.getAsJsonObject().get("name").getAsString()))
                .map(charm -> charm.getAsJsonObject().get("fullText").getAsString())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing charm: " + charmName));
    }

    private int occurrences(String text, String target) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(target, offset)) >= 0) {
            count++;
            offset += target.length();
        }
        return count;
    }

    private void assertEyeOfTheUnconqueredSunPrerequisite(Path importedSource) throws IOException {
        Path awarenessFile = importedSource.resolve("charms/solar/awareness.json");
        JsonElement awareness = JsonParser.parseString(Files.readString(awarenessFile));
        JsonElement eye =
                awareness.getAsJsonObject().getAsJsonArray("charms").asList().stream()
                        .filter(charm ->
                                "Eye of the Unconquered Sun"
                                        .equals(charm.getAsJsonObject().get("name").getAsString()))
                        .findFirst()
                        .orElseThrow();
        JsonElement prerequisites = eye.getAsJsonObject().get("prerequisites");
        assertFalse(eye.getAsJsonObject().has("importWarnings"));
        assertTrue(hasCharmPrerequisite(prerequisites, "Awakening Eye"));
        JsonElement countRule = findRule(prerequisites, "countFromAbilities");
        assertNotNull(countRule, "The non-Excellency Awareness count should be structured");
        assertEquals(3, countRule.getAsJsonObject().get("count").getAsInt());
        assertEquals(
                "Awareness",
                countRule.getAsJsonObject().getAsJsonArray("abilities").get(0).getAsString());
        assertTrue(countRule.getAsJsonObject().get("excludeExcellencies").getAsBoolean());
        java.util.Set<String> excludedIds = new java.util.HashSet<>();
        countRule
                .getAsJsonObject()
                .getAsJsonArray("excludedCharmIds")
                .forEach(id -> excludedIds.add(id.getAsString()));
        assertEquals(java.util.Set.of(charmId("Awakening Eye")), excludedIds);

        java.util.Set<java.util.Set<String>> prerequisitePaths = new java.util.HashSet<>();
        countRule
                .getAsJsonObject()
                .getAsJsonArray("excludedPrerequisitePaths")
                .forEach(path -> {
                    java.util.Set<String> ids = new java.util.HashSet<>();
                    path.getAsJsonArray().forEach(id -> ids.add(id.getAsString()));
                    prerequisitePaths.add(ids);
                });
        String sensoryAcuity = charmId("Sensory Acuity Prana");
        assertEquals(
                java.util.Set.of(
                        java.util.Set.of(
                                sensoryAcuity,
                                charmId("Keen Sight Technique"),
                                charmId("Keen Hearing and Touch Technique")),
                        java.util.Set.of(
                                sensoryAcuity,
                                charmId("Keen Sight Technique"),
                                charmId("Keen Taste and Smell Technique")),
                        java.util.Set.of(
                                sensoryAcuity,
                                charmId("Keen Hearing and Touch Technique"),
                                charmId("Keen Taste and Smell Technique"))),
                prerequisitePaths,
                "Each valid two-of-three Awakening Eye path should be an alternative exclusion");
    }

    private String charmId(String name) {
        return com.vibethema.service.DefinitionId.forCharm(
                SOURCE_ID,
                com.vibethema.model.mystic.CharmKind.EXALTED,
                com.vibethema.model.ExaltType.SOLAR,
                name,
                "Awareness");
    }

    private boolean hasCharmPrerequisite(JsonElement rule, String charmName) {
        if (rule == null || !rule.isJsonObject()) return false;
        JsonElement charmId = rule.getAsJsonObject().get("charmId");
        if ("charm".equals(rule.getAsJsonObject().get("type").getAsString())
                && charmId != null
                && charmId.getAsString()
                        .equals(
                                com.vibethema.service.DefinitionId.forCharm(
                                        SOURCE_ID,
                                        com.vibethema.model.mystic.CharmKind.EXALTED,
                                        com.vibethema.model.ExaltType.SOLAR,
                                        charmName,
                                        "Awareness"))) return true;
        return hasRuleInChildren(rule, "requirements", charmName)
                || hasRuleInChildren(rule, "options", charmName);
    }

    private boolean hasRuleInChildren(JsonElement rule, String key, String charmName) {
        JsonElement children = rule.getAsJsonObject().get(key);
        if (children == null || !children.isJsonArray()) return false;
        for (JsonElement child : children.getAsJsonArray()) {
            if (hasCharmPrerequisite(child, charmName)) return true;
        }
        return false;
    }

    private JsonElement findRule(JsonElement rule, String type) {
        if (rule == null || !rule.isJsonObject()) return null;
        JsonElement ruleType = rule.getAsJsonObject().get("type");
        if (ruleType != null && type.equals(ruleType.getAsString())) return rule;
        for (String key : new String[] {"requirements", "options"}) {
            JsonElement children = rule.getAsJsonObject().get(key);
            if (children != null && children.isJsonArray()) {
                for (JsonElement child : children.getAsJsonArray()) {
                    JsonElement found = findRule(child, type);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    private void assertJsonEquals(Path expected, Path actual) {
        try (Reader refReader = Files.newBufferedReader(expected, StandardCharsets.UTF_8);
                Reader actReader = Files.newBufferedReader(actual, StandardCharsets.UTF_8)) {

            JsonElement refJson = JsonParser.parseReader(refReader);
            JsonElement actJson = JsonParser.parseReader(actReader);

            if (expected.getFileName().toString().equals("manifest.json")) {
                refJson.getAsJsonObject().remove("importedAt");
                actJson.getAsJsonObject().remove("importedAt");
            }

            // Using Gson's built-in equality which is content-based and formatting-agnostic
            if (!refJson.equals(actJson)) {
                // If they don't match, print them for debugging (trimmed)
                String expectedStr = gson.toJson(refJson);
                String actualStr = gson.toJson(actJson);
                assertEquals(
                        expectedStr, actualStr, "Content mismatch in " + expected.getFileName());
            }
        } catch (IOException e) {
            fail("Failed to read JSON files for comparison: " + e.getMessage());
        }
    }
}
