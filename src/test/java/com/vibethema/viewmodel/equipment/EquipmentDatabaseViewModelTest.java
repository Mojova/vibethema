package com.vibethema.viewmodel.equipment;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class EquipmentDatabaseViewModelTest {

    @Test
    public void testItemsAreSortedAlphabetically() {
        EquipmentDatabaseViewModel vm = new EquipmentDatabaseViewModel();
        
        // Use strings as mock items (since the VM uses toString())
        List<String> input = Arrays.asList("Zebra", "Apple", "Monkey", "Banana");
        vm.setItems(input);
        
        List<Object> items = vm.getFilteredItems();
        assertEquals(4, items.size());
        assertEquals("Apple", items.get(0).toString());
        assertEquals("Banana", items.get(1).toString());
        assertEquals("Monkey", items.get(2).toString());
        assertEquals("Zebra", items.get(3).toString());
    }

    @Test
    public void testSortingIsCaseInsensitive() {
        EquipmentDatabaseViewModel vm = new EquipmentDatabaseViewModel();
        
        List<String> input = Arrays.asList("c", "A", "b", "D");
        vm.setItems(input);
        
        List<Object> items = vm.getFilteredItems();
        assertEquals("A", items.get(0).toString());
        assertEquals("b", items.get(1).toString());
        assertEquals("c", items.get(2).toString());
        assertEquals("D", items.get(3).toString());
    }

    @Test
    public void testSearchFilteringCombinedWithSorting() {
        EquipmentDatabaseViewModel vm = new EquipmentDatabaseViewModel();
        vm.setItems(Arrays.asList("Steel Sword", "Iron Shield", "Steel Shield", "Axe"));
        
        // Initial sorted order: Axe, Iron Shield, Steel Shield, Steel Sword
        assertEquals("Axe", vm.getFilteredItems().get(0).toString());
        
        // Filter for "Steel"
        vm.searchQueryProperty().set("Steel");
        
        assertEquals(2, vm.getFilteredItems().size());
        assertEquals("Steel Shield", vm.getFilteredItems().get(0).toString());
        assertEquals("Steel Sword", vm.getFilteredItems().get(1).toString());
    }

    @Test
    public void testCanDeleteProperty() {
        EquipmentDatabaseViewModel vm = new EquipmentDatabaseViewModel();
        
        com.vibethema.model.Weapon w1 = new com.vibethema.model.Weapon("sword", "Steel Sword", com.vibethema.model.Weapon.WeaponRange.CLOSE, com.vibethema.model.Weapon.WeaponType.MORTAL, com.vibethema.model.Weapon.WeaponCategory.MEDIUM);
        com.vibethema.model.Weapon unarmed = new com.vibethema.model.Weapon(com.vibethema.model.Weapon.UNARMED_ID, "Unarmed", com.vibethema.model.Weapon.WeaponRange.CLOSE, com.vibethema.model.Weapon.WeaponType.MORTAL, com.vibethema.model.Weapon.WeaponCategory.LIGHT);
        
        // Nothing selected
        assertFalse(vm.canDeleteSelectedProperty().get());
        
        // Select normal sword
        vm.selectedItemProperty().set(w1);
        assertTrue(vm.canDeleteSelectedProperty().get());
        
        // Select Unarmed
        vm.selectedItemProperty().set(unarmed);
        assertFalse(vm.canDeleteSelectedProperty().get(), "Unarmed should NOT be deletable");
        
        // Select another non-weapon object
        vm.selectedItemProperty().set("Random String");
        assertTrue(vm.canDeleteSelectedProperty().get(), "Non-weapon items should be deletable by default in this generic VM");
    }
}
