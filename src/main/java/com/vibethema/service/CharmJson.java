package com.vibethema.service;

import com.google.gson.*;
import com.vibethema.model.ExaltType;
import com.vibethema.model.mystic.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Strict Charm format codec. Directories never supply missing classification metadata. */
public final class CharmJson {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public CharmCollection read(Path path) throws IOException {
        try {
            return read(JsonParser.parseString(Files.readString(path)).getAsJsonObject());
        } catch (JsonParseException | IllegalStateException | IllegalArgumentException ex) {
            throw new JsonParseException(
                    "Invalid Charm collection " + path + ": " + ex.getMessage(), ex);
        }
    }

    public CharmCollection read(JsonObject json) {
        rejectLegacyFields(json);
        var allowed =
                java.util.Set.of(
                        "$schema",
                        "version",
                        "sourceId",
                        "kind",
                        "exaltType",
                        "id",
                        "ability",
                        "artifactId",
                        "artifactName",
                        "charms");
        for (String field : json.keySet())
            if (!allowed.contains(field))
                throw error("Unsupported Charm collection field: " + field);
        if (!CharmCollection.VERSION.equals(required(json, "version")))
            throw error("Unsupported Charm format version; re-import the source book.");
        CharmCollection collection = new CharmCollection();
        collection.sourceId = required(json, "sourceId");
        collection.kind =
                switch (required(json, "kind")) {
                    case "exalted" -> CharmKind.EXALTED;
                    case "martialArts" -> CharmKind.MARTIAL_ARTS;
                    case "evocation" -> CharmKind.EVOCATION;
                    default -> throw error("Unsupported Charm kind: " + json.get("kind"));
                };
        collection.id = optional(json, "id");
        if (collection.kind == CharmKind.EXALTED) {
            try {
                collection.exaltType = ExaltType.valueOf(required(json, "exaltType"));
            } catch (IllegalArgumentException ex) {
                throw error("Unsupported Charm Exalt type: " + json.get("exaltType"));
            }
            if (collection.exaltType != ExaltType.SOLAR)
                throw error(
                        "Unsupported native Charm family: "
                                + collection.exaltType.name()
                                + ". Its rules are not implemented.");
        } else if (json.has("exaltType")) {
            throw error("Only native Exalted Charms may declare exaltType.");
        }
        if (collection.kind == CharmKind.EVOCATION) {
            if (json.has("ability")) throw error("Evocations use artifactId rather than ability.");
            collection.artifactId = required(json, "artifactId");
            collection.artifactName = optional(json, "artifactName");
        } else {
            if (json.has("artifactId") || json.has("artifactName"))
                throw error("Only Evocations may declare an artifact.");
            collection.ability = required(json, "ability");
        }
        if (!json.has("charms") || !json.get("charms").isJsonArray())
            throw error("A Charm collection requires a charms array.");
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (JsonElement element : json.getAsJsonArray("charms")) {
            JsonObject entry = element.getAsJsonObject();
            rejectLegacyFields(entry);
            for (String field : List.of("kind", "exaltType", "sourceId", "ability", "artifactId")) {
                if (entry.has(field))
                    throw error(
                            "Charm entries inherit "
                                    + field
                                    + " from the collection; remove the duplicate field.");
            }
            String id = required(entry, "id");
            required(entry, "name");
            if (!ids.add(id)) throw error("Duplicate Charm ID: " + id);
            Charm charm =
                    switch (collection.kind) {
                        case EXALTED -> gson.fromJson(entry, SolarCharm.class);
                        case MARTIAL_ARTS -> gson.fromJson(entry, MartialArtsCharm.class);
                        case EVOCATION -> gson.fromJson(entry, Evocation.class);
                    };
            charm.setSourceId(collection.sourceId);
            charm.setAbility(
                    collection.kind == CharmKind.EVOCATION
                            ? collection.artifactId
                            : collection.ability);
            collection.charms.add(charm);
        }
        return collection;
    }

    public JsonObject toJson(CharmCollection collection) {
        JsonObject json = new JsonObject();
        json.addProperty("$schema", "../charm-schema.json");
        json.addProperty("version", CharmCollection.VERSION);
        json.addProperty("sourceId", collection.sourceId);
        json.add("kind", gson.toJsonTree(collection.kind));
        if (collection.exaltType != null)
            json.addProperty("exaltType", collection.exaltType.name());
        if (collection.id != null) json.addProperty("id", collection.id);
        if (collection.kind == CharmKind.EVOCATION) {
            json.addProperty("artifactId", collection.artifactId);
            if (collection.artifactName != null)
                json.addProperty("artifactName", collection.artifactName);
        } else json.addProperty("ability", collection.ability);
        JsonArray entries = new JsonArray();
        for (Charm charm : collection.charms) {
            if (charm.getKind() != collection.kind || charm.getExaltType() != collection.exaltType)
                throw error("A collection cannot mix Charm families.");
            if (!java.util.Objects.equals(
                    charm.getAbility(),
                    collection.kind == CharmKind.EVOCATION
                            ? collection.artifactId
                            : collection.ability))
                throw error("A collection cannot mix Abilities or artifacts.");
            if (charm.getSourceId() != null && !charm.getSourceId().equals(collection.sourceId))
                throw error("A collection cannot mix source books.");
            JsonObject entry = gson.toJsonTree(charm).getAsJsonObject();
            for (String field : List.of("kind", "exaltType", "sourceId", "ability", "artifactId"))
                entry.remove(field);
            entries.add(entry);
        }
        json.add("charms", entries);
        read(json); // Validate writes using the same contract as reads.
        return json;
    }

    public void write(Path path, CharmCollection collection) throws IOException {
        String text;
        try {
            text = gson.toJson(toJson(collection));
        } catch (JsonParseException ex) {
            throw new IOException(
                    "Cannot save Charm collection " + path + ": " + ex.getMessage(), ex);
        }
        Files.createDirectories(path.getParent());
        Files.writeString(path, text, StandardCharsets.UTF_8);
    }

    private static String required(JsonObject json, String field) {
        String value = optional(json, field);
        if (value == null || value.isBlank()) throw error("Missing required field: " + field);
        return value;
    }

    private static String optional(JsonObject json, String field) {
        if (!json.has(field) || json.get(field).isJsonNull()) return null;
        if (!json.get(field).isJsonPrimitive() || !json.getAsJsonPrimitive(field).isString())
            throw error("Expected a string for " + field);
        return json.get(field).getAsString();
    }

    private static void rejectLegacyFields(JsonObject json) {
        for (String field : List.of("category", "exalt", "exaltTypes", "evocations")) {
            if (json.has(field))
                throw error("Unsupported legacy field " + field + "; re-import the source book.");
        }
    }

    private static JsonParseException error(String message) {
        return new JsonParseException(message);
    }
}
