package com.vibethema.ui;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.vibethema.model.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;
import com.vibethema.model.traits.*;
import com.vibethema.service.CharmDataService;
import com.vibethema.service.EquipmentDataService;
import com.vibethema.service.SystemDataService;
import com.vibethema.viewmodel.MainViewModel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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

        Platform.runLater(
                () -> {
                    try {
                        CharacterData data = new CharacterData();
                        SystemDataService systemDataService = Mockito.mock(SystemDataService.class);
                        EquipmentDataService equipmentService =
                                Mockito.mock(EquipmentDataService.class);
                        CharmDataService charmDataService = Mockito.mock(CharmDataService.class);

                        when(systemDataService.isCoreDataImported()).thenReturn(true);

                        MainViewModel viewModel =
                                new MainViewModel(
                                        data,
                                        systemDataService,
                                        equipmentService,
                                        charmDataService);

                        // We load MainView. Note: in real app we use ViewCache, but here we load
                        // fresh for testing.
                        de.saxsys.mvvmfx.ViewTuple<MainView, MainViewModel> viewTuple =
                                de.saxsys.mvvmfx.FluentViewLoader.javaView(MainView.class)
                                        .viewModel(viewModel)
                                        .load();

                        MainView view = (MainView) viewTuple.getView();
                        TabPane tabPane = (TabPane) view.getCenter();

                        // 1. Check initially loaded tab (Stats)
                        Tab statsTab =
                                tabPane.getTabs().stream()
                                        .filter(t -> "Stats".equals(t.getText()))
                                        .findFirst()
                                        .orElseThrow();

                        // Nest multiple Platform.runLater calls to match StatsTab's staggered
                        // loading (4 pulses)
                        Platform.runLater(
                                () -> {
                                    Platform.runLater(
                                            () -> {
                                                Platform.runLater(
                                                        () -> {
                                                            Platform.runLater(
                                                                    () -> {
                                                                        ScrollPane scrollPane =
                                                                                (ScrollPane)
                                                                                        statsTab
                                                                                                .getContent();
                                                                        VBox content =
                                                                                (VBox)
                                                                                        scrollPane
                                                                                                .getContent();
                                                                        assertNotNull(
                                                                                content,
                                                                                "Stats tab should"
                                                                                    + " eventually"
                                                                                    + " load"
                                                                                    + " content");
                                                                        // Verify at least one
                                                                        // section was added
                                                                        assertFalse(
                                                                                content.getChildren()
                                                                                        .isEmpty(),
                                                                                "Stats content"
                                                                                    + " should not"
                                                                                    + " be empty");

                                                                        // 2. Check lazy tab
                                                                        // (Merits)
                                                                        Tab meritsTab =
                                                                                tabPane
                                                                                        .getTabs()
                                                                                        .stream()
                                                                                        .filter(
                                                                                                t ->
                                                                                                        "Merits"
                                                                                                                .equals(
                                                                                                                        t
                                                                                                                                .getText()))
                                                                                        .findFirst()
                                                                                        .orElseThrow();
                                                                        assertNull(
                                                                                meritsTab
                                                                                        .getContent(),
                                                                                "Merits tab should"
                                                                                    + " NOT be"
                                                                                    + " loaded"
                                                                                    + " initially");

                                                                        // 3. Select Merits tab and
                                                                        // verify it loads
                                                                        tabPane.getSelectionModel()
                                                                                .select(meritsTab);
                                                                        assertNotNull(
                                                                                meritsTab
                                                                                        .getContent(),
                                                                                "Merits tab should"
                                                                                    + " load after"
                                                                                    + " selection");

                                                                        latch.countDown();
                                                                    });
                                                        });
                                            });
                                });
                    } catch (Exception e) {
                        e.printStackTrace();
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Test timed out or failed");
    }

    @Test
    void testStaggeredStatsLoad() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(
                () -> {
                    try {
                        CharacterData data = new CharacterData();
                        StatsTab statsTab =
                                (StatsTab)
                                        de.saxsys.mvvmfx.FluentViewLoader.javaView(StatsTab.class)
                                                .viewModel(
                                                        new com.vibethema.viewmodel.StatsViewModel(
                                                                data))
                                                .load()
                                                .getView();

                        // Pulse 1: Advantages + Initial setup (Step 1)
                        Platform.runLater(
                                () -> {
                                    ScrollPane scrollPane = (ScrollPane) statsTab;
                                    VBox content = (VBox) scrollPane.getContent();
                                    assertNotNull(
                                            content,
                                            "Content should be initialized in first pulse");
                                    assertEquals(
                                            1,
                                            content.getChildren().size(),
                                            "Should have 1 section (Advantages)");

                                    // Pulse 2: Attributes (Step 2)
                                    Platform.runLater(
                                            () -> {
                                                assertEquals(
                                                        2,
                                                        content.getChildren().size(),
                                                        "Should have 2 sections (Advantages +"
                                                                + " Attributes)");

                                                // Pulse 3: Abilities (Step 3)
                                                Platform.runLater(
                                                        () -> {
                                                            // Plus 1 for the separator added in
                                                            // step 3
                                                            assertEquals(
                                                                    4,
                                                                    content.getChildren().size(),
                                                                    "Should have added Abilities"
                                                                            + " and Separator");

                                                            // Pulse 4: Final stats (Step 4)
                                                            Platform.runLater(
                                                                    () -> {
                                                                        assertEquals(
                                                                                7,
                                                                                content.getChildren()
                                                                                        .size(),
                                                                                "Should have added"
                                                                                    + " all remaining"
                                                                                    + " sections");
                                                                        latch.countDown();
                                                                    });
                                                        });
                                            });
                                });
                    } catch (Exception e) {
                        e.printStackTrace();
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Staggered load test timed out");
    }
}
