package com.vibethema.viewmodel;

import com.vibethema.model.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;
import com.vibethema.model.traits.*;
import de.saxsys.mvvmfx.ViewModel;
import java.util.UUID;
import javafx.collections.ObservableList;

public class IntimaciesViewModel implements ViewModel {
    private final CharacterData data;

    public IntimaciesViewModel(CharacterData data) {
        this.data = data;
    }

    public ObservableList<Intimacy> getIntimacies() {
        return data.getIntimacies();
    }

    public void addIntimacy() {
        data.getIntimacies()
                .add(
                        new Intimacy(
                                UUID.randomUUID().toString(),
                                "",
                                Intimacy.Type.TIE,
                                Intimacy.Intensity.MINOR));
        data.setDirty(true);
    }

    public void removeIntimacy(Intimacy intimacy) {
        data.getIntimacies().remove(intimacy);
        data.setDirty(true);
    }
}
