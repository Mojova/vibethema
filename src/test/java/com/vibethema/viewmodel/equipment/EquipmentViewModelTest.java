package com.vibethema.viewmodel.equipment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.gson.Gson;
import com.vibethema.model.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.CharmPurchaseRules;
import com.vibethema.model.mystic.Evocation;
import com.vibethema.model.mystic.PrerequisiteRule;
import com.vibethema.model.mystic.PurchasedCharm;
import com.vibethema.service.CharmDataService;
import com.vibethema.service.EquipmentDataService;
import com.vibethema.viewmodel.charms.EditCharmViewModel;
import com.vibethema.viewmodel.util.Messenger;
import de.saxsys.mvvmfx.utils.notifications.NotificationObserver;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EquipmentViewModelTest {
    private CharacterData data;
    private EquipmentViewModel viewModel;

    @Mock private EquipmentDataService equipmentService;
    @Mock private CharmDataService dataService;
    @Mock private Runnable refreshSummary;

    @BeforeEach
    void setUp() {
        data = new CharacterData();
        // Add a mock 'Unarmed' to simulate a character created with default gear
        data.getWeapons()
                .add(
                        new Weapon(
                                Weapon.UNARMED_ID,
                                "Unarmed",
                                Weapon.WeaponRange.CLOSE,
                                Weapon.WeaponType.MORTAL,
                                Weapon.WeaponCategory.LIGHT));
        data.setDirty(false);

        viewModel =
                new EquipmentViewModel(
                        data, equipmentService, dataService, new HashMap<>(), refreshSummary);
    }

    @Test
    void testAddingWeaponUpdatesViewModelAndDirtyState() {
        assertFalse(data.isDirty());
        Weapon weapon = new Weapon("Test Blade");
        viewModel.saveWeapon(weapon, true);

        assertTrue(data.isDirty());
        assertEquals(2, data.getWeapons().size()); // 1 (Unarmed) + 1
        assertEquals(2, viewModel.getWeapons().size());
        verify(refreshSummary, times(1)).run();
    }

    @Test
    void testEditingArtifactDoesNotChangeTemplate() throws IOException {
        Weapon artifact = new Weapon("Soul Mirror");
        artifact.setType(Weapon.WeaponType.ARTIFACT);
        data.getWeapons().add(artifact);

        artifact.setName("Soul Mirror (Awakened)");
        viewModel.saveWeapon(artifact, false);

        assertTrue(data.isDirty());
        verify(dataService, never()).updateEvocationCollectionName(anyString(), anyString());
    }

    @Test
    void twoArtifactCopiesKeepSeparateTreesAndProgressAfterReopening() throws IOException {
        var template = new Weapon("Published daiklave");
        template.setType(Weapon.WeaponType.ARTIFACT);
        var root = new Evocation();
        root.setName("Opening");
        root.setArtifactId(template.getId());
        var branch = new Evocation();
        branch.setName("Follow-up");
        branch.setArtifactId(template.getId());
        branch.setPrerequisites(
                PrerequisiteRule.allOf(List.of(PrerequisiteRule.charm(root.getId()))));
        when(dataService.loadEvocations(template.getId()))
                .thenReturn(
                        new CharmDataService.EvocationCollection(
                                template.getId(), template.getName(), List.of(root, branch)));

        viewModel.addWeaponFromDatabase(template);
        viewModel.addWeaponFromDatabase(template);
        Weapon first = data.getWeapons().get(1);
        Weapon second = data.getWeapons().get(2);
        String firstId = first.getInstanceId();
        String secondId = second.getInstanceId();
        Evocation firstRoot = data.getEvocations(firstId).getFirst();
        Evocation firstBranch = data.getEvocations(firstId).getLast();
        Evocation secondBranch = data.getEvocations(secondId).getLast();
        assertNotEquals(firstRoot.getId(), data.getEvocations(secondId).getFirst().getId());
        assertNotEquals(root.getId(), firstRoot.getId());
        assertFalse(CharmPurchaseRules.canPurchase(firstBranch, data));
        data.addCharm(new PurchasedCharm(firstRoot.getId(), firstRoot.getName(), firstId));
        assertTrue(CharmPurchaseRules.canPurchase(firstBranch, data));
        assertFalse(CharmPurchaseRules.canPurchase(secondBranch, data));

        var editedWeapon = Weapon.fromData(first.toData());
        editedWeapon.setName("Personal daiklave");
        editedWeapon.setStatOverrides(Map.of(Weapon.Stat.DAMAGE, 17));
        viewModel.saveWeapon(editedWeapon, false);
        var editor =
                new EditCharmViewModel(
                        firstRoot, dataService, editedWeapon.getName(), "Evocation", null, data);
        editor.nameProperty().set("Personal awakening");
        editor.save();

        Gson gson = new Gson();
        var state = data.exportState();
        assertTrue(state.unlockedCharms.isEmpty());
        assertEquals(1, state.artifactEvocations.get(firstId).unlocked().size());
        assertTrue(state.artifactEvocations.get(secondId).unlocked().isEmpty());
        CharacterData reopened = new CharacterData();
        reopened.importState(gson.fromJson(gson.toJson(state), CharacterSaveState.class), null);
        assertEquals(state.artifactEvocations, reopened.exportState().artifactEvocations);
        assertEquals(state.weapons, reopened.exportState().weapons);
        assertEquals(
                0,
                com.vibethema.model.logic.ExperienceRuleEngine.calculateStatus(reopened, state)
                        .regularXpSpent);
        assertEquals("Personal daiklave", reopened.getArtifactName(firstId));
        assertEquals("Published daiklave", reopened.getArtifactName(secondId));
        assertEquals(17, reopened.getWeapons().get(1).getDamage());
        assertEquals(template.getDamage(), reopened.getWeapons().get(2).getDamage());
        assertEquals("Personal awakening", reopened.getEvocations(firstId).getFirst().getName());
        assertEquals("Opening", reopened.getEvocations(secondId).getFirst().getName());
        assertTrue(reopened.hasCharm(firstRoot.getId()));
        assertTrue(
                CharmPurchaseRules.canPurchase(
                        reopened.getEvocations(firstId).getLast(), reopened));
        assertFalse(
                CharmPurchaseRules.canPurchase(
                        reopened.getEvocations(secondId).getLast(), reopened));
        assertEquals("Published daiklave", template.getName());
        assertEquals("Opening", root.getName());
        assertEquals(
                root.getId(), branch.getPrerequisites().getRequirements().getFirst().getCharmId());

        var cycle = reopened.getEvocations(firstId).getFirst().copyForArtifact(firstId);
        cycle.setPrerequisites(PrerequisiteRule.charm(firstBranch.getId()));
        assertThrows(IllegalArgumentException.class, () -> reopened.saveEvocation(cycle));
        assertThrows(IllegalArgumentException.class, () -> reopened.deleteEvocation(cycle));
    }

    @Test
    void testRemovingWeaponUpdatesViewModelAndDirtyState() {
        Weapon weapon = data.getWeapons().get(0); // Unarmed
        viewModel.removeWeapon(weapon);

        assertTrue(data.isDirty());
        assertEquals(0, data.getWeapons().size());
        assertEquals(0, viewModel.getWeapons().size());
        verify(refreshSummary, times(1)).run();
    }

    @Test
    void testSaveArmorNew() {
        Armor armor = new Armor("Plate");
        viewModel.saveArmor(armor, true);

        assertTrue(data.isDirty());
        assertEquals(1, data.getArmors().size());
        assertEquals(1, viewModel.getArmors().size());
        verify(refreshSummary, times(1)).run();
    }

    @Test
    void testSaveHearthstoneNew() {
        Hearthstone hs = new Hearthstone("Fire Gem", "Hot");
        viewModel.saveHearthstone(hs, true);

        assertTrue(data.isDirty());
        assertEquals(1, data.getHearthstones().size());
    }

    @Test
    void testSaveOtherEquipmentNew() {
        OtherEquipment oe = new OtherEquipment("Climbing Gear", "Basic");
        viewModel.saveOtherEquipment(oe, true);

        assertTrue(data.isDirty());
        assertEquals(1, data.getOtherEquipment().size());
    }

    @Test
    void testCallEvocationsTrigger() {
        AtomicReference<String[]> ref = new AtomicReference<>();
        NotificationObserver observer =
                (name, payload) -> ref.set(new String[] {(String) payload[0], (String) payload[1]});
        Messenger.subscribe("jump_to_evocations", observer);
        try {
            viewModel.callEvocations("art123", "Daiklave");
            assertArrayEquals(new String[] {"art123", "Daiklave"}, ref.get());
        } finally {
            Messenger.unsubscribe(null, observer);
        }
    }

    @Test
    void testModelUpdateSyncsToViewModel() {
        Weapon weapon = new Weapon("Background Added Blade");
        data.getWeapons().add(weapon);

        assertEquals(2, viewModel.getWeapons().size());
        assertEquals("Background Added Blade", viewModel.getWeapons().get(1).nameProperty().get());
        verify(refreshSummary, times(1)).run();
    }

    @Test
    void testMarkDirty() {
        assertFalse(data.isDirty());
        viewModel.markDirty();
        assertTrue(data.isDirty());
    }

    @Test
    void testRequestDialogsPublishNotifications() {
        AtomicReference<String> lastMessage = new AtomicReference<>();
        AtomicReference<Object[]> lastPayload = new AtomicReference<>();
        NotificationObserver observer =
                (name, payload) -> {
                    lastMessage.set(name);
                    lastPayload.set(payload);
                };

        String[] messages = {
            "show_weapon_dialog", "show_armor_dialog", "show_hearthstone_dialog",
                    "show_other_equipment_dialog",
            "show_weapon_database", "show_armor_database", "show_hearthstone_database",
                    "show_other_equipment_database"
        };

        for (String msg : messages) {
            Messenger.subscribe(msg, observer);
        }

        try {
            viewModel.requestAddWeapon();
            assertEquals("show_weapon_dialog", lastMessage.get());
            assertNull(lastPayload.get()[0]);

            Weapon w = new Weapon("Sun Blade");
            viewModel.requestEditWeapon(w);
            assertEquals("show_weapon_dialog", lastMessage.get());
            assertEquals(w, lastPayload.get()[0]);

            viewModel.requestWeaponDatabase();
            assertEquals("show_weapon_database", lastMessage.get());

            viewModel.requestAddArmor();
            assertEquals("show_armor_dialog", lastMessage.get());
            assertNull(lastPayload.get()[0]);

            viewModel.requestArmorDatabase();
            assertEquals("show_armor_database", lastMessage.get());
        } finally {
            for (String msg : messages) {
                Messenger.unsubscribe(msg, observer);
            }
        }
    }
}
