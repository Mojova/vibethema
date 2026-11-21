package com.vibethema.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class DefinitionIdTest {
    @Test
    void idsAreStableForTheSameSourceAndIdentity() {
        assertEquals(
                DefinitionId.forCharm(
                        "exalted-3e-core",
                        com.vibethema.model.mystic.CharmKind.EXALTED,
                        com.vibethema.model.ExaltType.SOLAR,
                        "Excellent Strike",
                        "Melee"),
                DefinitionId.forCharm(
                        "exalted-3e-core",
                        com.vibethema.model.mystic.CharmKind.EXALTED,
                        com.vibethema.model.ExaltType.SOLAR,
                        "Excellent Strike",
                        "Melee"));
    }

    @Test
    void idsDifferWhenTheSourceChanges() {
        assertNotEquals(
                DefinitionId.forCharm(
                        "exalted-3e-core",
                        com.vibethema.model.mystic.CharmKind.EXALTED,
                        com.vibethema.model.ExaltType.SOLAR,
                        "Excellent Strike",
                        "Melee"),
                DefinitionId.forCharm(
                        "exalted-3e-lunars",
                        com.vibethema.model.mystic.CharmKind.EXALTED,
                        com.vibethema.model.ExaltType.SOLAR,
                        "Excellent Strike",
                        "Melee"));
    }

    @Test
    void entityTypesCannotCollide() {
        assertNotEquals(
                DefinitionId.forSpell("exalted-3e-core", "Invulnerable Skin", "SOLAR"),
                DefinitionId.forCharm(
                        "exalted-3e-core",
                        com.vibethema.model.mystic.CharmKind.EXALTED,
                        com.vibethema.model.ExaltType.SOLAR,
                        "Invulnerable Skin",
                        "SOLAR"));
    }

    @Test
    void canonicalizesIdentityPartsAndSupportsMartialArtsStyles() {
        assertEquals(
                DefinitionId.forCharm(
                        "core",
                        com.vibethema.model.mystic.CharmKind.EXALTED,
                        com.vibethema.model.ExaltType.SOLAR,
                        "Fire  Punch",
                        " Melee "),
                DefinitionId.forCharm(
                        "CORE",
                        com.vibethema.model.mystic.CharmKind.EXALTED,
                        com.vibethema.model.ExaltType.SOLAR,
                        "fire punch",
                        "melee"));
        assertNotEquals(
                DefinitionId.forMartialArtsStyle("core", "Snake Style"),
                DefinitionId.forMartialArtsStyle("lunar-book", "Snake Style"));
    }

    @Test
    void sameBookAndNameCannotCollideAcrossNativeFamiliesOrSharedDisciplines() {
        var solar =
                DefinitionId.forCharm(
                        "mixed-book",
                        com.vibethema.model.mystic.CharmKind.EXALTED,
                        com.vibethema.model.ExaltType.SOLAR,
                        "Test",
                        "Melee");
        var lunar =
                DefinitionId.forCharm(
                        "mixed-book",
                        com.vibethema.model.mystic.CharmKind.EXALTED,
                        com.vibethema.model.ExaltType.LUNAR,
                        "Test",
                        "Melee");
        var martial =
                DefinitionId.forCharm(
                        "mixed-book",
                        com.vibethema.model.mystic.CharmKind.MARTIAL_ARTS,
                        null,
                        "Test",
                        "Melee");
        assertNotEquals(solar, lunar);
        assertNotEquals(solar, martial);
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () ->
                        DefinitionId.forCharm(
                                "book",
                                com.vibethema.model.mystic.CharmKind.EXALTED,
                                null,
                                "Test",
                                "Melee"));
    }
}
