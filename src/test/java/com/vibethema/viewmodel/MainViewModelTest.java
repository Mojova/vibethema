package com.vibethema.viewmodel;

import com.vibethema.model.CharacterData;
import de.saxsys.mvvmfx.utils.notifications.NotificationObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.vibethema.viewmodel.util.Messenger;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class MainViewModelTest {

    private MainViewModel viewModel;
    private CharacterData data;

    @BeforeEach
    void setUp() {
        data = new CharacterData();
        viewModel = new MainViewModel(data);
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
}
