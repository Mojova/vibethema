package com.vibethema.ui;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.vibethema.model.*;
import com.vibethema.service.CharmDataService;
import com.vibethema.service.EquipmentDataService;
import com.vibethema.service.PdfExportService;
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
    void animaEffectsRendersGeneralAndCasteTextAndUpdatesWhenCasteChanges() throws Exception {
        var task =
                new java.util.concurrent.FutureTask<Void>(() -> {
                    var data = new CharacterData();
                    var service = Mockito.mock(com.vibethema.service.AnimaDataService.class);
                    var general =
                            new com.vibethema.model.mystic.AnimaAbility(
                                    "general",
                                    "source",
                                    java.util.List.of("SOLAR"),
                                    java.util.List.of(),
                                    "General effects",
                                    "Shared cost and conditions",
                                    java.util.List.of("Shared effect"));
                    var dawn =
                            new com.vibethema.model.mystic.AnimaAbility(
                                    "dawn",
                                    "source",
                                    java.util.List.of("SOLAR"),
                                    java.util.List.of("DAWN"),
                                    "Dawn effects",
                                    "",
                                    java.util.List.of("Dawn effect"));
                    var levels =
                            new com.vibethema.model.mystic.AnimaAbility(
                                    "levels",
                                    "source",
                                    java.util.List.of("SOLAR"),
                                    java.util.List.of(),
                                    "Anima Levels",
                                    "Intensifying",
                                    java.util.List.of("Fading"),
                                    java.util.List.of(
                                            new com.vibethema.model.mystic.AnimaLevel(
                                                    "Dim", "Hidden"),
                                            new com.vibethema.model.mystic.AnimaLevel(
                                                    "Glowing", "Visible")));
                    when(service.loadAbilities(ExaltType.SOLAR, Caste.DAWN))
                            .thenReturn(java.util.List.of(general, levels, dawn));
                    when(service.loadAbilities(ExaltType.SOLAR, Caste.NIGHT))
                            .thenReturn(java.util.List.of(general, levels));
                    data.casteProperty().set(Caste.DAWN);
                    var vm = new com.vibethema.viewmodel.AnimaEffectsViewModel(data, service);
                    var view =
                            de.saxsys.mvvmfx.FluentViewLoader.javaView(AnimaEffectsTab.class)
                                    .viewModel(vm)
                                    .load()
                                    .getView();
                    VBox content = (VBox) ((ScrollPane) view).getContent();
                    var message = (javafx.scene.control.Label) content.getChildren().get(0);
                    assertFalse(message.isVisible());
                    assertFalse(message.isManaged());
                    VBox groups = (VBox) content.getChildren().get(1);
                    assertEquals(3, groups.getChildren().size());
                    VBox generalSection = (VBox) groups.getChildren().get(0);
                    assertEquals(
                            java.util.List.of(
                                    "General effects",
                                    "Shared cost and conditions",
                                    "• Shared effect"),
                            generalSection.getChildren().stream()
                                    .map(node -> ((javafx.scene.control.Label) node).getText())
                                    .toList());
                    VBox levelSection = (VBox) groups.getChildren().get(1);
                    assertEquals(
                            java.util.List.of(
                                    "Anima Levels",
                                    "Intensifying",
                                    "Dim",
                                    "Hidden",
                                    "Glowing",
                                    "Visible",
                                    "• Fading"),
                            levelSection.getChildren().stream()
                                    .map(node -> ((javafx.scene.control.Label) node).getText())
                                    .toList());
                    VBox casteSection = (VBox) groups.getChildren().get(2);
                    assertEquals(
                            "Dawn effects",
                            ((javafx.scene.control.Label) casteSection.getChildren().get(0))
                                    .getText());
                    assertEquals(
                            "• Dawn effect",
                            ((javafx.scene.control.Label) casteSection.getChildren().get(1))
                                    .getText());
                    data.casteProperty().set(Caste.NIGHT);
                    assertEquals(2, groups.getChildren().size());
                    data.casteProperty().set(Caste.NONE);
                    assertTrue(groups.getChildren().isEmpty());
                    assertTrue(message.isVisible());
                    return null;
                });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    @Test
    void animaEffectsTabFollowsStatsAndShowsCastePrompt() throws Exception {
        var task =
                new java.util.concurrent.FutureTask<Void>(() -> {
                    var data = new CharacterData();
                    var vm =
                            new MainViewModel(
                                    data,
                                    Mockito.mock(SystemDataService.class),
                                    Mockito.mock(EquipmentDataService.class),
                                    Mockito.mock(CharmDataService.class),
                                    Mockito.mock(PdfExportService.class));
                    MainView view =
                            (MainView)
                                    de.saxsys.mvvmfx.FluentViewLoader.javaView(MainView.class)
                                            .viewModel(vm)
                                            .load()
                                            .getView();
                    TabPane tabs = (TabPane) view.getCenter();
                    assertEquals("Stats", tabs.getTabs().get(0).getText());
                    Tab anima = tabs.getTabs().get(1);
                    assertEquals("Anima Effects", anima.getText());
                    assertNull(anima.getContent());
                    tabs.getSelectionModel().select(anima);
                    assertInstanceOf(AnimaEffectsTab.class, anima.getContent());
                    VBox content = (VBox) ((ScrollPane) anima.getContent()).getContent();
                    var message = (javafx.scene.control.Label) content.getChildren().getFirst();
                    assertEquals("Select caste to see anima effects", message.getText());
                    data.casteProperty().set(Caste.DAWN);
                    assertNotEquals("Select caste to see anima effects", message.getText());
                    data.casteProperty().set(Caste.NONE);
                    assertEquals("Select caste to see anima effects", message.getText());
                    return null;
                });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
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

                MainViewModel viewModel =
                        new MainViewModel(
                                data,
                                systemDataService,
                                equipmentService,
                                charmDataService,
                                Mockito.mock(PdfExportService.class));

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
                Platform.runLater(() -> {
                    Platform.runLater(() -> {
                        Platform.runLater(() -> {
                            Platform.runLater(() -> {
                                ScrollPane scrollPane = (ScrollPane) statsTab.getContent();
                                VBox content = (VBox) scrollPane.getContent();
                                assertNotNull(
                                        content,
                                        "Stats tab should" + " eventually" + " load" + " content");
                                // Verify at least one
                                // section was added
                                assertFalse(
                                        content.getChildren().isEmpty(),
                                        "Stats content" + " should not" + " be empty");

                                // 2. Check lazy tab
                                // (Merits)
                                Tab meritsTab =
                                        tabPane.getTabs().stream()
                                                .filter(t -> "Merits".equals(t.getText()))
                                                .findFirst()
                                                .orElseThrow();
                                assertNull(
                                        meritsTab.getContent(),
                                        "Merits tab should" + " NOT be" + " loaded" + " initially");

                                // 3. Select Merits tab and
                                // verify it loads
                                tabPane.getSelectionModel().select(meritsTab);
                                assertNotNull(
                                        meritsTab.getContent(),
                                        "Merits tab should" + " load after" + " selection");

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

        Platform.runLater(() -> {
            try {
                CharacterData data = new CharacterData();
                StatsTab statsTab =
                        (StatsTab)
                                de.saxsys.mvvmfx.FluentViewLoader.javaView(StatsTab.class)
                                        .viewModel(new com.vibethema.viewmodel.StatsViewModel(data))
                                        .load()
                                        .getView();

                // Pulse 1: Advantages + Initial setup (Step 1)
                Platform.runLater(() -> {
                    ScrollPane scrollPane = (ScrollPane) statsTab;
                    VBox content = (VBox) scrollPane.getContent();
                    assertNotNull(content, "Content should be initialized in first pulse");
                    assertEquals(
                            1, content.getChildren().size(), "Should have 1 section (Advantages)");

                    // Pulse 2: Attributes (Step 2)
                    Platform.runLater(() -> {
                        assertEquals(
                                2,
                                content.getChildren().size(),
                                "Should have 2 sections (Advantages +" + " Attributes)");

                        // Pulse 3: Abilities (Step 3)
                        Platform.runLater(() -> {
                            // Plus 1 for the separator added in
                            // step 3
                            assertEquals(
                                    4,
                                    content.getChildren().size(),
                                    "Should have added Abilities" + " and Separator");

                            // Pulse 4: Final stats (Step 4)
                            Platform.runLater(() -> {
                                assertEquals(
                                        7,
                                        content.getChildren().size(),
                                        "Should have added" + " all remaining" + " sections");
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

    @Test
    void checklistLinksLoadTheTargetTabAndHighlightTheMeritRow() throws Exception {
        java.util.concurrent.atomic.AtomicReference<MainView> viewRef =
                new java.util.concurrent.atomic.AtomicReference<>();
        var setup =
                new java.util.concurrent.FutureTask<Void>(() -> {
                    var data = new CharacterData();
                    data.getMerits()
                            .add(new com.vibethema.model.traits.Merit("navigate-test", "", 5));
                    var vm =
                            new MainViewModel(
                                    data,
                                    Mockito.mock(SystemDataService.class),
                                    Mockito.mock(EquipmentDataService.class),
                                    Mockito.mock(CharmDataService.class),
                                    Mockito.mock(PdfExportService.class));
                    MainView view =
                            (MainView)
                                    de.saxsys.mvvmfx.FluentViewLoader.javaView(MainView.class)
                                            .viewModel(vm)
                                            .load()
                                            .getView();
                    viewRef.set(view);
                    new javafx.scene.Scene(view, 1200, 850);
                    view.applyCss();
                    view.layout();
                    view.lookupAll(".titled-pane").stream()
                            .filter(n ->
                                    n instanceof javafx.scene.control.TitledPane pane
                                            && pane.getText().startsWith("Creation checklist"))
                            .forEach(n -> ((javafx.scene.control.TitledPane) n).setExpanded(true));
                    view.applyCss();
                    view.layout();
                    var issue =
                            vm.getFooterViewModel().getValidationIssues().stream()
                                    .filter(i -> i.code().equals("merit_name"))
                                    .findFirst()
                                    .orElseThrow();
                    var link =
                            view.lookupAll(".hyperlink").stream()
                                    .filter(n ->
                                            n instanceof javafx.scene.control.Hyperlink h
                                                    && h.getText().equals(issue.message()))
                                    .map(n -> (javafx.scene.control.Hyperlink) n)
                                    .findFirst()
                                    .orElseThrow();
                    link.fire();
                    return null;
                });
        Platform.runLater(setup);
        try {
            setup.get(10, TimeUnit.SECONDS);
            // Drain the queued lazy layout and highlight operations before asserting the result.
            for (int i = 0; i < 3; i++) {
                var pulse = new java.util.concurrent.FutureTask<Void>(() -> null);
                Platform.runLater(pulse);
                pulse.get(10, TimeUnit.SECONDS);
            }
            var verify =
                    new java.util.concurrent.FutureTask<Void>(() -> {
                        var view = viewRef.get();
                        var tabs = (javafx.scene.control.TabPane) view.getCenter();
                        assertEquals(
                                "Merits", tabs.getSelectionModel().getSelectedItem().getText());
                        var row = view.lookup("#merit_navigate-test");
                        assertNotNull(row);
                        assertTrue(row.getStyleClass().contains("pulse-highlight"));
                        return null;
                    });
            Platform.runLater(verify);
            verify.get(10, TimeUnit.SECONDS);
        } finally {
            var cleanup =
                    new java.util.concurrent.FutureTask<Void>(() -> {
                        if (viewRef.get() != null) viewRef.get().cleanup();
                        return null;
                    });
            Platform.runLater(cleanup);
            cleanup.get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    void charmChecklistLinksSelectAndHighlightAbilityAndEvocationTrees() throws Exception {
        var solar = new com.vibethema.model.mystic.SolarCharm();
        solar.setName("Test Navigation Charm");
        solar.setId("navigate-charm");
        solar.setAbility("Melee");
        solar.setMinAbility(5);
        assertCharmIssueNavigates(solar);
        var evocation = new com.vibethema.model.mystic.Evocation();
        evocation.setName("Test Navigation Evocation");
        evocation.setId("navigate-evocation");
        evocation.setArtifactId("missing-artifact");
        assertCharmIssueNavigates(evocation);
    }

    private void assertCharmIssueNavigates(com.vibethema.model.mystic.Charm charm)
            throws Exception {
        java.util.concurrent.atomic.AtomicReference<MainView> viewRef =
                new java.util.concurrent.atomic.AtomicReference<>();
        var setup =
                new java.util.concurrent.FutureTask<Void>(() -> {
                    var data = new CharacterData();
                    data.addCharm(
                            new com.vibethema.model.mystic.PurchasedCharm(
                                    charm.getId(), charm.getName(), charm.getAbility()));
                    var service = Mockito.mock(CharmDataService.class);
                    when(service.findCharmById(charm.getId())).thenReturn(charm);
                    when(service.loadCharmsForAbility("Melee"))
                            .thenReturn(java.util.List.of(charm));
                    when(service.loadEvocations("missing-artifact"))
                            .thenReturn(
                                    new CharmDataService.EvocationCollection(
                                            "missing-artifact",
                                            "Test Artifact",
                                            java.util.List.of(charm)));
                    var vm =
                            new MainViewModel(
                                    data,
                                    Mockito.mock(SystemDataService.class),
                                    Mockito.mock(EquipmentDataService.class),
                                    service,
                                    Mockito.mock(PdfExportService.class));
                    var view =
                            (MainView)
                                    de.saxsys.mvvmfx.FluentViewLoader.javaView(MainView.class)
                                            .viewModel(vm)
                                            .load()
                                            .getView();
                    viewRef.set(view);
                    new javafx.scene.Scene(view, 1200, 850);
                    view.applyCss();
                    view.layout();
                    var issue =
                            vm.getFooterViewModel().getValidationIssues().stream()
                                    .filter(i -> i.code().equals("charm_requirements"))
                                    .findFirst()
                                    .orElseThrow();
                    vm.getFooterViewModel().navigateToIssue(issue);
                    return null;
                });
        Platform.runLater(setup);
        try {
            setup.get(10, TimeUnit.SECONDS);
            for (int i = 0; i < 3; i++) {
                var pulse = new java.util.concurrent.FutureTask<Void>(() -> null);
                Platform.runLater(pulse);
                pulse.get(10, TimeUnit.SECONDS);
            }
            var verify =
                    new java.util.concurrent.FutureTask<Void>(() -> {
                        var view = viewRef.get();
                        var tabs = (javafx.scene.control.TabPane) view.getCenter();
                        assertEquals(
                                "Solar Charms",
                                tabs.getSelectionModel().getSelectedItem().getText());
                        var node = view.lookup("#charm_" + charm.getId());
                        assertNotNull(node);
                        assertTrue(node.getStyleClass().contains("pulse-highlight"));
                        return null;
                    });
            Platform.runLater(verify);
            verify.get(10, TimeUnit.SECONDS);
        } finally {
            var cleanup =
                    new java.util.concurrent.FutureTask<Void>(() -> {
                        if (viewRef.get() != null) viewRef.get().cleanup();
                        return null;
                    });
            Platform.runLater(cleanup);
            cleanup.get(10, TimeUnit.SECONDS);
        }
    }
}
