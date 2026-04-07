package com.vibethema.viewmodel;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.social.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.logic.*;


import com.vibethema.service.CharmDataService;
import com.vibethema.service.EquipmentDataService;
import com.vibethema.service.SystemDataService;
import com.vibethema.viewmodel.util.Messenger;
import de.saxsys.mvvmfx.utils.notifications.NotificationObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.ArgumentMatchers.anyString;

public class MainViewModelTest {

    private MainViewModel viewModel;
    private CharacterData data;
    private EquipmentDataService equipmentService;
    private CharmDataService charmDataService;
    private SystemDataService systemDataService;

    @BeforeEach
    void setUp() {
        data = new CharacterData();
        systemDataService = Mockito.mock(SystemDataService.class);
        equipmentService = Mockito.mock(EquipmentDataService.class);
        charmDataService = Mockito.mock(CharmDataService.class);
        
        // By default, assume core data is imported for these tests
        when(systemDataService.isCoreDataImported()).thenReturn(true);
        // Important: loadWeapon should be stubbed to return null (or a mock) to avoid race conditions 
        // with the background thread during mock setup for other methods.
        when(equipmentService.loadWeapon(anyString())).thenReturn(null); 
        when(equipmentService.loadEquipmentTags()).thenReturn(new HashMap<>());
        when(charmDataService.loadKeywords()).thenReturn(Collections.emptyList());
        
        viewModel = new MainViewModel(data, systemDataService, equipmentService, charmDataService);
    }

    @Test
    void testAsyncDataLoading() throws InterruptedException {
        // Prepare mock data
        Map<String, List<EquipmentDataService.Tag>> mockTags = new HashMap<>();
        mockTags.put("weapon", List.of(new EquipmentDataService.Tag("Lethal", "Causes lethal damage")));
        
        com.vibethema.model.mystic.Keyword mockKeyword = new com.vibethema.model.mystic.Keyword();
        mockKeyword.setName("Aggravated");
        mockKeyword.setDescription("Causes aggravated damage");
        
        // Use doReturn for cleaner thread safety if the mock is being accessed concurrently
        doReturn(mockTags).when(equipmentService).loadEquipmentTags();
        doReturn(List.of(mockKeyword)).when(charmDataService).loadKeywords();

        // Trigger background loading by creating ViewModel
        viewModel = new MainViewModel(data, systemDataService, equipmentService, charmDataService);
        
        // Wait briefly for the background thread to complete
        int attempts = 0;
        while ((viewModel.getTagDescriptions().isEmpty() || viewModel.getKeywordDefs().isEmpty()) && attempts < 10) {
            Thread.sleep(100);
            attempts++;
        }
        
        assertEquals("Causes lethal damage", viewModel.getTagDescriptions().get("Lethal"));
        assertEquals("Causes aggravated damage", viewModel.getKeywordDefs().get("Aggravated"));
    }

    @Test
    void testNewCharacterRequestWhenClean() {
        // First set some state, which will mark it dirty
        viewModel.getData().nameProperty().set("Modified");
        // Then manually mark it clean to simulate a "just saved" state
        viewModel.getData().setDirty(false);
        
        viewModel.onNewCharacterRequest();
        
        assertEquals("", viewModel.getData().nameProperty().get());
        assertFalse(viewModel.dirtyProperty().get());
    }

    @Test
    void testNewCharacterRequestWhenDirtyPublishesConfirmation() {
        AtomicReference<String> receivedMessage = new AtomicReference<>();
        viewModel.getData().setDirty(true);
        
        NotificationObserver observer = (name, payload) -> receivedMessage.set(name);
        Messenger.subscribe("confirm_discard_changes", observer);
        
        try {
            viewModel.onNewCharacterRequest();
            assertEquals("confirm_discard_changes", receivedMessage.get());
        } finally {
            Messenger.unsubscribe("confirm_discard_changes", observer);
        }
    }

    @Test
    void testSaveRequestWhenNoFilePublishesRequestSaveAs() {
        AtomicReference<Object[]> receivedPayload = new AtomicReference<>();
        viewModel.currentFileProperty().set(null);
        viewModel.getData().nameProperty().set("Tulentanssija");
        
        NotificationObserver observer = (name, payload) -> receivedPayload.set(payload);
        Messenger.subscribe("request_save_as", observer);
        
        try {
            viewModel.onSaveRequest();
            assertNotNull(receivedPayload.get());
            assertEquals("Tulentanssija.vbtm", receivedPayload.get()[0]);
        } finally {
            Messenger.unsubscribe("request_save_as", observer);
        }
    }

    @Test
    void testLoadRequestWhenDirtyPublishesConfirmation() {
        AtomicReference<String> receivedMessage = new AtomicReference<>();
        viewModel.getData().setDirty(true);
        
        NotificationObserver observer = (name, payload) -> receivedMessage.set(name);
        Messenger.subscribe("confirm_discard_changes", observer);
        
        try {
            viewModel.onLoadRequest();
            assertEquals("confirm_discard_changes", receivedMessage.get());
        } finally {
            Messenger.unsubscribe("confirm_discard_changes", observer);
        }
    }

    @Test
    void testExportPdfRequestPublishesWithSuggestedName() {
        AtomicReference<Object[]> receivedPayload = new AtomicReference<>();
        viewModel.currentFileProperty().set(new java.io.File("Hero.vbtm"));
        
        NotificationObserver observer = (name, payload) -> receivedPayload.set(payload);
        Messenger.subscribe("request_pdf_export", observer);
        
        try {
            viewModel.onExportPdfRequest();
            assertNotNull(receivedPayload.get());
            assertEquals("Hero.pdf", receivedPayload.get()[0]);
        } finally {
            Messenger.unsubscribe("request_pdf_export", observer);
        }
    }

    @Test
    void testResetToNewPreservesObjectIdentity() {
        CharacterData originalData = viewModel.getData();
        viewModel.getData().nameProperty().set("ToBeCleared");
        
        viewModel.resetToNew();
        
        assertSame(originalData, viewModel.getData(), "CharacterData instance must be preserved for bindings");
        assertEquals("", viewModel.getData().nameProperty().get());
    }
}