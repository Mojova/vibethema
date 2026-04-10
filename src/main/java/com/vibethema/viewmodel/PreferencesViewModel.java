package com.vibethema.viewmodel;

import com.vibethema.service.UserPreferencesService;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PreferencesViewModel implements ViewModel {
    private final UserPreferencesService prefService;
    private final StringProperty paperSize = new SimpleStringProperty();
    private final StringProperty baseTheme = new SimpleStringProperty();

    public PreferencesViewModel() {
        this.prefService = UserPreferencesService.getInstance();
        this.paperSize.set(prefService.getPaperSize());
        this.baseTheme.set(prefService.getBaseTheme());
    }

    public StringProperty paperSizeProperty() {
        return paperSize;
    }

    public StringProperty baseThemeProperty() {
        return baseTheme;
    }

    public void save() {
        prefService.setPaperSize(paperSize.get());
        prefService.setBaseTheme(baseTheme.get());
        com.vibethema.viewmodel.util.Messenger.publish("preferences_updated");
    }
}
