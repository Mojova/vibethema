package com.vibethema.viewmodel.equipment;

import com.vibethema.model.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;
import com.vibethema.model.traits.*;
import com.vibethema.service.EquipmentDataService;
import de.saxsys.mvvmfx.ViewModel;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ArmorDialogViewModel implements ViewModel {
    private final StringProperty name = new SimpleStringProperty("");
    private final ObjectProperty<Armor.ArmorType> type =
            new SimpleObjectProperty<>(Armor.ArmorType.MORTAL);
    private final ObjectProperty<Armor.ArmorWeight> weight =
            new SimpleObjectProperty<>(Armor.ArmorWeight.MEDIUM);
    private final ObservableList<TagSelectionViewModel> availableTags =
            FXCollections.observableArrayList();

    public ArmorDialogViewModel(Armor existing, List<EquipmentDataService.Tag> allArmorTags) {
        if (existing != null) {
            name.set(existing.getName());
            type.set(existing.getType());
            weight.set(existing.getWeight());
        }

        List<String> currentTags = (existing != null) ? existing.getTags() : List.of();
        availableTags.setAll(
                allArmorTags.stream()
                        .map(t -> new TagSelectionViewModel(t, currentTags.contains(t.getName())))
                        .collect(Collectors.toList()));
    }

    public StringProperty nameProperty() {
        return name;
    }

    public ObjectProperty<Armor.ArmorType> typeProperty() {
        return type;
    }

    public ObjectProperty<Armor.ArmorWeight> weightProperty() {
        return weight;
    }

    public ObservableList<TagSelectionViewModel> getAvailableTags() {
        return availableTags;
    }

    public Armor applyTo(Armor armor) {
        if (armor == null) {
            armor = new Armor(name.get());
        }
        armor.setName(name.get());
        armor.setType(type.get());
        armor.setWeight(weight.get());

        List<String> selected =
                availableTags.stream()
                        .filter(TagSelectionViewModel::isSelected)
                        .map(TagSelectionViewModel::getName)
                        .collect(Collectors.toList());
        armor.getTags().setAll(selected);

        return armor;
    }

    public static class TagSelectionViewModel implements ViewModel {
        private final EquipmentDataService.Tag tag;
        private final BooleanProperty selected = new SimpleBooleanProperty();

        public TagSelectionViewModel(EquipmentDataService.Tag tag, boolean initial) {
            this.tag = tag;
            this.selected.set(initial);
        }

        public String getName() {
            return tag.getName();
        }

        public String getDescription() {
            return tag.getDescription();
        }

        public BooleanProperty selectedProperty() {
            return selected;
        }

        public boolean isSelected() {
            return selected.get();
        }

        @Override
        public String toString() {
            return getName();
        }
    }
}
