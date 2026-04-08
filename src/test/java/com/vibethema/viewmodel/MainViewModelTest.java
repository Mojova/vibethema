package com.vibethema.viewmodel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.vibethema.model.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.traits.*;
import com.vibethema.service.CharmDataService;
import com.vibethema.service.EquipmentDataService;
import com.vibethema.service.PdfExportService;
import com.vibethema.service.SystemDataService;
import com.vibethema.viewmodel.util.Messenger;
import de.saxsys.mvvmfx.utils.notifications.NotificationObserver;
import java.util.Collections;
import java.util.HashMap;
import com.vibethema.viewmodel.charms.CharmsViewModel;
import com.vibethema.viewmodel.equipment.EquipmentViewModel;
import com.vibethema.viewmodel.experience.ExperienceViewModel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import java.nio.file.Path;

public class MainViewModelTest {

    @TempDir
    Path tempDir;

    private MainViewModel viewModel;
    private CharacterData data;
    private EquipmentDataService equipmentService;
    private CharmDataService charmDataService;
    private SystemDataService systemDataService;
    private PdfExportService pdfExportService;

    @BeforeEach
    void setUp() {
        data = new CharacterData();
        systemDataService = Mockito.mock(SystemDataService.class);
        equipmentService = Mockito.mock(EquipmentDataService.class);
        charmDataService = Mockito.mock(CharmDataService.class);
        pdfExportService = Mockito.mock(PdfExportService.class);

        // By default, assume core data is imported for these tests
        when(systemDataService.isCoreDataImported()).thenReturn(true);
        // Important: loadWeapon should be stubbed to return null (or a mock) to avoid race
        // conditions
        // with the background thread during mock setup for other methods.
        when(equipmentService.loadWeapon(anyString())).thenReturn(null);
        when(equipmentService.loadEquipmentTags()).thenReturn(new HashMap<>());
        when(charmDataService.loadKeywords()).thenReturn(Collections.emptyList());

        viewModel =
                new MainViewModel(
                        data,
                        systemDataService,
                        equipmentService,
                        charmDataService,
                        pdfExportService);
    }

    @Test
    void testAsyncDataLoading() throws InterruptedException {
        // Prepare mock data
        Map<String, List<EquipmentDataService.Tag>> mockTags = new HashMap<>();
        mockTags.put(
                "weapon", List.of(new EquipmentDataService.Tag("Lethal", "Causes lethal damage")));

        com.vibethema.model.mystic.Keyword mockKeyword = new com.vibethema.model.mystic.Keyword();
        mockKeyword.setName("Aggravated");
        mockKeyword.setDescription("Causes aggravated damage");

        // Use doReturn for cleaner thread safety if the mock is being accessed concurrently
        doReturn(mockTags).when(equipmentService).loadEquipmentTags();
        doReturn(List.of(mockKeyword)).when(charmDataService).loadKeywords();

        // Trigger background loading by creating ViewModel
        viewModel =
                new MainViewModel(
                        data,
                        systemDataService,
                        equipmentService,
                        charmDataService,
                        Mockito.mock(PdfExportService.class));

        // Wait briefly for the background thread to complete
        int attempts = 0;
        while ((viewModel.getTagDescriptions().isEmpty() || viewModel.getKeywordDefs().isEmpty())
                && attempts < 10) {
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
        Messenger.subscribe("request_pdf_save_location", observer);

        try {
            viewModel.onExportPdfRequest();
            assertNotNull(receivedPayload.get());
            assertEquals("Hero.pdf", receivedPayload.get()[0]);
        } finally {
            Messenger.unsubscribe("request_pdf_save_location", observer);
        }
    }

    @Test
    void testResetToNewPreservesObjectIdentity() {
        CharacterData originalData = viewModel.getData();
        viewModel.getData().nameProperty().set("ToBeCleared");

        viewModel.resetToNew();

        assertSame(
                originalData,
                viewModel.getData(),
                "CharacterData instance must be preserved for bindings");
        assertEquals("", viewModel.getData().nameProperty().get());
    }

    @Test
    void testValidateCasteChangeNoConflicts() {
        data.casteProperty().set(Caste.DAWN);
        MainViewModel.CasteChangeReport report = viewModel.validateCasteChange(Caste.ZENITH);

        assertTrue(report.isEmpty());
        assertEquals(0, report.lostCasteAbilities().size());
        assertNull(report.lostSupernal());
        assertTrue(report.illegalCharmNames().isEmpty());
    }

    @Test
    void testValidateCasteChangeWithAbilityLoss() {
        data.casteProperty().set(Caste.DAWN);
        data.getCasteAbility(Ability.ARCHERY).set(true); // Dawn ability
        data.getCasteAbility(Ability.MELEE).set(true); // Dawn ability

        // Zenith does not have Archery or Melee in its list
        MainViewModel.CasteChangeReport report = viewModel.validateCasteChange(Caste.ZENITH);

        assertFalse(report.isEmpty());
        assertEquals(2, report.lostCasteAbilities().size());
        assertTrue(report.lostCasteAbilities().contains(Ability.ARCHERY));
        assertTrue(report.lostCasteAbilities().contains(Ability.MELEE));
    }

    @Test
    void testValidateCasteChangeWithSupernalAndCharmLoss() {
        data.casteProperty().set(Caste.DAWN);
        data.getCasteAbility(Ability.ARCHERY).set(true);
        data.supernalAbilityProperty().set("Archery");
        data.essenceProperty().set(1);

        // Add a charm that requires Essence 3
        PurchasedCharm pc = new PurchasedCharm("charm-1", "Blazing Solar Bolt", "Archery");
        data.getUnlockedCharms().add(pc);

        SolarCharm def = new SolarCharm();
        def.setId("charm-1");
        def.setName("Blazing Solar Bolt");
        def.setAbility("Archery");
        def.setMinEssence(3);

        when(charmDataService.loadCharmsForAbility("Archery")).thenReturn(List.of(def));

        // Change to Zenith (Archery is not a Zenith ability)
        MainViewModel.CasteChangeReport report = viewModel.validateCasteChange(Caste.ZENITH);

        assertEquals("Archery", report.lostSupernal());
        assertTrue(report.illegalCharmNames().contains("Blazing Solar Bolt"));
    }

    @Test
    void testApplyCasteChangeCleansUpModel() {
        data.casteProperty().set(Caste.DAWN);
        data.getCasteAbility(Ability.ARCHERY).set(true);
        data.supernalAbilityProperty().set("Archery");
        data.getUnlockedCharms()
                .add(new PurchasedCharm("charm-1", "Blazing Solar Bolt", "Archery"));

        List<Ability> lostAbilities = List.of(Ability.ARCHERY);
        List<String> illegalCharms = List.of("Blazing Solar Bolt");
        MainViewModel.CasteChangeReport report =
                new MainViewModel.CasteChangeReport(lostAbilities, "Archery", illegalCharms);

        viewModel.applyCasteChange(Caste.ZENITH, report);

        assertEquals(Caste.ZENITH, data.casteProperty().get());
        assertFalse(data.getCasteAbility(Ability.ARCHERY).get());
        assertTrue(data.getUnlockedCharms().isEmpty());
    }
    @Test
    void testGenerateCasteChangeWarning() {
        List<Ability> lostAbilities = List.of(Ability.ARCHERY, Ability.MELEE);
        List<String> illegalCharms = List.of("Blazing Solar Bolt");
        MainViewModel.CasteChangeReport report =
                new MainViewModel.CasteChangeReport(lostAbilities, "Archery", illegalCharms);

        String warning = viewModel.generateCasteChangeWarning(Caste.ZENITH, report);

        assertTrue(warning.contains("Zenith"));
        assertTrue(warning.contains("Archery"));
        assertTrue(warning.contains("Melee"));
        assertTrue(warning.contains("Blazing Solar Bolt"));
        assertTrue(warning.contains("no longer a Caste ability"));
    }

    @Test
    void testExportToPdfSuccess() throws java.io.IOException {
        java.io.File mockFile = tempDir.resolve("test.pdf").toFile();
        AtomicReference<String> receivedMessage = new AtomicReference<>();
        NotificationObserver observer = (name, payload) -> receivedMessage.set(name);
        Messenger.subscribe("pdf_export_success", observer);

        try {
            viewModel.exportToPdf(mockFile);
            Mockito.verify(pdfExportService).exportToPdf(data, mockFile);
            assertEquals("pdf_export_success", receivedMessage.get());
        } finally {
            Messenger.unsubscribe("pdf_export_success", observer);
        }
    }

    @Test
    void testExportToPdfFailure() throws java.io.IOException {
        java.io.File mockFile = tempDir.resolve("fail.pdf").toFile();
        Mockito.doThrow(new java.io.IOException("Disk Full"))
                .when(pdfExportService)
                .exportToPdf(data, mockFile);

        AtomicReference<String> receivedMessage = new AtomicReference<>();
        NotificationObserver observer = (name, payload) -> receivedMessage.set(name);
        Messenger.subscribe("pdf_export_error", observer);

        try {
            viewModel.exportToPdf(mockFile);
            assertEquals("pdf_export_error", receivedMessage.get());
        } finally {
            Messenger.unsubscribe("pdf_export_error", observer);
        }
    }

    @Test
    void testPrepareNewCustomCharmSolar() {
        Charm charm = viewModel.prepareNewCustomCharm("Athletics", "Ability");
        assertNotNull(charm);
        assertTrue(charm instanceof SolarCharm);
        assertEquals("Athletics", charm.getAbility());
        assertEquals("charm", charm.getCategory());
        assertTrue(charm.isCustom());
        assertTrue(charm.getName().startsWith("New Charm"));
    }

    @Test
    void testPrepareNewCustomCharmMartialArts() {
        Charm charm = viewModel.prepareNewCustomCharm("Single Point", "Martial Arts Style");
        assertNotNull(charm);
        assertTrue(charm instanceof MartialArtsCharm);
        assertEquals("Single Point", charm.getAbility());
        assertEquals("charm", charm.getCategory());
        assertTrue(charm.isCustom());
    }

    @Test
    void testPrepareNewCustomCharmEvocation() {
        Charm charm = viewModel.prepareNewCustomCharm("Volcano Cutter", "Evocation");
        assertNotNull(charm);
        assertTrue(charm instanceof SolarCharm);
        assertEquals("Volcano Cutter", charm.getAbility());
        assertEquals("evocation", charm.getCategory());
        assertTrue(charm.getName().startsWith("New Evocation"));
    }

    @Test
    void testResolveNavigationTargetSolar() {
        MainViewModel.NavigationTarget target = viewModel.resolveNavigationTarget("Athletics");
        assertEquals("Solar Charms", target.tabName());
        assertEquals("Athletics", target.filterValue());
    }

    @Test
    void testResolveNavigationTargetMartialArts() {
        CharacterData data = viewModel.getData();
        data.getMartialArtsStyles()
                .add(new com.vibethema.model.traits.MartialArtsStyle("ma1", "Single Point", 1));

        MainViewModel.NavigationTarget target = viewModel.resolveNavigationTarget("Single Point");
        assertEquals("Martial Arts", target.tabName());
        assertEquals("Single Point", target.filterValue());
    }

    @Test
    void testResolveEvocationTarget() {
        MainViewModel.NavigationTarget target = viewModel.resolveEvocationTarget("Volcano Cutter");
        assertEquals("Solar Charms", target.tabName());
        assertEquals("Volcano Cutter", target.filterValue());
    }

    @Test
    void testSubViewModelFactories() {
        // Verify lazy loading and caching for various sub-ViewModels
        StatsViewModel s1 = viewModel.getStatsViewModel();
        StatsViewModel s2 = viewModel.getStatsViewModel();
        assertNotNull(s1);
        assertSame(s1, s2, "StatsViewModel should be cached");

        MeritsViewModel m1 = viewModel.getMeritsViewModel();
        assertNotNull(m1);
        assertSame(m1, viewModel.getMeritsViewModel());

        CharmsViewModel c1 = viewModel.getCharmsViewModel("Ability");
        CharmsViewModel c2 = viewModel.getCharmsViewModel("Ability");
        CharmsViewModel c3 = viewModel.getCharmsViewModel("Martial Arts Style");
        assertNotNull(c1);
        assertSame(c1, c2, "CharmsViewModel for the same filter should be cached");
        assertNotSame(c1, c3, "CharmsViewModel for different filters should be separate");

        EquipmentViewModel e1 = viewModel.getEquipmentViewModel();
        assertNotNull(e1);
        assertSame(e1, viewModel.getEquipmentViewModel());

        ExperienceViewModel exp1 = viewModel.getExperienceViewModel(() -> {});
        assertNotNull(exp1);
        assertSame(exp1, viewModel.getExperienceViewModel(() -> {}));
    }
}
