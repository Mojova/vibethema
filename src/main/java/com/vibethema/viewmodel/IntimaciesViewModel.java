package com.vibethema.viewmodel;

import com.vibethema.model.CharacterData;
import com.vibethema.model.Intimacy;
import de.saxsys.mvvmfx.ViewModel;
import javafx.collections.ObservableList;
import java.util.UUID;

public class IntimaciesViewModel implements ViewModel {
    private final CharacterData data;

    public IntimaciesViewModel(CharacterData data) {
        this.data = data;
    }

    public ObservableList<Intimacy> getIntimacies() {
        return data.getIntimacies();
    }

    public void addIntimacy() {
        data.getIntimacies().add(new Intimacy(UUID.randomUUID().toString(), "", Intimacy.Type.TIE, Intimacy.Intensity.MINOR));
        data.setDirty(true);
    }

    public void removeIntimacy(Intimacy intimacy) {
        data.getIntimacies().remove(intimacy);
        data.setDirty(true);
    }
}
