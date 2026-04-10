package com.vibethema.viewmodel;

import com.vibethema.service.UserPreferencesService;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PreferencesViewModel implements ViewModel {
    private final UserPreferencesService prefService;
    private final StringProperty paperSize = new SimpleStringProperty();

    public PreferencesViewModel() {
        this.prefService = UserPreferencesService.getInstance();
        this.paperSize.set(prefService.getPaperSize());
    }

    public StringProperty paperSizeProperty() {
        return paperSize;
    }

    public void save() {
        prefService.setPaperSize(paperSize.get());
    }
}
