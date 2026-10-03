package com.vibethema.service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Generates stable, source-aware identifiers for imported definitions. */
public final class DefinitionId {
    private DefinitionId() {}

    public static String forCharm(String sourceId, String name, String ability) {
        return from(sourceId, "charm", name, ability);
    }

    public static String forSpell(String sourceId, String name, String circle) {
        return from(sourceId, "spell", name, circle);
    }

    public static String forMerit(String sourceId, String name) {
        return from(sourceId, "merit", name);
    }

    public static String forEquipment(String sourceId, String category, String name) {
        return from(sourceId, "equipment", category, name);
    }

    public static String forMartialArtsStyle(String sourceId, String styleName) {
        return from(sourceId, "martial-arts-style", styleName);
    }

    public static String from(String sourceId, String entityType, String... identityParts) {
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("sourceId must not be blank");
        }
        StringBuilder key =
                new StringBuilder(canonical(sourceId)).append('|').append(canonical(entityType));
        for (String part : identityParts) {
            key.append('|').append(canonical(part));
        }
        return UUID.nameUUIDFromBytes(key.toString().getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String canonical(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
