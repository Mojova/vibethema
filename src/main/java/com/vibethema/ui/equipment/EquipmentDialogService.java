package com.vibethema.ui.equipment;

import com.vibethema.model.equipment.*;
import com.vibethema.service.EquipmentDataService;
import java.util.Map;
import java.util.Optional;
import javafx.stage.Window;

/**
 * Service interface for displaying equipment-related dialogs. Separates UI interaction from the
 * main view components.
 */
public interface EquipmentDialogService {

    Optional<Weapon> showWeaponDialog(
            Weapon existing,
            EquipmentDataService equipmentService,
            Map<String, String> tagDescriptions,
            javafx.collections.ObservableList<com.vibethema.model.traits.Specialty>
                    characterSpecialties,
            Window owner);

    Optional<Armor> showArmorDialog(
            Armor existing, Map<String, String> tagDescriptions, Window owner);

    Optional<Hearthstone> showHearthstoneDialog(Hearthstone existing, Window owner);

    Optional<OtherEquipment> showOtherEquipmentDialog(OtherEquipment existing, Window owner);
}
