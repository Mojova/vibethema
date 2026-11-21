package com.vibethema.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Synthetic current-format collections; no rulebook text is used in service tests. */
public final class CharmFixtures {
    private CharmFixtures() {}

    public static String solar(String source, String ability, String entries) {
        return nativeCharms(source, "SOLAR", ability, entries);
    }

    public static String nativeCharms(String source, String owner, String ability, String entries) {
        JsonObject json = base(source, "exalted", entries);
        json.addProperty("exaltType", owner);
        json.addProperty("ability", ability);
        return json.toString();
    }

    public static String martialArts(String source, String ability, String entries) {
        JsonObject json = base(source, "martialArts", entries);
        json.addProperty("ability", ability);
        return json.toString();
    }

    public static String evocations(String source, String artifact, String entries) {
        JsonObject json = base(source, "evocation", entries);
        json.addProperty("artifactId", artifact);
        json.addProperty("artifactName", "Test Artifact");
        return json.toString();
    }

    private static JsonObject base(String source, String kind, String entries) {
        JsonObject json = new JsonObject();
        json.addProperty("version", "0.3.0");
        json.addProperty("sourceId", source);
        json.addProperty("kind", kind);
        json.add("charms", JsonParser.parseString(entries));
        return json;
    }
}
