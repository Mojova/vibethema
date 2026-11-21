package com.vibethema.model.logic;

import com.vibethema.model.CharacterData;
import com.vibethema.service.DefinitionId;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Identity-based rules shared by creation validation and Martial Arts access. */
public final class MeritRules {
    public static final String MARTIAL_ARTIST_ID =
            DefinitionId.forMerit("exalted-3e-core", "Martial Artist");
    private static final String LEGACY_MARTIAL_ARTIST_ID =
            UUID.nameUUIDFromBytes("Martial Artist".getBytes(StandardCharsets.UTF_8)).toString();

    private MeritRules() {}

    public static boolean hasMartialArtist(CharacterData data) {
        return data.getMerits().stream()
                .anyMatch(m ->
                        (MARTIAL_ARTIST_ID.equals(m.getDefinitionId())
                                        || LEGACY_MARTIAL_ARTIST_ID.equals(m.getDefinitionId()))
                                && m.getRating() == 4);
    }
}
