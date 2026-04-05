package com.vibethema.viewmodel;

import com.vibethema.model.*;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ListChangeListener;
import com.vibethema.viewmodel.util.Messenger;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ListChangeListener;

public class StatsViewModel implements ViewModel {
    private final CharacterData data;
    private final IntegerProperty maTotalDots = new SimpleIntegerProperty(0);

    public StatsViewModel(CharacterData data) {
        this.data = data;
        
        // MA total dots for Brawl min-dot logic
        updateMaTotal();
        data.getMartialArtsStyles().addListener((ListChangeListener<? super MartialArtsStyle>) c -> {
            updateMaTotal();
            while (c.next()) {
                if (c.wasAdded()) {
                    for (MartialArtsStyle mas : c.getAddedSubList()) {
                        mas.ratingProperty().addListener((obs, ov, nv) -> updateMaTotal());
                    }
                }
            }
        });
        for (MartialArtsStyle mas : data.getMartialArtsStyles()) {
            mas.ratingProperty().addListener((obs, ov, nv) -> updateMaTotal());
        }
    }

    private void updateMaTotal() {
        int total = data.getMartialArtsStyles().stream().mapToInt(MartialArtsStyle::getRating).sum();
        maTotalDots.set(total);
    }

    public CharacterData getData() {
        return data;
    }

    public IntegerProperty maTotalDotsProperty() {
        return maTotalDots;
    }

    public void jumpToCharms(String abilityName) {
        Messenger.publish("jump_to_charms", abilityName);
    }
    
    public void markDirty() {
        data.setDirty(true);
    }
}
