package com.vibethema.viewmodel.charms;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vibethema.model.CharacterData;
import com.vibethema.service.CharmDataService;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CharmTreeViewModelTest {

    private CharmTreeViewModel viewModel;
    private CharacterData data;
    private CharmDataService dataService;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        latch.await(5, TimeUnit.SECONDS);
    }

    @BeforeEach
    void setUp() {
        data = new CharacterData();
        dataService = mock(CharmDataService.class);
        viewModel = new CharmTreeViewModel(data, dataService, new HashMap<>());
        viewModel.initialize("Ability", "Melee", "Melee", null, null);
    }

    @Test
    void testSearchDebouncing() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        // Mock return for Melee charms
        when(dataService.loadCharmsForAbility("Melee")).thenReturn(new java.util.ArrayList<>());

        Platform.runLater(
                () -> {
                    // 1. Initial refresh should have happened in initialize()
                    verify(dataService, atLeastOnce()).loadCharmsForAbility("Melee");
                    reset(dataService); // Clear counts

                    // 2. Change search text
                    viewModel.searchTextProperty().set("Fire");

                    // 3. Verify it hasn't refreshed immediately
                    verify(dataService, never()).loadCharmsForAbility(anyString());

                    // 4. Wait for debounce (250ms + margin)
                    new Thread(
                                    () -> {
                                        try {
                                            Thread.sleep(400); // 250ms debounce
                                            Platform.runLater(
                                                    () -> {
                                                        verify(dataService, atLeastOnce())
                                                                .loadCharmsForAbility("Melee");
                                                        latch.countDown();
                                                    });
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    })
                            .start();
                });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Debounce refresh did not happen in time");
    }
}
