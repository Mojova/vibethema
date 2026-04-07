package com.vibethema.model;

import static org.junit.jupiter.api.Assertions.*;

import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;
import com.vibethema.model.traits.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the XpAward model and the XP-related fields in CharacterData and
 * CharacterSaveState.
 */
public class XpModelTest {

    private CharacterData data;

    @BeforeEach
    void setUp() {
        data = new CharacterData();
    }

    // ─── XpAward construction ──────────────────────────────────────

    @Test
    void testXpAwardDefaults() {
        XpAward award = new XpAward();
        assertNotNull(award.getId(), "ID should be auto-generated");
        assertFalse(award.getId().isBlank(), "ID should not be blank");
        assertEquals("", award.getDescription());
        assertEquals(0, award.getAmount());
        assertFalse(award.isSolar());
    }

    @Test
    void testXpAwardConvenienceConstructor() {
        XpAward award = new XpAward("Session 1", 5, false);
        assertEquals("Session 1", award.getDescription());
        assertEquals(5, award.getAmount());
        assertFalse(award.isSolar());
    }

    @Test
    void testXpAwardSolarFlag() {
        XpAward solar = new XpAward("Arete bonus", 3, true);
        assertTrue(solar.isSolar());

        XpAward regular = new XpAward("Session 2", 4, false);
        assertFalse(regular.isSolar());
    }

    @Test
    void testXpAwardExplicitId() {
        XpAward award = new XpAward("my-id", "Bonus", 10, true);
        assertEquals("my-id", award.getId());
    }

    @Test
    void testXpAwardNullIdFallsBackToGenerated() {
        XpAward award = new XpAward(null, "Bonus", 10, false);
        assertNotNull(award.getId());
        assertFalse(award.getId().isBlank());
    }

    @Test
    void testXpAwardMutableProperties() {
        XpAward award = new XpAward("Session 1", 5, false);
        award.setDescription("Updated Desc");
        award.setAmount(12);
        award.setSolar(true);

        assertEquals("Updated Desc", award.getDescription());
        assertEquals(12, award.getAmount());
        assertTrue(award.isSolar());
    }

    @Test
    void testXpAwardJavaFxProperties() {
        XpAward award = new XpAward("Test", 7, false);
        assertNotNull(award.descriptionProperty());
        assertNotNull(award.amountProperty());
        assertNotNull(award.isSolarProperty());

        // Verify property values match getters
        assertEquals(award.getDescription(), award.descriptionProperty().get());
        assertEquals(award.getAmount(), award.amountProperty().get());
        assertEquals(award.isSolar(), award.isSolarProperty().get());
    }

    // ─── CharacterData XP list ─────────────────────────────────────

    @Test
    void testCharacterDataStartsWithEmptyXpAwards() {
        assertTrue(data.getXpAwards().isEmpty(), "New character should have no XP awards");
    }

    @Test
    void testAddXpAwardToCharacter() {
        XpAward award = new XpAward("Session 1", 5, false);
        data.getXpAwards().add(award);

        assertEquals(1, data.getXpAwards().size());
        assertEquals(5, data.getXpAwards().get(0).getAmount());
    }

    @Test
    void testMultipleXpAwards() {
        data.getXpAwards().add(new XpAward("Session 1", 5, false));
        data.getXpAwards().add(new XpAward("Session 2", 3, false));
        data.getXpAwards().add(new XpAward("Solar Bonus", 2, true));

        assertEquals(3, data.getXpAwards().size());

        int totalRegular =
                data.getXpAwards().stream()
                        .filter(a -> !a.isSolar())
                        .mapToInt(XpAward::getAmount)
                        .sum();
        int totalSolar =
                data.getXpAwards().stream()
                        .filter(XpAward::isSolar)
                        .mapToInt(XpAward::getAmount)
                        .sum();

        assertEquals(8, totalRegular);
        assertEquals(2, totalSolar);
    }

    @Test
    void testRemoveXpAward() {
        XpAward a1 = new XpAward("Session 1", 5, false);
        XpAward a2 = new XpAward("Session 2", 3, false);
        data.getXpAwards().add(a1);
        data.getXpAwards().add(a2);

        data.getXpAwards().remove(a1);
        assertEquals(1, data.getXpAwards().size());
        assertEquals("Session 2", data.getXpAwards().get(0).getDescription());
    }

    // ─── Creation snapshot ─────────────────────────────────────────

    @Test
    void testCreationSnapshotStartsNull() {
        assertNull(data.getCreationSnapshot(), "New character should have no creation snapshot");
    }

    @Test
    void testSetCreationSnapshot() {
        CharacterSaveState snapshot = new CharacterSaveState();
        snapshot.name = "Snapshot";
        data.setCreationSnapshot(snapshot);

        assertNotNull(data.getCreationSnapshot());
        assertEquals("Snapshot", data.getCreationSnapshot().name);
    }

    @Test
    void testClearCreationSnapshot() {
        data.setCreationSnapshot(new CharacterSaveState());
        data.setCreationSnapshot(null);
        assertNull(data.getCreationSnapshot());
    }

    // ─── Round-trip serialization ──────────────────────────────────

    @Test
    void testXpAwardsRoundTrip() {
        data.getXpAwards().add(new XpAward("aa-bb", "Session 1", 5, false));
        data.getXpAwards().add(new XpAward("cc-dd", "Solar Bonus", 2, true));

        CharacterSaveState exported = data.exportState();

        assertEquals(2, exported.xpAwards.size());

        CharacterSaveState.XpAwardData first = exported.xpAwards.get(0);
        assertEquals("aa-bb", first.id);
        assertEquals("Session 1", first.description);
        assertEquals(5, first.amount);
        assertFalse(first.isSolar);

        CharacterSaveState.XpAwardData second = exported.xpAwards.get(1);
        assertEquals("cc-dd", second.id);
        assertEquals("Solar Bonus", second.description);
        assertEquals(2, second.amount);
        assertTrue(second.isSolar);
    }

    @Test
    void testImportStateRestoresXpAwards() {
        CharacterSaveState state = new CharacterSaveState();
        CharacterSaveState.XpAwardData xd = new CharacterSaveState.XpAwardData();
        xd.id = "ee-ff";
        xd.description = "Loaded Award";
        xd.amount = 7;
        xd.isSolar = false;
        state.xpAwards.add(xd);

        CharacterData fresh = new CharacterData();
        fresh.importState(state, new com.vibethema.service.EquipmentDataService());

        assertEquals(1, fresh.getXpAwards().size());
        XpAward loaded = fresh.getXpAwards().get(0);
        assertEquals("ee-ff", loaded.getId());
        assertEquals("Loaded Award", loaded.getDescription());
        assertEquals(7, loaded.getAmount());
        assertFalse(loaded.isSolar());
    }

    @Test
    void testCreationSnapshotRoundTrip() {
        CharacterSaveState snapshot = new CharacterSaveState();
        snapshot.name = "Baseline";
        snapshot.essence = 1;
        data.setCreationSnapshot(snapshot);

        CharacterSaveState exported = data.exportState();
        assertNotNull(exported.creationSnapshot);
        assertEquals("Baseline", exported.creationSnapshot.name);
        assertEquals(1, exported.creationSnapshot.essence);
    }

    @Test
    void testImportStateRestoresCreationSnapshot() {
        CharacterSaveState snapshot = new CharacterSaveState();
        snapshot.name = "SnapName";

        CharacterSaveState state = new CharacterSaveState();
        state.creationSnapshot = snapshot;

        CharacterData fresh = new CharacterData();
        fresh.importState(state, new com.vibethema.service.EquipmentDataService());

        assertNotNull(fresh.getCreationSnapshot());
        assertEquals("SnapName", fresh.getCreationSnapshot().name);
    }

    @Test
    void testImportStateWithNoXpAwardsClearsExisting() {
        // Pre-load some awards
        data.getXpAwards().add(new XpAward("Old", 5, false));

        // Import a state with no awards
        CharacterSaveState state = new CharacterSaveState();
        state.xpAwards = null;
        data.importState(state, new com.vibethema.service.EquipmentDataService());

        assertTrue(
                data.getXpAwards().isEmpty(),
                "Importing state without awards should clear existing awards");
    }
}
