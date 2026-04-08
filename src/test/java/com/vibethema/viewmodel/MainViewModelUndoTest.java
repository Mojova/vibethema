package com.vibethema.viewmodel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vibethema.model.CharacterData;
import com.vibethema.service.CharmDataService;
import com.vibethema.service.EquipmentDataService;
import com.vibethema.service.PdfExportService;
import com.vibethema.service.SystemDataService;
import com.vibethema.viewmodel.util.Messenger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MainViewModelUndoTest {
    private MainViewModel viewModel;
    private CharacterData data;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            // Toolkit already started
            latch.countDown();
        }
        latch.await(5, TimeUnit.SECONDS);
    }

    @BeforeEach
    void setUp() {
        data = new CharacterData();
        viewModel = new MainViewModel(
                data,
                mock(SystemDataService.class),
                mock(EquipmentDataService.class),
                mock(CharmDataService.class),
                mock(PdfExportService.class)
        );
    }

    @Test
    void testRecordUndoCheckpointViaMessenger() {
        data.nameProperty().set("Initial");
        
        // Publish checkpoint message
        Messenger.publish("RECORD_UNDO_CHECKPOINT", 
            new MainViewModel.CheckpointRequest("Stats", "Name change"));
        
        assertTrue(viewModel.canUndoProperty().get(), "Should be able to undo after checkpoint");
        
        data.nameProperty().set("New Name");
        
        viewModel.undo();
        
        assertEquals("Initial", data.nameProperty().get(), "Undo should revert name");
        assertFalse(viewModel.canUndoProperty().get());
        assertTrue(viewModel.canRedoProperty().get());
        
        viewModel.redo();
        assertEquals("New Name", data.nameProperty().get(), "Redo should restore name");
    }

    @Test
    void testUndoTriggersTabSwitch() {
        // 1. Initial state on Stats tab
        viewModel.currentTabIdProperty().set("Stats");
        data.nameProperty().set("Bob");
        Messenger.publish("RECORD_UNDO_CHECKPOINT", new MainViewModel.CheckpointRequest("Stats", "Change Name"));
        
        // 2. Change tab to Charms
        viewModel.currentTabIdProperty().set("Charms");
        data.nameProperty().set("Alice");
        
        // Trigger undo from Charms tab for a Stats operation
        // Use an observer to catch the switch message
        final String[] switchedTo = {null};
        Messenger.subscribe("switch_to_tab", (name, payload) -> {
            switchedTo[0] = (String) payload[0];
        });
        
        viewModel.undo();
        
        assertEquals("Bob", data.nameProperty().get());
        assertEquals("Stats", switchedTo[0], "System should publish request to switch back to Stats tab");
    }
}
