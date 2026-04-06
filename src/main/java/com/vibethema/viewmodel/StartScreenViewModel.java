package com.vibethema.viewmodel;

import com.vibethema.viewmodel.util.Messenger;
import de.saxsys.mvvmfx.ViewModel;
import java.io.File;

/**
 * ViewModel for the initial application start screen.
 * Handles primary user actions and initiates the transition to the main builder.
 */
public class StartScreenViewModel implements ViewModel {

    /**
     * Triggers the transition to start a fresh character creation.
     */
    public void onNewCharacter() {
        Messenger.publish("show_main_view");
    }

    /**
     * Requests the View to display a FileChooser for loading an existing character.
     */
    public void onLoadCharacter() {
        Messenger.publish("request_load_file_start");
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
        if (file != null) {
            Messenger.publish("show_main_view", file);
        }
    }
}
