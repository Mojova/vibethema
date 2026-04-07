package com.vibethema.viewmodel;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.social.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.logic.*;


import com.vibethema.service.SystemDataService;
import com.vibethema.viewmodel.util.Messenger;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.io.File;

/**
 * ViewModel for the initial application start screen.
 * Handles primary user actions and initiates the transition to the main builder.
 */
public class StartScreenViewModel implements ViewModel {

    private final SystemDataService systemDataService;
    private final BooleanProperty coreDataImported = new SimpleBooleanProperty();
    private final StringProperty statusMessage = new SimpleStringProperty();

    public StartScreenViewModel() {
        this(new SystemDataService());
    }

    public StartScreenViewModel(SystemDataService systemDataService) {
        this.systemDataService = systemDataService;
        refreshStatus();
    }

    public void refreshStatus() {
        boolean imported = systemDataService.isCoreDataImported();
        coreDataImported.set(imported);
        if (!imported) {
            statusMessage.set("The Exalted 3rd Edition Core PDF has not been imported. Please import the PDF to enable character creation.");
        } else {
            statusMessage.set("");
        }
    }

    public BooleanProperty coreDataImportedProperty() {
        return coreDataImported;
    }

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    /**
     * Triggers the transition to start a fresh character creation.
     */
    public void onNewCharacter() {
        if (coreDataImported.get()) {
            Messenger.publish("show_main_view");
        }
    }

    /**
     * Requests the View to display a FileChooser for loading an existing character.
     */
    public void onLoadCharacter() {
        if (coreDataImported.get()) {
            Messenger.publish("request_load_file_start");
        }
    }

    /**
     * Requests the View to handle PDF core rulebook import.
     */
    public void onImportPdf() {
        Messenger.publish("request_import_pdf");
    }

    /**
     * Called by the View after a file is selected via the showOpenDialog.
     * Initiates the transition to the main builder with the selected file.
     *
     * @param file The character file to load.
     */
    public void onLoadFileSelected(File file) {
        if (file != null && coreDataImported.get()) {
            Messenger.publish("show_main_view", file);
        }
    }
}