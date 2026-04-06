package com.vibethema.viewmodel;

import com.vibethema.model.*;
import com.vibethema.service.CharmDataService;
import com.vibethema.service.EquipmentDataService;
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
import java.util.HashMap;
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
    private final EquipmentDataService equipmentService = new EquipmentDataService();
    private final CharmDataService charmDataService = new CharmDataService();

    private final Map<String, String> tagDescriptions = new HashMap<>();
    private final Map<String, String> keywordDefs = new HashMap<>();

    private final ObjectProperty<File> currentFile = new SimpleObjectProperty<>();
    private final StringProperty windowTitle = new SimpleStringProperty("Vibethema");
    private final BooleanProperty dirty = new SimpleBooleanProperty();

    public MainViewModel() {
        this(new CharacterData());
    }

    public MainViewModel(CharacterData data) {
        init(data);
    }

    public void init(CharacterData data) {
        this.data = data;
        this.footerViewModel = new FooterViewModel(data, this::handleFinalization);
        this.dirty.unbind();
        this.dirty.bind(data.dirtyProperty());
        
        loadTagDescriptions();
        loadKeywords();
        
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
}
