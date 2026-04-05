package com.vibethema.ui.equipment;

import com.vibethema.model.Armor;
import com.vibethema.model.Hearthstone;
import com.vibethema.model.OtherEquipment;
import com.vibethema.model.Specialty;
import com.vibethema.model.Weapon;
import com.vibethema.service.EquipmentDataService;
import javafx.stage.Window;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for displaying equipment-related dialogs.
 * Separates UI interaction from the main view components.
 */
public interface EquipmentDialogService {
    
    Optional<Weapon> showWeaponDialog(
        Weapon existing, 
        EquipmentDataService equipmentService, 
        Map<String, String> tagDescriptions,
        Window owner
    );

    Optional<Armor> showArmorDialog(
        Armor existing, 
        Map<String, String> tagDescriptions,
        Window owner
    );

    Optional<Hearthstone> showHearthstoneDialog(
        Hearthstone existing,
        Window owner
    );

    Optional<OtherEquipment> showOtherEquipmentDialog(
        OtherEquipment existing,
        Window owner
    );
}
