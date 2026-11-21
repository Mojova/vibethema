package com.vibethema.service;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.vibethema.model.*;
import com.vibethema.model.mystic.*;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CharmDataService {
    private static final Logger logger = LoggerFactory.getLogger(CharmDataService.class);
    private static final String CHARMS_DIR = "charms";
    private static final String MA_DIR = "martial_arts";
    private static final String EVOCATIONS_DIR = "evocations";
    private static final String SPELLS_DIR = "spells";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final CharmJson charmJson = new CharmJson();
    private Map<String, Charm> charmIndex;

    public static Path getUserCharmsPath() {
        return PathService.getCustomDataPath().resolve(CHARMS_DIR);
    }

    public static Path getUserExaltedCharmsPath(ExaltType type) {
        return getUserCharmsPath().resolve(type.name().toLowerCase(Locale.ROOT));
    }

    public static Path getUserMartialArtsPath() {
        return getUserCharmsPath().resolve(MA_DIR);
    }

    public static Path getUserEvocationsPath() {
        return getUserCharmsPath().resolve(EVOCATIONS_DIR);
    }

    public static Path getUserSpellsPath() {
        return PathService.getCustomDataPath().resolve(SPELLS_DIR);
    }

    private static List<Path> officialPaths(String... components) {
        return officialPaths(null, components);
    }

    private static List<Path> officialPaths(ExaltType exaltType, String... components) {
        List<Path> paths = new ArrayList<>();
        List<Path> sources =
                exaltType == null
                        ? PathService.getOfficialSourcePaths()
                        : new SourceCatalog()
                                .getSourcesFor(exaltType).stream()
                                        .map(s -> PathService.getOfficialSourcePath(s.sourceId))
                                        .toList();
        for (Path source : sources) {
            Path path = source;
            for (String component : components) path = path.resolve(component);
            paths.add(path);
        }
        return paths;
    }

    private static List<Path> collectionFiles(Path root) {
        if (!Files.isDirectory(root)) return List.of();
        try (var files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .filter(p ->
                            !Set.of("keywords.json", "charm-schema.json")
                                    .contains(p.getFileName().toString()))
                    .sorted()
                    .toList();
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not list Charm collections in " + root, ex);
        }
    }

    private List<Path> allCollectionFiles() {
        List<Path> files = new ArrayList<>();
        for (Path path : officialPaths(CHARMS_DIR))
            files.addAll(
                    collectionFiles(path).stream()
                            .filter(f -> !f.getParent().equals(path))
                            .toList());
        Path customRoot = getUserCharmsPath();
        files.addAll(
                collectionFiles(customRoot).stream()
                        .filter(f -> !f.getParent().equals(customRoot))
                        .toList());
        return files;
    }

    private JsonObject header(Path file) {
        try {
            return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        } catch (IOException | RuntimeException ex) {
            throw new JsonParseException("Could not read Charm collection " + file, ex);
        }
    }

    private CharmCollection readCollection(Path file) {
        try {
            CharmCollection collection = charmJson.read(file);
            boolean custom = file.startsWith(getUserCharmsPath());
            collection.charms.forEach(c -> c.setCustom(custom));
            return collection;
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not read Charm collection " + file, ex);
        }
    }

    public List<Charm> loadCharmsForAbility(String ability) {
        return loadCharmsForAbility(ability, ExaltType.SOLAR);
    }

    /** Native-family filtering uses collection metadata; shared disciplines have no Exalt owner. */
    public List<Charm> loadCharmsForAbility(String ability, ExaltType exaltType) {
        Map<String, Charm> merged = new LinkedHashMap<>();
        for (Path file : allCollectionFiles()) {
            JsonObject json = header(file);
            if (!json.has("ability")
                    || !ability.equalsIgnoreCase(json.get("ability").getAsString())) continue;
            if (json.has("kind")
                    && "exalted".equals(json.get("kind").getAsString())
                    && json.has("exaltType")) {
                ExaltType owner;
                try {
                    owner = ExaltType.valueOf(json.get("exaltType").getAsString());
                } catch (RuntimeException ex) {
                    throw new JsonParseException("Unsupported Charm Exalt type in " + file, ex);
                }
                if (exaltType != null && owner != exaltType) continue;
            }
            CharmCollection collection = readCollection(file);
            if (collection.kind == CharmKind.EVOCATION) continue;
            collection.charms.forEach(c -> merged.put(c.getId(), c));
        }
        return new ArrayList<>(merged.values());
    }

    public synchronized void invalidateIndex() {
        charmIndex = null;
    }

    private synchronized Map<String, Charm> index() {
        if (charmIndex == null) {
            Map<String, Charm> built = new LinkedHashMap<>();
            for (Path file : allCollectionFiles())
                readCollection(file).charms.forEach(c -> built.put(c.getId(), c));
            charmIndex = Map.copyOf(built);
        }
        return charmIndex;
    }

    public Charm findCharmById(String id) {
        return id == null ? null : index().get(id);
    }

    public String getCharmName(String id) {
        Charm charm = findCharmById(id);
        return charm == null ? id : charm.getName();
    }

    public String getCharmAbility(String id) {
        Charm charm = findCharmById(id);
        return charm == null ? null : charm.getAbility();
    }

    public int getGlobalCharmCount() {
        return index().size();
    }

    public static void exportSchema(Path charmsRoot) throws IOException {
        Files.createDirectories(charmsRoot);
        try (InputStream schema =
                CharmDataService.class.getResourceAsStream("/charms/charm-schema.json")) {
            if (schema == null) throw new IOException("Bundled Charm schema is missing.");
            Files.copy(
                    schema,
                    charmsRoot.resolve("charm-schema.json"),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void exportSchema() throws IOException {
        exportSchema(getUserCharmsPath());
    }

    private CharmCollection collectionFor(Charm charm) {
        CharmCollection collection = new CharmCollection();
        collection.sourceId = charm.getSourceId() == null ? "custom" : charm.getSourceId();
        collection.kind = charm.getKind();
        collection.exaltType = charm.getExaltType();
        collection.ability = charm.getAbility();
        return collection;
    }

    private Path customPath(Charm charm) {
        if (charm.getKind() == CharmKind.MARTIAL_ARTS) {
            Path existing =
                    findMartialArtsFileByAbility(
                            charm.getAbility(),
                            charm.getSourceId() == null ? "custom" : charm.getSourceId());
            return existing != null
                    ? existing
                    : getUserMartialArtsPath()
                            .resolve(
                                    DefinitionId.forMartialArtsStyle(
                                                    charm.getSourceId() == null
                                                            ? "custom"
                                                            : charm.getSourceId(),
                                                    charm.getAbility())
                                            + ".json");
        }
        if (charm.getKind() != CharmKind.EXALTED || charm.getExaltType() == null)
            throw new IllegalArgumentException(
                    "Use saveEvocation for Evocations; native Charms require an Exalt type.");
        return getUserExaltedCharmsPath(charm.getExaltType())
                .resolve(
                        charm.getAbility().toLowerCase(Locale.ROOT).replace(" ", "-")
                                + sourceSuffix(charm)
                                + "-custom.json");
    }

    private String sourceSuffix(Charm charm) {
        return charm.getSourceId() == null || "custom".equals(charm.getSourceId())
                ? ""
                : "-" + DefinitionId.from(charm.getSourceId(), "custom-charms");
    }

    private Path evocationPath(String artifactId, Charm charm) {
        return getUserEvocationsPath().resolve(artifactId + sourceSuffix(charm) + ".json");
    }

    public void saveCharm(Charm charm) throws IOException {
        Path path = customPath(charm);
        CharmCollection collection =
                Files.exists(path) ? readCollection(path) : collectionFor(charm);
        if (collection.kind == CharmKind.MARTIAL_ARTS && collection.id == null)
            collection.id = path.getFileName().toString().replace(".json", "");
        collection.charms.removeIf(c -> c.getId().equals(charm.getId()));
        collection.charms.add(charm);
        charmJson.write(path, collection);
        invalidateIndex();
        exportSchema();
    }

    public void deleteCustomCharm(Charm charm) throws IOException {
        Path path = customPath(charm);
        if (!Files.exists(path)) return;
        CharmCollection collection = readCollection(path);
        collection.charms.removeIf(c -> c.getId().equals(charm.getId()));
        charmJson.write(path, collection);
        invalidateIndex();
    }

    public static class EvocationCollection {
        public String artifactId;
        public String artifactName;
        public List<Charm> evocations;

        public EvocationCollection(String artifactId, String artifactName, List<Charm> charms) {
            this.artifactId = artifactId;
            this.artifactName = artifactName;
            this.evocations = charms;
        }
    }

    public EvocationCollection loadEvocations(String artifactId) {
        Map<String, Charm> merged = new LinkedHashMap<>();
        String name = "Artifact";
        for (Path file : allCollectionFiles()) {
            JsonObject json = header(file);
            if (!json.has("artifactId") || !artifactId.equals(json.get("artifactId").getAsString()))
                continue;
            CharmCollection collection = readCollection(file);
            if (collection.kind != CharmKind.EVOCATION)
                throw new JsonParseException("Expected Evocations in " + file);
            if (collection.artifactName != null) name = collection.artifactName;
            collection.charms.forEach(c -> merged.put(c.getId(), c));
        }
        return new EvocationCollection(artifactId, name, new ArrayList<>(merged.values()));
    }

    public void saveEvocation(String artifactId, String artifactName, Charm charm)
            throws IOException {
        if (charm.getKind() != CharmKind.EVOCATION)
            throw new IllegalArgumentException(
                    "Only Evocations can be saved in an artifact collection.");
        Path path = evocationPath(artifactId, charm);
        CharmCollection collection =
                Files.exists(path) ? readCollection(path) : collectionFor(charm);
        collection.artifactId = artifactId;
        collection.artifactName = artifactName;
        collection.ability = null;
        collection.charms.removeIf(c -> c.getId().equals(charm.getId()));
        collection.charms.add(charm);
        charmJson.write(path, collection);
        invalidateIndex();
        exportSchema();
    }

    public void updateEvocationCollectionName(String artifactId, String artifactName)
            throws IOException {
        for (Path path : collectionFiles(getUserEvocationsPath())) {
            CharmCollection collection = readCollection(path);
            if (!artifactId.equals(collection.artifactId)) continue;
            collection.artifactName = artifactName;
            charmJson.write(path, collection);
        }
        invalidateIndex();
    }

    public void deleteEvocation(String artifactId, Charm charm) throws IOException {
        Path path = evocationPath(artifactId, charm);
        if (!Files.exists(path)) return;
        CharmCollection collection = readCollection(path);
        collection.charms.removeIf(c -> c.getId().equals(charm.getId()));
        charmJson.write(path, collection);
        invalidateIndex();
    }

    public List<String> getAvailableMartialArtsStyles() {
        Set<String> styles = new TreeSet<>();
        for (Path file : allCollectionFiles()) {
            JsonObject json = header(file);
            if (json.has("kind") && "martialArts".equals(json.get("kind").getAsString()))
                styles.add(readCollection(file).ability);
        }
        return new ArrayList<>(styles);
    }

    public void createNewMartialArtsStyle(String id, String name) throws IOException {
        Path path = getUserMartialArtsPath().resolve(id + ".json");
        if (Files.exists(path)) {
            updateMartialArtsStyleName(id, name);
            return;
        }
        CharmCollection collection = new CharmCollection();
        collection.kind = CharmKind.MARTIAL_ARTS;
        collection.sourceId = "custom";
        collection.id = id;
        collection.ability = name;
        charmJson.write(path, collection);
        invalidateIndex();
        exportSchema();
    }

    public void updateMartialArtsStyleName(String id, String newName) throws IOException {
        Path path = getUserMartialArtsPath().resolve(id + ".json");
        if (!Files.exists(path)) return;
        CharmCollection collection = readCollection(path);
        collection.ability = newName;
        collection.charms.forEach(c -> c.setAbility(newName));
        charmJson.write(path, collection);
        invalidateIndex();
    }

    private Path findMartialArtsFileByAbility(String ability, String sourceId) {
        for (Path file : collectionFiles(getUserMartialArtsPath())) {
            CharmCollection collection = readCollection(file);
            if (collection.kind == CharmKind.MARTIAL_ARTS
                    && sourceId.equals(collection.sourceId)
                    && ability.equalsIgnoreCase(collection.ability)) return file;
        }
        return null;
    }

    private static class SpellListWrapper {
        public String version;
        public String circle;
        public List<CharacterSaveState.SpellData> spells;
    }

    public int getGlobalSpellCount() {
        int count = 0;
        List<Path> spellDirectories = new ArrayList<>(officialPaths(SPELLS_DIR));
        spellDirectories.add(getUserSpellsPath());
        try {
            for (Path spellDirectory : spellDirectories) {
                if (!Files.exists(spellDirectory)) continue;
                try (java.util.stream.Stream<Path> stream = Files.list(spellDirectory)) {
                    count =
                            count
                                    + (int)
                                            stream.filter(p -> p.toString().endsWith(".json"))
                                                    .map(p -> {
                                                        try (Reader r =
                                                                Files.newBufferedReader(
                                                                        p,
                                                                        StandardCharsets.UTF_8)) {
                                                            SpellListWrapper w =
                                                                    gson.fromJson(
                                                                            r,
                                                                            SpellListWrapper.class);
                                                            return w != null && w.spells != null
                                                                    ? w.spells.size()
                                                                    : 0;
                                                        } catch (Exception e) {
                                                            return 0;
                                                        }
                                                    })
                                                    .mapToInt(Integer::intValue)
                                                    .sum();
                }
            }
        } catch (IOException e) {
            logger.error("Error counting spells", e);
        }
        return count;
    }

    public List<Keyword> loadKeywords() {
        String filename = "keywords.json";
        List<Keyword> result = new ArrayList<>();
        for (Path sourcePath : officialPaths(CHARMS_DIR)) {
            loadKeywordsFromFile(sourcePath.resolve(filename), result);
        }
        loadKeywordsFromFile(getUserCharmsPath().resolve(filename), result);
        if (!result.isEmpty()) return result;

        Path userPath = getUserCharmsPath().resolve(filename);

        // Try user directory first
        if (Files.exists(userPath)) {
            try (Reader reader = Files.newBufferedReader(userPath, StandardCharsets.UTF_8)) {
                Type listType = new TypeToken<ArrayList<Keyword>>() {}.getType();
                return gson.fromJson(reader, listType);
            } catch (IOException e) {
                logger.error("Error loading user keyword file: {}", userPath, e);
            }
        }

        return new ArrayList<>();
    }

    private void loadKeywordsFromFile(Path path, List<Keyword> target) {
        if (!Files.exists(path)) return;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<ArrayList<Keyword>>() {}.getType();
            List<Keyword> keywords = gson.fromJson(reader, listType);
            if (keywords != null) target.addAll(keywords);
        } catch (IOException e) {
            logger.error("Error loading keyword file: {}", path, e);
        }
    }

    public List<Spell> loadSpells(String circle) {
        return loadSpells(circle, null);
    }

    /** Loads spells from sources supporting the selected Exalt type plus custom spells. */
    public List<Spell> loadSpells(String circle, ExaltType exaltType) {
        List<Spell> result = new ArrayList<>();

        String filename = circle.toLowerCase() + ".json";
        for (Path sourcePath : officialPaths(exaltType, SPELLS_DIR)) {
            loadSpellsFromFile(sourcePath.resolve(filename), result, false);
        }
        loadSpellsFromFile(getUserSpellsPath().resolve(filename), result, true);

        String customFilename = "custom-" + circle.toLowerCase() + ".json";
        loadSpellsFromFile(getUserSpellsPath().resolve(customFilename), result, true);

        java.util.LinkedHashMap<String, Spell> merged = new java.util.LinkedHashMap<>();
        for (Spell spell : result) {
            if (spell.getId() != null && !spell.getId().isBlank()) merged.put(spell.getId(), spell);
        }
        return new ArrayList<>(merged.values());
    }

    private void loadSpellsFromFile(Path path, List<Spell> result, boolean isCustom) {
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                SpellListWrapper wrapper = gson.fromJson(reader, SpellListWrapper.class);
                if (wrapper != null && wrapper.spells != null) {
                    for (CharacterSaveState.SpellData sd : wrapper.spells) {
                        Spell s =
                                new Spell(
                                        sd.id,
                                        sd.name,
                                        sd.circle,
                                        sd.cost,
                                        sd.keywords,
                                        sd.duration,
                                        sd.description);
                        s.setCustom(isCustom);
                        result.add(s);
                    }
                }
            } catch (IOException e) {
                logger.error("Error loading spells from: {}", path, e);
            }
        }
    }

    public void saveCustomSpell(Spell spell) throws IOException {
        String circle = spell.getCircle();
        String filename = "custom-" + circle.toLowerCase() + ".json";
        Path targetPath = getUserSpellsPath().resolve(filename);

        if (!Files.exists(targetPath.getParent())) {
            Files.createDirectories(targetPath.getParent());
        }

        // Load existing
        List<Spell> existing = loadSpells(circle);
        existing.removeIf(s -> !s.isCustom()); // We only want to save custom ones to this file

        // Replace or add
        boolean found = false;
        for (int i = 0; i < existing.size(); i++) {
            if (existing.get(i).getId().equals(spell.getId())) {
                existing.set(i, spell);
                found = true;
                break;
            }
        }
        if (!found) existing.add(spell);

        // Convert back to SpellData for serialization
        List<CharacterSaveState.SpellData> dataList = new ArrayList<>();
        for (Spell s : existing) {
            CharacterSaveState.SpellData sd = new CharacterSaveState.SpellData();
            sd.id = s.getId();
            sd.name = s.getName();
            sd.circle = s.getCircle();
            sd.cost = s.getCost();
            sd.keywords = new ArrayList<>(s.getKeywords());
            sd.duration = s.getDuration();
            sd.description = s.getDescription();
            dataList.add(sd);
        }

        SpellListWrapper wrapper = new SpellListWrapper();
        wrapper.version = "0.1.0";
        wrapper.circle = circle;
        wrapper.spells = dataList;

        Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = Files.newBufferedWriter(targetPath, StandardCharsets.UTF_8)) {
            prettyGson.toJson(wrapper, writer);
        }
    }

    public void deleteCustomSpell(Spell spell) throws IOException {
        String circle = spell.getCircle();
        String filename = "custom-" + circle.toLowerCase() + ".json";
        Path targetPath = getUserSpellsPath().resolve(filename);

        if (!Files.exists(targetPath)) return;

        List<Spell> existing = loadSpells(circle);
        existing.removeIf(s -> !s.isCustom());
        existing.removeIf(s -> s.getId().equals(spell.getId()));

        List<CharacterSaveState.SpellData> dataList = new ArrayList<>();
        for (Spell s : existing) {
            CharacterSaveState.SpellData sd = new CharacterSaveState.SpellData();
            sd.id = s.getId();
            sd.name = s.getName();
            sd.circle = s.getCircle();
            sd.cost = s.getCost();
            sd.keywords = new ArrayList<>(s.getKeywords());
            sd.duration = s.getDuration();
            sd.description = s.getDescription();
            dataList.add(sd);
        }

        SpellListWrapper wrapper = new SpellListWrapper();
        wrapper.version = "0.1.0";
        wrapper.circle = circle;
        wrapper.spells = dataList;

        Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = Files.newBufferedWriter(targetPath, StandardCharsets.UTF_8)) {
            prettyGson.toJson(wrapper, writer);
        }
    }

    public void saveCustomKeyword(String name) throws IOException {
        List<Keyword> keywords = loadKeywords();
        boolean exists = keywords.stream().anyMatch(k -> k.getName().equalsIgnoreCase(name));
        if (!exists) {
            Keyword nk = new Keyword();
            nk.setName(name);
            nk.setDescription("User-defined keyword.");
            keywords.add(nk);

            Path userPath = getUserCharmsPath().resolve("keywords.json");
            try (Writer writer = Files.newBufferedWriter(userPath, StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(keywords, writer);
            }
        }
    }
}
