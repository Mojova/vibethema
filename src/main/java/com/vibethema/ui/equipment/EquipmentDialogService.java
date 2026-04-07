package com.vibethema.ui.equipment;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.social.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.logic.*;


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
        javafx.collections.ObservableList<com.vibethema.model.traits.Specialty> characterSpecialties,
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