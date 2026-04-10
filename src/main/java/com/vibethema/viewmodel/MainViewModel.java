package com.vibethema.viewmodel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vibethema.model.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.traits.*;
import com.vibethema.service.CharmDataService;
import com.vibethema.service.EquipmentDataService;
import com.vibethema.service.PathService;
import com.vibethema.service.PdfExportService;
import com.vibethema.service.SystemDataService;
import com.vibethema.service.UndoManager;
import com.vibethema.viewmodel.charms.CharmsViewModel;
import com.vibethema.viewmodel.equipment.EquipmentViewModel;
import com.vibethema.viewmodel.experience.ExperienceViewModel;
import com.vibethema.viewmodel.footer.FooterViewModel;
import com.vibethema.viewmodel.util.Messenger;
import de.saxsys.mvvmfx.ViewModel;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.PauseTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainViewModel implements ViewModel {
    public record NavigationTarget(String tabName, String filterType, String filterValue) {}

    public record CasteChangeReport(
            List<Ability> lostCasteAbilities, String lostSupernal, List<String> illegalCharmNames) {
        public boolean isEmpty() {
            return lostCasteAbilities.isEmpty()
                    && lostSupernal == null
                    && illegalCharmNames.isEmpty();
        }
    }

    public record CheckpointRequest(String contextId, String description, String targetId) {}

    private static final Logger logger = LoggerFactory.getLogger(MainViewModel.class);
    private CharacterData data;
    private FooterViewModel footerViewModel;
    private final EquipmentDataService equipmentService;
    private final CharmDataService charmDataService;
    private final PdfExportService pdfExportService;
    private final SystemDataService systemDataService;
    private final UndoManager undoManager = new UndoManager();
    private final StringProperty currentTabId = new SimpleStringProperty("Stats");

    private final Map<String, String> tagDescriptions =
            new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, String> keywordDefs = new java.util.concurrent.ConcurrentHashMap<>();

    private StatsViewModel statsViewModel;
    private MeritsViewModel meritsViewModel;
    private IntimaciesViewModel intimaciesViewModel;
    private SorceryViewModel sorceryViewModel;
    private EquipmentViewModel equipmentViewModel;
    private ExperienceViewModel experienceViewModel;
    private final Map<String, CharmsViewModel> charmViewModels = new HashMap<>();

    // Export Messages
    public static final String EXPORT_SUCCESS_TITLE = "Export Successful";
    public static final String EXPORT_SUCCESS_MSG = "Character sheet exported successfully!";
    public static final String EXPORT_FAILURE_TITLE = "Export Failed";

    private final ObjectProperty<File> currentFile = new SimpleObjectProperty<>();
    private final StringProperty windowTitle = new SimpleStringProperty("Vibethema");
    private final BooleanProperty dirty = new SimpleBooleanProperty();
    private final BooleanProperty coreDataImported = new SimpleBooleanProperty();
    public static final String FINALIZATION_DIALOG_TITLE = "Finalize Character";
    public static final String FINALIZATION_DIALOG_HEADER = "Finalizing Character Creation";
    public static final String FINALIZATION_DIALOG_CONTENT =
            "Once finalized, your Caste, Supernal, and Caste/Favored abilities cannot be"
                    + " changed. This is a one-way process.\n\n"
                    + "Proceed to Experienced mode?";

    public MainViewModel() {
        this(new EquipmentDataService(), new CharmDataService(), new PdfExportService());
    }

    public MainViewModel(
            EquipmentDataService equipmentService,
            CharmDataService charmDataService,
            PdfExportService pdfExportService) {
        this(
                CharacterFactory.createNewCharacter(),
                new SystemDataService(),
                equipmentService,
                charmDataService,
                pdfExportService);
    }

    public MainViewModel(CharacterData data) {
        this(
                data,
                new SystemDataService(),
                new EquipmentDataService(),
                new CharmDataService(),
                new PdfExportService());
    }

    public MainViewModel(CharacterData data, SystemDataService systemDataService) {
        this(
                data,
                systemDataService,
                new EquipmentDataService(),
                new CharmDataService(),
                new PdfExportService());
    }

    public MainViewModel(
            CharacterData data,
            SystemDataService systemDataService,
            EquipmentDataService equipmentService,
            CharmDataService charmDataService,
            PdfExportService pdfExportService) {
        this.equipmentService = equipmentService;
        this.charmDataService = charmDataService;
        this.pdfExportService = pdfExportService;
        this.systemDataService = systemDataService;
        init(data);
        coreDataImported.set(systemDataService.isCoreDataImported());
    }

    public void init(CharacterData data) {
        this.data = data;
        this.footerViewModel = new FooterViewModel(data, this::handleFinalization);
        this.dirty.unbind();
        this.dirty.bind(data.dirtyProperty());

        new Thread(
                        () -> {
                            // Load global data
                            loadTagDescriptions();
                            loadKeywords();

                            // Load character-specific defaults if this is a new character (no
                            // weapons yet)
                            if (data.getWeapons().isEmpty()) {
                                new CharacterFactory()
                                        .initializeDefaultEquipment(data, equipmentService);
                            }
                        },
                        "MainViewModel-Data-Loader")
                .start();

        updateWindowTitle();
        currentFile.addListener((obs, oldV, newV) -> updateWindowTitle());
        dirty.addListener((obs, oldV, newV) -> updateWindowTitle());

        Messenger.subscribe("request_charm_creation", this::handleCharmCreationRequest);
        Messenger.subscribe(
                "RECORD_UNDO_CHECKPOINT",
                (name, payload) -> {
                    if (payload != null
                            && payload.length > 0
                            && payload[0] instanceof CheckpointRequest req) {
                        undoManager.pushCheckpoint(
                                data.exportState(), req.contextId, req.description, req.targetId);
                    }
                });

        setupDebouncedCheckpoint(data.nameProperty(), "Info", "Change Name", "info_name");
        setupDebouncedCheckpoint(
                data.limitTriggerProperty(),
                "Stats",
                "Change Limit Trigger",
                "stats_limit_trigger");

        setupSyncListeners(data);
    }

    private void setupSyncListeners(CharacterData data) {
        // Listen to all abilities and attributes for UI-wide refresh (e.g., Charms eligibility)
        data.getAbilities().values().forEach(p -> p.addListener((obs, ov, nv) -> notifyRefresh()));
        data.getAttributes().values().forEach(p -> p.addListener((obs, ov, nv) -> notifyRefresh()));

        data.essenceProperty().addListener((obs, ov, nv) -> notifyRefresh());
        data.willpowerProperty().addListener((obs, ov, nv) -> notifyRefresh());
        data.casteProperty().addListener((obs, ov, nv) -> notifyRefresh());
        data.supernalAbilityProperty().addListener((obs, ov, nv) -> notifyRefresh());

        // Also listen to Caste/Favored status changes
        data.getCasteAbilities()
                .values()
                .forEach(p -> p.addListener((obs, ov, nv) -> notifyRefresh()));
        data.getFavoredAbilities()
                .values()
                .forEach(p -> p.addListener((obs, ov, nv) -> notifyRefresh()));
    }

    private void notifyRefresh() {
        if (!data.isImporting()) {
            Messenger.publish("refresh_all_ui");
        }
    }

    private void setupDebouncedCheckpoint(
            StringProperty property, String contextId, String description, String targetId) {
        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(
                e -> {
                    undoManager.pushCheckpoint(
                            data.exportState(), contextId, description, targetId);
                });

        property.addListener(
                (obs, oldV, newV) -> {
                    // We only want to trigger if it wasn't a restored state (undo/redo)
                    // But UndoManager handles that by checking if the state is already current.
                    // Actually, we should probably check if the value actually changed to avoid
                    // redundant restarts.
                    if (oldV != null && !oldV.equals(newV)) {
                        pause.playFromStart();
                    }
                });
    }

    private void loadTagDescriptions() {
        Map<String, List<EquipmentDataService.Tag>> allTags = equipmentService.loadEquipmentTags();
        if (allTags != null) {
            for (List<EquipmentDataService.Tag> list : allTags.values()) {
                if (list != null) {
                    for (EquipmentDataService.Tag t : list) {
                        tagDescriptions.put(t.getName(), t.getDescription());
                    }
                }
            }
        }
    }

    private void loadKeywords() {
        List<com.vibethema.model.mystic.Keyword> keywords = charmDataService.loadKeywords();
        for (com.vibethema.model.mystic.Keyword kw : keywords) {
            keywordDefs.put(kw.getName(), kw.getDescription());
        }
    }

    private void updateWindowTitle() {
        String title = "Vibethema";
        if (currentFile.get() != null) {
            title += " - " + currentFile.get().getName();
        }
        if (dirty.get()) {
            title += "*";
        }
        windowTitle.set(title);
    }

    // Accessors
    public CharacterData getData() {
        return data;
    }

    public FooterViewModel getFooterViewModel() {
        return footerViewModel;
    }

    public EquipmentDataService getEquipmentService() {
        return equipmentService;
    }

    public CharmDataService getCharmDataService() {
        return charmDataService;
    }

    public Map<String, String> getTagDescriptions() {
        return tagDescriptions;
    }

    public Map<String, String> getKeywordDefs() {
        return keywordDefs;
    }

    public StringProperty windowTitleProperty() {
        return windowTitle;
    }

    public ObjectProperty<File> currentFileProperty() {
        return currentFile;
    }

    public BooleanProperty dirtyProperty() {
        return dirty;
    }

    public BooleanProperty coreDataImportedProperty() {
        return coreDataImported;
    }

    public StringProperty currentTabIdProperty() {
        return currentTabId;
    }

    public javafx.beans.property.ReadOnlyBooleanProperty canUndoProperty() {
        return undoManager.canUndoProperty();
    }

    public javafx.beans.property.ReadOnlyBooleanProperty canRedoProperty() {
        return undoManager.canRedoProperty();
    }

    // Actions
    public void saveCharacter(File file) {
        if (file == null) return;
        try (FileWriter writer = new FileWriter(file)) {
            CharacterSaveState state = data.exportState();
            new GsonBuilder().setPrettyPrinting().create().toJson(state, writer);
            data.setDirty(false);
            currentFile.set(file);
        } catch (Exception ex) {
            logger.error("Failed to save character to {}", file.getAbsolutePath(), ex);
        }
    }

    public void loadCharacter(File file) {
        if (file == null) return;
        try (FileReader reader = new FileReader(file)) {
            CharacterSaveState state = new Gson().fromJson(reader, CharacterSaveState.class);
            if (state != null) {
                data.importState(state, equipmentService);
                currentFile.set(file);
                data.setDirty(false);
                undoManager.clear();
            }
        } catch (Exception ex) {
            logger.error("Failed to load character from {}", file.getAbsolutePath(), ex);
        }
    }

    public void undo() {
        if (undoManager.canUndoProperty().get()) {
            UndoManager.UndoEntry entry = undoManager.undo(currentTabId.get(), data.exportState());
            if (entry != null) {
                if (!entry.contextId().equals(currentTabId.get())) {
                    jumpToContext(entry.contextId(), entry.state());
                }
                data.restoreState(entry.state(), equipmentService);
                handlePostHistoryAction(entry);
            }
        }
    }

    public void redo() {
        if (undoManager.canRedoProperty().get()) {
            UndoManager.UndoEntry entry = undoManager.redo(currentTabId.get(), data.exportState());
            if (entry != null) {
                if (!entry.contextId().equals(currentTabId.get())) {
                    jumpToContext(entry.contextId(), entry.state());
                }
                data.restoreState(entry.state(), equipmentService);
                handlePostHistoryAction(entry);
            }
        }
    }

    private void handlePostHistoryAction(UndoManager.UndoEntry entry) {
        if (entry.targetId() != null) {
            String currentTab = currentTabId.get();
            // If we are already in the correct context, trigger immediately.
            // Otherwise, keep a small delay to allow tab switching/rendering to complete.
            if (entry.contextId() != null && entry.contextId().equals(currentTab)) {
                Messenger.publish("highlight_element", entry.targetId());
            } else {
                PauseTransition delay = new PauseTransition(Duration.millis(100));
                delay.setOnFinished(e -> Messenger.publish("highlight_element", entry.targetId()));
                delay.play();
            }
        }
    }

    private void jumpToContext(String contextId, CharacterSaveState state) {
        // Find if it's a specific ability in Charms
        if (contextId.startsWith("Charms:")) {
            String ability = contextId.substring(7);
            Messenger.publish("jump_to_charms", ability);
        } else {
            // Otherwise try a direct tab jump
            Messenger.publish("switch_to_tab", contextId);
        }
    }

    private void handleFinalization() {
        Messenger.publish("show_finalization_dialog");
    }

    public void proceedWithFinalization() {
        data.setCreationSnapshot(data.exportState());
        data.setMode(CharacterMode.EXPERIENCED);
        if (currentFile.get() != null) {
            saveCharacter(currentFile.get());
        }
    }

    public void onNewCharacterRequest() {
        if (!coreDataImported.get()) return;
        if (dirty.get()) {
            Messenger.publish("confirm_discard_changes", "NEW");
        } else {
            resetToNew();
        }
    }

    public void onSaveRequest() {
        if (currentFile.get() != null) {
            saveCharacter(currentFile.get());
        } else {
            String suggestName =
                    data.nameProperty().get().trim().isEmpty()
                            ? "Character.vbtm"
                            : data.nameProperty().get().trim() + ".vbtm";
            Messenger.publish("request_save_as", suggestName);
        }
    }

    public void onLoadRequest() {
        if (!coreDataImported.get()) return;
        if (dirty.get()) {
            Messenger.publish("confirm_discard_changes", "LOAD");
        } else {
            Messenger.publish("request_load_file");
        }
    }

    public void onExportPdfRequest() {
        String suggestName = "Character.pdf";
        if (currentFile.get() != null) {
            suggestName = currentFile.get().getName().replace(".vbtm", ".pdf");
        }
        Messenger.publish("request_pdf_save_location", suggestName);
    }

    public void onExportCharmsPdfRequest() {
        String suggestName = "Charms_Spells.pdf";
        if (currentFile.get() != null) {
            suggestName = currentFile.get().getName().replace(".vbtm", "_Charms.pdf");
        }
        Messenger.publish("request_charms_pdf_save_location", suggestName);
    }

    public void exportToPdf(File file) {
        if (file == null) return;
        try {
            pdfExportService.exportToPdf(data, file);
            Messenger.publish("pdf_export_success", EXPORT_SUCCESS_MSG);
        } catch (Exception ex) {
            logger.error("Failed to export PDF to {}: {}", file.getAbsolutePath(), ex.getMessage());
            Messenger.publish("pdf_export_error", "Export failed: " + ex.getMessage());
        }
    }

    public void exportCharmsToPdf(File file) {
        if (file == null) return;
        try {
            pdfExportService.exportCharmsToPdf(data, file, charmDataService);
            Messenger.publish("pdf_export_success", "Charms and Spells exported successfully!");
        } catch (Exception ex) {
            logger.error(
                    "Failed to export Charms PDF to {}: {}",
                    file.getAbsolutePath(),
                    ex.getMessage());
            Messenger.publish("pdf_export_error", "Export failed: " + ex.getMessage());
        }
    }

    /**
     * Point 3: Model Manipulation & Entity Creation Factory logic for creating a new custom charm
     * based on the current context.
     */
    public com.vibethema.model.mystic.Charm prepareNewCustomCharm(
            String abilityId, String filterType) {
        com.vibethema.model.mystic.Charm charm =
                "Martial Arts Style".equals(filterType)
                        ? new com.vibethema.model.mystic.MartialArtsCharm()
                        : new com.vibethema.model.mystic.SolarCharm();

        charm.setId(java.util.UUID.randomUUID().toString());
        charm.setName("New " + ("Evocation".equals(filterType) ? "Evocation" : "Charm"));
        charm.setAbility(abilityId);
        charm.setCategory("Evocation".equals(filterType) ? "evocation" : "charm");
        charm.setCustom(true);

        return charm;
    }

    private void handleCharmCreationRequest(String name, Object[] payload) {
        if (payload == null || payload.length < 4) return;
        String abilityId = (String) payload[0];
        String contextName = (String) payload[1];
        String filterType = (String) payload[2];
        Runnable onSave = (Runnable) payload[3];

        com.vibethema.model.mystic.Charm charm = prepareNewCustomCharm(abilityId, filterType);
        Messenger.publish(
                "open_edit_charm_dialog", new Object[] {charm, contextName, filterType, onSave});
    }

    public NavigationTarget resolveNavigationTarget(String abilityName) {
        String filterType = "Ability";
        String tabName = "Solar Charms";
        if (data.isMartialArtsStyle(abilityName)) {
            filterType = "Martial Arts Style";
            tabName = "Martial Arts";
        }
        return new NavigationTarget(tabName, filterType, abilityName);
    }

    public NavigationTarget resolveEvocationTarget(String artifactName) {
        return new NavigationTarget("Solar Charms", "Ability", artifactName);
    }

    // ── Sub-ViewModel Factories ──────────────────────────────────────────────

    public StatsViewModel getStatsViewModel() {
        if (statsViewModel == null) {
            statsViewModel = new StatsViewModel(data);
        }
        return statsViewModel;
    }

    public MeritsViewModel getMeritsViewModel() {
        if (meritsViewModel == null) {
            meritsViewModel = new MeritsViewModel(data);
        }
        return meritsViewModel;
    }

    public IntimaciesViewModel getIntimaciesViewModel() {
        if (intimaciesViewModel == null) {
            intimaciesViewModel = new IntimaciesViewModel(data);
        }
        return intimaciesViewModel;
    }

    public SorceryViewModel getSorceryViewModel() {
        if (sorceryViewModel == null) {
            sorceryViewModel = new SorceryViewModel(data, charmDataService);
        }
        return sorceryViewModel;
    }

    public CharmsViewModel getCharmsViewModel(String filterType) {
        return charmViewModels.computeIfAbsent(
                filterType, ft -> new CharmsViewModel(data, charmDataService, keywordDefs, ft));
    }

    public EquipmentViewModel getEquipmentViewModel() {
        if (equipmentViewModel == null) {
            equipmentViewModel =
                    new EquipmentViewModel(
                            data, equipmentService, charmDataService, tagDescriptions, null);
        }
        return equipmentViewModel;
    }

    public ExperienceViewModel getExperienceViewModel(Runnable refreshFooter) {
        if (experienceViewModel == null) {
            experienceViewModel = new ExperienceViewModel(data, refreshFooter);
        }
        return experienceViewModel;
    }

    public CasteChangeReport validateCasteChange(Caste newCaste) {
        Caste oldCaste = data.casteProperty().get();
        if (newCaste == oldCaste) {
            return new CasteChangeReport(Collections.emptyList(), null, Collections.emptyList());
        }

        List<Ability> lostCasteAbilities = new ArrayList<>();
        String supernalLossDetail = null;
        List<String> illegalCharms = new ArrayList<>();

        // 1. Identify Invalid Caste Abilities
        for (Ability abil : SystemData.ABILITIES) {
            if (data.getCasteAbility(abil).get()) {
                if (!SystemData.CASTE_OPTIONS.get(newCaste).contains(abil)) {
                    lostCasteAbilities.add(abil);
                }
            }
        }

        // 2. Identify Supernal Loss
        String currentSupernal = data.supernalAbilityProperty().get();
        boolean supernalWillBeLost = false;
        if (!currentSupernal.isEmpty()) {
            supernalWillBeLost =
                    lostCasteAbilities.stream()
                            .anyMatch(a -> a.getDisplayName().equalsIgnoreCase(currentSupernal));
            if (supernalWillBeLost) {
                supernalLossDetail = currentSupernal;
            }
        }

        // 3. Identify Illegal Charms
        if (data.getMode() == CharacterMode.CREATION) {
            int essenceLevel = data.essenceProperty().get();
            for (com.vibethema.model.mystic.PurchasedCharm pc : data.getUnlockedCharms()) {
                List<com.vibethema.model.mystic.Charm> abilityCharms =
                        charmDataService.loadCharmsForAbility(pc.ability());
                com.vibethema.model.mystic.Charm def =
                        abilityCharms.stream()
                                .filter(c -> c.getId().equals(pc.id()))
                                .findFirst()
                                .orElse(null);

                if (def != null && def.getMinEssence() > essenceLevel) {
                    boolean wouldBeSupernal =
                            !supernalWillBeLost
                                    && def.getAbility().equalsIgnoreCase(currentSupernal);
                    if (!wouldBeSupernal && !currentSupernal.isEmpty()) {
                        if ("Martial Arts".equalsIgnoreCase(currentSupernal)
                                && data.isMartialArtsStyle(def.getAbility()))
                            wouldBeSupernal = true;
                        if ("Craft".equalsIgnoreCase(currentSupernal)
                                && data.isCraftExpertise(def.getAbility())) wouldBeSupernal = true;
                    }

                    if (!wouldBeSupernal) {
                        illegalCharms.add(pc.name());
                    }
                }
            }
        }

        return new CasteChangeReport(lostCasteAbilities, supernalLossDetail, illegalCharms);
    }

    public String generateCasteChangeWarning(Caste newCaste, CasteChangeReport report) {
        StringBuilder content =
                new StringBuilder("Changing Caste to ")
                        .append(newCaste.toString())
                        .append(" will invalidate some data.\n\n")
                        .append("The following selections will be removed/deselected:\n\n");

        if (!report.lostCasteAbilities().isEmpty()) {
            content.append("Caste Abilities: ")
                    .append(
                            report.lostCasteAbilities().stream()
                                    .map(Ability::getDisplayName)
                                    .collect(java.util.stream.Collectors.joining(", ")))
                    .append("\n");
        }
        if (report.lostSupernal() != null) {
            content.append("Supernal Ability: ")
                    .append(report.lostSupernal())
                    .append(" (no longer a Caste ability)\n");
        }
        if (!report.illegalCharmNames().isEmpty()) {
            content.append("Invalid Charms (Essence requirement): ")
                    .append(String.join(", ", report.illegalCharmNames()))
                    .append("\n");
        }
        content.append("\nContinue with Caste change?");
        return content.toString();
    }

    public void applyCasteChange(Caste newCaste, CasteChangeReport report) {
        data.casteProperty().set(newCaste);
        // Cleanup based on report
        if (report != null) {
            for (Ability abil : report.lostCasteAbilities()) {
                data.getCasteAbility(abil).set(false);
            }
            if (!report.illegalCharmNames().isEmpty()) {
                data.getUnlockedCharms()
                        .removeIf(pc -> report.illegalCharmNames().contains(pc.name()));
            }
        }
        Messenger.publish("refresh_all_ui");
    }

    public void resetToNew() {
        this.data.clear();
        Weapon unarmed = equipmentService.loadWeapon(Weapon.UNARMED_ID);
        if (unarmed != null) {
            data.getWeapons().add(unarmed.copy());
        }
        this.currentFile.set(null);
        this.data.setDirty(false);
        Messenger.publish("refresh_all_ui");
    }

    public void refreshImportStatus() {
        coreDataImported.set(systemDataService.isCoreDataImported());
        Messenger.publish("refresh_all_ui");
    }

    public void showDatabaseStats() {
        int charms = charmDataService.getGlobalCharmCount();
        int spells = charmDataService.getGlobalSpellCount();
        int equipment = equipmentService.getTotalEquipmentCount();

        String msg =
                String.format(
                        "Database Statistics:\n\n"
                                + "• Charms Registered: %d\n"
                                + "• Spells Known: %d\n"
                                + "• Equipment Items: %d\n\n"
                                + "All data is loaded from your local application data directory.",
                        charms, spells, equipment);

        Messenger.publish("show_info_alert", new Object[] {"Database Statistics", msg});
    }

    public void openDataDirectory() {
        java.io.File dataDir = PathService.getDataPath().toFile();
        if (java.awt.Desktop.isDesktopSupported()) {
            try {
                java.awt.Desktop.getDesktop().open(dataDir);
            } catch (Exception e) {
                logger.error("Failed to open data directory", e);
                Messenger.publish(
                        "show_error_alert", "Could not open directory: " + e.getMessage());
            }
        } else {
            Messenger.publish(
                    "show_info_alert",
                    new Object[] {
                        "Data Directory", "Your data is located at:\n" + dataDir.getAbsolutePath()
                    });
        }
    }

    public void checkMissingData() {
        List<String> missing = new ArrayList<>();
        if (!systemDataService.isCoreDataImported()) {
            missing.add("Core Book Data (keywords.json missing)");
        }

        // Basic check for some common solar files
        java.nio.file.Path charmsDir = PathService.getDataPath().resolve("charms");
        String[] required = {"archery.json", "melee.json", "brawl.json", "integrity.json"};
        for (String file : required) {
            if (!java.nio.file.Files.exists(charmsDir.resolve(file))) {
                missing.add("Solar " + file.replace(".json", "") + " charms");
            }
        }

        if (missing.isEmpty()) {
            Messenger.publish(
                    "show_info_alert",
                    new Object[] {
                        "Data Check",
                        "All essential core data seems to be present and accounted for!"
                    });
        } else {
            String report =
                    "The following data files are missing:\n\n• " + String.join("\n• ", missing);
            report += "\n\nYou may need to re-import your Core PDF via the Import menu.";
            Messenger.publish(
                    "show_info_alert", new Object[] {"Data Check - Issues Found", report});
        }
    }

    public void showAboutDialog() {
        String msg =
                "Vibethema - Exalted 3rd Edition Solar Builder\n"
                    + "Version: 1.0.0-SNAPSHOT\n\n"
                    + "A character creation and management tool for Solar Exalted.\n"
                    + "Designed for ease of use and rapid character drafting.\n\n"
                    + "Built with JavaFX and MVVM-FX.\n"
                    + "Licensed under GNU GPL v3.0.\n\n"
                    + "--- LEGAL ---\n"
                    + "Vibethema is not official Exalted material.\n\n"
                    + " information please visit worldofdarkness.com.\n\n"
                    + "--- CREDITS ---\n"
                    + "Includes Libertinus Serif font by Khaled Hosny, licensed under the SIL Open Font"
                    + " License.";
        Messenger.publish("show_about_dialog", new Object[] {"About Vibethema", msg});
    }
}
