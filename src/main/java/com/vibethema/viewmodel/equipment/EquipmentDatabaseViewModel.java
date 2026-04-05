package com.vibethema.viewmodel.equipment;

import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;


/**
 * A generic ViewModel for browsing and selecting items from the equipment database.
 * Supports filtering/searching and custom actions for creation.
 */
public class EquipmentDatabaseViewModel implements ViewModel {
    
    private final ObservableList<Object> allItems = FXCollections.observableArrayList();
    private final FilteredList<Object> filteredItems = new FilteredList<>(allItems);
    
    private final StringProperty searchQuery = new SimpleStringProperty("");
    private final ObjectProperty<Object> selectedItem = new SimpleObjectProperty<>();
    private final StringProperty title = new SimpleStringProperty("Database");
    private final StringProperty createNewText = new SimpleStringProperty("+ Create New");
    
    private Runnable createNewAction;
    
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

    public void setItems(java.util.Collection<?> items) {
        allItems.setAll(items);
    }

    public void setCreateNewAction(Runnable action) {
        this.createNewAction = action;
    }

    public void onCreateNew() {
        if (createNewAction != null) {
            createNewAction.run();
        }
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
