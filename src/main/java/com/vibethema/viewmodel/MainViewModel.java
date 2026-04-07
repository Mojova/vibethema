package com.vibethema.viewmodel;

import com.vibethema.model.*;
import com.vibethema.service.CharmDataService;
import com.vibethema.service.EquipmentDataService;
import com.vibethema.service.SystemDataService;
import com.vibethema.viewmodel.footer.FooterViewModel;
import com.vibethema.viewmodel.util.Messenger;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainViewModel implements ViewModel {
    private static final Logger logger = LoggerFactory.getLogger(MainViewModel.class);
    private CharacterData data;
    private FooterViewModel footerViewModel;
    private final EquipmentDataService equipmentService;
    private final CharmDataService charmDataService;
    private final SystemDataService systemDataService;

    private final Map<String, String> tagDescriptions = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, String> keywordDefs = new java.util.concurrent.ConcurrentHashMap<>();

    private final ObjectProperty<File> currentFile = new SimpleObjectProperty<>();
    private final StringProperty windowTitle = new SimpleStringProperty("Vibethema");
    private final BooleanProperty dirty = new SimpleBooleanProperty();
    private final BooleanProperty coreDataImported = new SimpleBooleanProperty();

    public MainViewModel() {
        this(new EquipmentDataService(), new CharmDataService());
    }

    public MainViewModel(EquipmentDataService equipmentService, CharmDataService charmDataService) {
        this(CharacterFactory.createNewCharacter(), new SystemDataService(), equipmentService, charmDataService);
    }

    public MainViewModel(CharacterData data) {
        this(data, new SystemDataService(), new EquipmentDataService(), new CharmDataService());
    }

    public MainViewModel(CharacterData data, SystemDataService systemDataService) {
        this(data, systemDataService, new EquipmentDataService(), new CharmDataService());
    }

    public MainViewModel(CharacterData data, SystemDataService systemDataService, 
                        EquipmentDataService equipmentService, CharmDataService charmDataService) {
        this.systemDataService = systemDataService;
        this.equipmentService = equipmentService;
        this.charmDataService = charmDataService;
        init(data);
        coreDataImported.set(systemDataService.isCoreDataImported());
    }

    public void init(CharacterData data) {
        this.data = data;
        this.footerViewModel = new FooterViewModel(data, this::handleFinalization);
        this.dirty.unbind();
        this.dirty.bind(data.dirtyProperty());
        
        new Thread(() -> {
            // Load global data
            loadTagDescriptions();
            loadKeywords();
            
            // Load character-specific defaults if this is a new character (no weapons yet)
            if (data.getWeapons().isEmpty()) {
                new CharacterFactory().initializeDefaultEquipment(data, equipmentService);
            }
        }, "MainViewModel-Data-Loader").start();
        
        updateWindowTitle();
        currentFile.addListener((obs, oldV, newV) -> updateWindowTitle());
        dirty.addListener((obs, oldV, newV) -> updateWindowTitle());
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
        List<com.vibethema.model.Keyword> keywords = charmDataService.loadKeywords();
        for (com.vibethema.model.Keyword kw : keywords) {
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
    public CharacterData getData() { return data; }
    public FooterViewModel getFooterViewModel() { return footerViewModel; }
    public EquipmentDataService getEquipmentService() { return equipmentService; }
    public CharmDataService getCharmDataService() { return charmDataService; }
    public Map<String, String> getTagDescriptions() { return tagDescriptions; }
    public Map<String, String> getKeywordDefs() { return keywordDefs; }
    
    public StringProperty windowTitleProperty() { return windowTitle; }
    public ObjectProperty<File> currentFileProperty() { return currentFile; }
    public BooleanProperty dirtyProperty() { return dirty; }
    public BooleanProperty coreDataImportedProperty() { return coreDataImported; }

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
            }
        } catch (Exception ex) {
            logger.error("Failed to load character from {}", file.getAbsolutePath(), ex);
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
            String suggestName = data.nameProperty().get().trim().isEmpty() ? "Character.vbtm" : data.nameProperty().get().trim() + ".vbtm";
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
        Messenger.publish("request_pdf_export", suggestName);
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
}
