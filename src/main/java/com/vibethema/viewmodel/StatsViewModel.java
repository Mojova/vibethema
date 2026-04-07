package com.vibethema.viewmodel;

import com.vibethema.model.*;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import com.vibethema.viewmodel.util.Messenger;
import com.vibethema.viewmodel.equipment.AttackPoolRowViewModel;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import com.vibethema.viewmodel.stats.CraftRowViewModel;
import com.vibethema.viewmodel.stats.SpecialtyRowViewModel;
import java.util.stream.Collectors;

public class StatsViewModel implements ViewModel {
    private final CharacterData data;
    private final IntegerProperty maTotalDots = new SimpleIntegerProperty(0);
    private final ObservableList<AttackPoolRowViewModel> attackPoolRows = FXCollections.observableArrayList();
    private final ObservableList<CraftRowViewModel> craftRows = FXCollections.observableArrayList();
    private final ObservableList<SpecialtyRowViewModel> specialtyRows = FXCollections.observableArrayList();

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

        // Sync attack pools
        updateAttackPoolRows();
        data.getAttackPools().addListener((ListChangeListener<? super AttackPoolData>) c -> updateAttackPoolRows());

        // Sync Crafts
        updateCraftRows();
        data.getCrafts().addListener((ListChangeListener<? super CraftAbility>) c -> updateCraftRows());

        // Sync Specialties
        updateSpecialtyRows();
        data.getSpecialties().addListener((ListChangeListener<? super Specialty>) c -> updateSpecialtyRows());
    }

    private void updateCraftRows() {
        craftRows.setAll(data.getCrafts().stream()
                .map(CraftRowViewModel::new)
                .collect(Collectors.toList()));
    }

    private void updateSpecialtyRows() {
        specialtyRows.setAll(data.getSpecialties().stream()
                .map(SpecialtyRowViewModel::new)
                .collect(Collectors.toList()));
    }

    private void updateAttackPoolRows() {
        attackPoolRows.setAll(data.getAttackPools().stream()
                .map(AttackPoolRowViewModel::new)
                .collect(Collectors.toList()));
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

    public ObservableList<AttackPoolRowViewModel> getAttackPoolRows() {
        return attackPoolRows;
    }

    public IntegerProperty personalMotesProperty() {
        return data.personalMotesProperty();
    }

    public IntegerProperty peripheralMotesProperty() {
        return data.peripheralMotesProperty();
    }

    public ObservableList<String> healthLevelsProperty() {
        return data.healthLevelsProperty();
    }

    public ObservableList<CraftRowViewModel> getCraftRows() {
        return craftRows;
    }

    public ObservableList<SpecialtyRowViewModel> getSpecialtyRows() {
        return specialtyRows;
    }

    public void jumpToCharms(String abilityName) {
        Messenger.publish("jump_to_charms", abilityName);
    }
    
    public void markDirty() {
        data.setDirty(true);
    }
}
