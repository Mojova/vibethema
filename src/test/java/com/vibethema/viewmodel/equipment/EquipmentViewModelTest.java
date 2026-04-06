package com.vibethema.viewmodel.equipment;

import com.vibethema.model.*;
import com.vibethema.service.CharmDataService;
import com.vibethema.service.EquipmentDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.vibethema.viewmodel.util.Messenger;
import de.saxsys.mvvmfx.utils.notifications.NotificationObserver;

@ExtendWith(MockitoExtension.class)
public class EquipmentViewModelTest {
    private CharacterData data;
    private EquipmentViewModel viewModel;

    @Mock
    private EquipmentDataService equipmentService;
    @Mock
    private CharmDataService dataService;
    @Mock
    private Runnable refreshSummary;

    @BeforeEach
    void setUp() {
        data = new CharacterData();
        viewModel = new EquipmentViewModel(
            data, 
            equipmentService, 
            dataService,
            new HashMap<>(), 
            refreshSummary
        );
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
    void testUpdateArtifactWeaponTriggersSideEffect() throws IOException {
        Weapon artifact = new Weapon("Soul Mirror");
        artifact.setType(Weapon.WeaponType.ARTIFACT);
        data.getWeapons().add(artifact);
        
        artifact.setName("Soul Mirror (Awakened)");
        viewModel.saveWeapon(artifact, false);

        assertTrue(data.isDirty());
        verify(dataService, times(1)).updateEvocationCollectionName(artifact.getId(), "Soul Mirror (Awakened)");
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
        NotificationObserver observer = (name, payload) -> ref.set(new String[]{(String)payload[0], (String)payload[1]});
        Messenger.subscribe("jump_to_evocations", observer);
        try {
            viewModel.callEvocations("art123", "Daiklave");
            assertArrayEquals(new String[]{"art123", "Daiklave"}, ref.get());
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
        NotificationObserver observer = (name, payload) -> {
            lastMessage.set(name);
            lastPayload.set(payload);
        };

        String[] messages = {
            "show_weapon_dialog", "show_armor_dialog", "show_hearthstone_dialog", "show_other_equipment_dialog",
            "show_weapon_database", "show_armor_database", "show_hearthstone_database", "show_other_equipment_database"
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
