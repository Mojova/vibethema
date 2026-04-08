package com.vibethema.viewmodel.equipment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vibethema.model.*;
import com.vibethema.model.equipment.*;
import com.vibethema.service.EquipmentDataService.Tag;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArmorDialogViewModelTest {

    private Tag tag1;
    private Tag tag2;
    private List<Tag> allTags;

    @BeforeEach
    void setUp() {
        tag1 = mock(Tag.class);
        when(tag1.getName()).thenReturn("Concealable");
        when(tag1.getDescription()).thenReturn("Easy to hide");

        tag2 = mock(Tag.class);
        when(tag2.getName()).thenReturn("Buoyant");
        when(tag2.getDescription()).thenReturn("Floats");

        allTags = List.of(tag1, tag2);
    }

    @Test
    void testInitializesFromExistingArmor() {
        Armor armor = new Armor("id1", "Silk", Armor.ArmorType.ARTIFACT, Armor.ArmorWeight.LIGHT);
        armor.getTags().add("Concealable");

        ArmorDialogViewModel vm = new ArmorDialogViewModel(armor, allTags);

        assertEquals("Silk", vm.nameProperty().get());
        assertEquals(Armor.ArmorType.ARTIFACT, vm.typeProperty().get());
        assertEquals(Armor.ArmorWeight.LIGHT, vm.weightProperty().get());

        // Tags
        assertTrue(vm.getAvailableTags().get(0).isSelected()); // Concealable
        assertFalse(vm.getAvailableTags().get(1).isSelected()); // Buoyant
    }

    @Test
    void testInitializesNewArmor() {
        ArmorDialogViewModel vm = new ArmorDialogViewModel(null, allTags);
        assertEquals("", vm.nameProperty().get());
        assertEquals(Armor.ArmorType.MORTAL, vm.typeProperty().get());

        for (ArmorDialogViewModel.TagSelectionViewModel tvm : vm.getAvailableTags()) {
            assertFalse(tvm.isSelected());
        }
    }

    @Test
    void testApplyToExistingArmor() {
        Armor armor = new Armor("id1", "Test", Armor.ArmorType.MORTAL, Armor.ArmorWeight.MEDIUM);
        ArmorDialogViewModel vm = new ArmorDialogViewModel(armor, allTags);

        vm.nameProperty().set("New Name");
        vm.typeProperty().set(Armor.ArmorType.ARTIFACT);
        vm.weightProperty().set(Armor.ArmorWeight.HEAVY);
        vm.getAvailableTags().get(1).selectedProperty().set(true); // "Buoyant"

        vm.applyTo(armor);

        assertEquals("New Name", armor.getName());
        assertEquals(Armor.ArmorType.ARTIFACT, armor.getType());
        assertEquals(Armor.ArmorWeight.HEAVY, armor.getWeight());
        assertEquals(1, armor.getTags().size());
        assertEquals("Buoyant", armor.getTags().get(0));
    }
}
