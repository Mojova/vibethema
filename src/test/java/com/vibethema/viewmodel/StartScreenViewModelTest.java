package com.vibethema.viewmodel;

import com.vibethema.service.SystemDataService;
import com.vibethema.viewmodel.util.Messenger;
import de.saxsys.mvvmfx.utils.notifications.NotificationObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for StartScreenViewModel.
 */
public class StartScreenViewModelTest {

    private StartScreenViewModel viewModel;
    private SystemDataService systemDataService;

    @BeforeEach
    void setUp() {
        systemDataService = Mockito.mock(SystemDataService.class);
        // By default, assume core data is imported for these tests
        when(systemDataService.isCoreDataImported()).thenReturn(true);
        viewModel = new StartScreenViewModel(systemDataService);
    }

    /**
     * Verifies that clicking "New Character" publishes the show_main_view notification.
     */
    @Test
    void testNewCharacterPublishesShowMainView() {
        AtomicReference<String> receivedMessage = new AtomicReference<>();
        NotificationObserver observer = (name, payload) -> receivedMessage.set(name);
        Messenger.subscribe("show_main_view", observer);

        try {
            viewModel.onNewCharacter();
            assertEquals("show_main_view", receivedMessage.get());
        } finally {
            Messenger.unsubscribe("show_main_view", observer);
        }
    }

    /**
     * Verifies that clicking "Load Character" publishes the request_load_file_start notification.
     */
    @Test
    void testLoadCharacterPublishesRequestLoadFileStart() {
        AtomicReference<String> receivedMessage = new AtomicReference<>();
        NotificationObserver observer = (name, payload) -> receivedMessage.set(name);
        Messenger.subscribe("request_load_file_start", observer);

        try {
            viewModel.onLoadCharacter();
            assertEquals("request_load_file_start", receivedMessage.get());
        } finally {
            Messenger.unsubscribe("request_load_file_start", observer);
        }
    }

    /**
     * Verifies that selecting a file publishes the show_main_view notification with the file payload.
     */
    @Test
    void testLoadFileSelectedPublishesShowMainViewWithFile() {
        AtomicReference<Object[]> receivedPayload = new AtomicReference<>();
        File testFile = new File("test.vbtm");
        NotificationObserver observer = (name, payload) -> receivedPayload.set(payload);
        Messenger.subscribe("show_main_view", observer);

        try {
            viewModel.onLoadFileSelected(testFile);
            assertNotNull(receivedPayload.get());
            assertEquals(testFile, receivedPayload.get()[0]);
        } finally {
            Messenger.unsubscribe("show_main_view", observer);
        }
    }
}
