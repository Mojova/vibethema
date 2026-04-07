package com.vibethema.ui;

import com.vibethema.model.CharacterData;
import com.vibethema.service.CharmDataService;
import com.vibethema.service.EquipmentDataService;
import com.vibethema.service.SystemDataService;
import com.vibethema.viewmodel.MainViewModel;
import javafx.application.Platform;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class MainViewTest {

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

    @Test
    void testLazyLoadingTabs() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                CharacterData data = new CharacterData();
                SystemDataService systemDataService = Mockito.mock(SystemDataService.class);
                EquipmentDataService equipmentService = Mockito.mock(EquipmentDataService.class);
                CharmDataService charmDataService = Mockito.mock(CharmDataService.class);
                
                when(systemDataService.isCoreDataImported()).thenReturn(true);
                
                MainViewModel viewModel = new MainViewModel(data, systemDataService, equipmentService, charmDataService);
                
                // We need to inject the viewmodel manually since we're not using FluentViewLoader here
                // but MainView uses @InjectViewModel. FluentViewLoader is the standard way.
                de.saxsys.mvvmfx.ViewTuple<MainView, MainViewModel> viewTuple = de.saxsys.mvvmfx.FluentViewLoader
                        .javaView(MainView.class)
                        .viewModel(viewModel)
                        .load();
                
                MainView view = (MainView) viewTuple.getView();
                TabPane tabPane = (TabPane) view.getCenter();
                
                // 1. Check initially loaded tab (Stats)
                Tab statsTab = tabPane.getTabs().stream()
                        .filter(t -> "Stats".equals(t.getText()))
                        .findFirst().orElseThrow();
                
                // StatsTab content is now loaded via Platform.runLater, so we need to wait a pulse
                Platform.runLater(() -> {
                    assertNotNull(statsTab.getContent(), "Stats tab should eventually load content");
                    
                    // 2. Check lazy tab (Merits)
                    Tab meritsTab = tabPane.getTabs().stream()
                            .filter(t -> "Merits".equals(t.getText()))
                            .findFirst().orElseThrow();
                    assertNull(meritsTab.getContent(), "Merits tab should NOT be loaded initially");
                    
                    // 3. Select Merits tab and verify it loads
                    tabPane.getSelectionModel().select(meritsTab);
                    assertNotNull(meritsTab.getContent(), "Merits tab should load after selection");
                    
                    latch.countDown();
                });
            } catch (Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Test timed out or failed");
    }
}
