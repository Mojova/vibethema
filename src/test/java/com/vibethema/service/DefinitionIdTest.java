package com.vibethema.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class DefinitionIdTest {
    @Test
    void idsAreStableForTheSameSourceAndIdentity() {
        assertEquals(
                DefinitionId.forCharm("exalted-3e-core", "Excellent Strike", "Melee"),
                DefinitionId.forCharm("exalted-3e-core", "Excellent Strike", "Melee"));
    }

    @Test
    void idsDifferWhenTheSourceChanges() {
        assertNotEquals(
                DefinitionId.forCharm("exalted-3e-core", "Excellent Strike", "Melee"),
                DefinitionId.forCharm("exalted-3e-lunars", "Excellent Strike", "Melee"));
    }

    @Test
    void entityTypesCannotCollide() {
        assertNotEquals(
                DefinitionId.forSpell("exalted-3e-core", "Invulnerable Skin", "SOLAR"),
                DefinitionId.forCharm("exalted-3e-core", "Invulnerable Skin", "SOLAR"));
    }

    @Test
    void canonicalizesIdentityPartsAndSupportsMartialArtsStyles() {
        assertEquals(
                DefinitionId.forCharm("core", "Fire  Punch", " Melee "),
                DefinitionId.forCharm("CORE", "fire punch", "melee"));
        assertNotEquals(
                DefinitionId.forMartialArtsStyle("core", "Snake Style"),
                DefinitionId.forMartialArtsStyle("lunar-book", "Snake Style"));
    }
}
