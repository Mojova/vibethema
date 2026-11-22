package com.vibethema.viewmodel.equipment;

import com.vibethema.model.equipment.Armor;
import com.vibethema.model.equipment.Weapon;
import de.saxsys.mvvmfx.ViewModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

/**
 * A generic ViewModel for browsing and selecting items from the equipment database. Supports
 * filtering/searching and custom actions for creation.
 */
public class EquipmentDatabaseViewModel implements ViewModel {
    private enum Section {
        MUNDANE("Mundane Equipment"),
        ARTIFACT("Artifact Equipment"),
        UNIQUE_ARTIFACT("Unique Artifacts"),
        UNGROUPED(null);

        private final String title;

        Section(String title) {
            this.title = title;
        }
    }

    private final ObservableList<Object> allItems = FXCollections.observableArrayList();
    private final FilteredList<Object> filteredItems = new FilteredList<>(allItems);

    private final StringProperty searchQuery = new SimpleStringProperty("");
    private final ObjectProperty<Object> selectedItem = new SimpleObjectProperty<>();
    private final StringProperty title = new SimpleStringProperty("Database");
    private final StringProperty createNewText = new SimpleStringProperty("+ Create New");

    private Runnable createNewAction;
    private java.util.function.Consumer<Object> deleteAction;
    private Set<String> uniqueArtifactIds = Set.of();

    public EquipmentDatabaseViewModel() {
        // Setup search filtering
        searchQuery.addListener((obs, oldVal, newVal) -> {
            filteredItems.setPredicate(item -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return item.toString().toLowerCase().contains(lower);
            });
        });
    }

    public void setItems(Collection<?> items) {
        setItems(items, Set.of());
    }

    public void setItems(Collection<?> items, Set<String> uniqueArtifactIds) {
        this.uniqueArtifactIds = Set.copyOf(uniqueArtifactIds);
        if (items == null) {
            allItems.clear();
            return;
        }

        List<Object> sortedList = new ArrayList<>(items);
        sortedList.sort(
                Comparator.comparing(this::section)
                        .thenComparing(Object::toString, String.CASE_INSENSITIVE_ORDER));
        allItems.setAll(sortedList);
    }

    public String getSectionHeading(int index) {
        if (index < 0 || index >= filteredItems.size()) return null;
        Section current = section(filteredItems.get(index));
        return index == 0 || current != section(filteredItems.get(index - 1))
                ? current.title
                : null;
    }

    private Section section(Object item) {
        return switch (item) {
            case Weapon weapon ->
                    classify(weapon.getType() == Weapon.WeaponType.ARTIFACT, weapon.getId());
            case Armor armor ->
                    classify(armor.getType() == Armor.ArmorType.ARTIFACT, armor.getId());
            default -> Section.UNGROUPED;
        };
    }

    private Section classify(boolean artifact, String id) {
        if (!artifact) return Section.MUNDANE;
        return id != null && uniqueArtifactIds.contains(id)
                ? Section.UNIQUE_ARTIFACT
                : Section.ARTIFACT;
    }

    public void setCreateNewAction(Runnable action) {
        this.createNewAction = action;
    }

    public void onCreateNew() {
        if (createNewAction != null) {
            createNewAction.run();
        }
    }

    public void setDeleteAction(java.util.function.Consumer<Object> action) {
        this.deleteAction = action;
    }

    public void onDeleteRequest() {
        Object selected = selectedItem.get();
        if (selected != null) {
            com.vibethema.viewmodel.util.Messenger.publish(
                    "confirm_database_deletion", selected.toString());
        }
    }

    public void performDelete() {
        Object selected = selectedItem.get();
        if (selected != null && deleteAction != null && canDeleteSelectedProperty().get()) {
            deleteAction.accept(selected);
            allItems.remove(selected);
        }
    }

    public javafx.beans.binding.BooleanExpression canDeleteSelectedProperty() {
        return javafx.beans.binding.Bindings.createBooleanBinding(
                () -> {
                    Object selected = selectedItem.get();
                    if (selected == null) return false;
                    if (selected instanceof com.vibethema.model.equipment.Weapon w) {
                        return !com.vibethema.model.equipment.Weapon.UNARMED_ID.equals(w.getId());
                    }
                    return true;
                },
                selectedItem);
    }

    public ObservableList<Object> getFilteredItems() {
        return filteredItems;
    }

    public StringProperty searchQueryProperty() {
        return searchQuery;
    }

    public ObjectProperty<Object> selectedItemProperty() {
        return selectedItem;
    }

    public StringProperty titleProperty() {
        return title;
    }

    public StringProperty createNewTextProperty() {
        return createNewText;
    }

    public Object getSelectedItem() {
        return selectedItem.get();
    }
}
