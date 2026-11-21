package com.vibethema.service;

import com.vibethema.model.ExaltType;
import com.vibethema.model.mystic.CharmKind;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Generates stable, source-aware identifiers for imported definitions. */
public final class DefinitionId {
    private DefinitionId() {}

    public static String forCharm(
            String sourceId, CharmKind kind, ExaltType owner, String name, String ability) {
        if (kind == null || (kind == CharmKind.EXALTED) != (owner != null))
            throw new IllegalArgumentException(
                    "Only native Exalted Charms require an owning Exalt type.");
        return from(
                sourceId, "charm", kind.name(), owner == null ? "" : owner.name(), name, ability);
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
