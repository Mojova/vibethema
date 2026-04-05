package com.vibethema.ui.equipment;

import com.vibethema.ui.util.UIUtils;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import com.vibethema.viewmodel.equipment.EquipmentViewModel;
import com.vibethema.viewmodel.equipment.WeaponRowViewModel;
import com.vibethema.viewmodel.equipment.ArmorRowViewModel;
import com.vibethema.viewmodel.equipment.HearthstoneRowViewModel;
import com.vibethema.viewmodel.equipment.OtherEquipmentRowViewModel;
import com.vibethema.model.Weapon;
import com.vibethema.model.Armor;
import com.vibethema.model.Hearthstone;
import com.vibethema.model.OtherEquipment;
import com.vibethema.model.Specialty;

/**
 * A dedicated component for managing character equipment.
 */
public class EquipmentTab extends ScrollPane implements JavaView<EquipmentViewModel> {
    
    @InjectViewModel
    private EquipmentViewModel viewModel;

    private final EquipmentDialogService dialogService;

    private VBox weaponsListContainer;
    private VBox armorListContainer;
    private VBox hearthstoneListContainer;
    private VBox otherEquipmentListContainer;

    public EquipmentTab(EquipmentDialogService dialogService) {
        this.dialogService = dialogService;
        setFitToWidth(true);
        getStyleClass().add("scroll-pane-custom");
    }

    public void initialize() {
        setContent(createContent());
    }

    private VBox createContent() {
        VBox content = new VBox(25);
        content.getStyleClass().add("content-area");
        content.setPadding(new Insets(20));

        // Weapons Section
        VBox weaponsSection = UIUtils.createSection("Weapons");
        weaponsListContainer = new VBox(10);
        weaponsListContainer.getStyleClass().add("merit-row-container");
        viewModel.getWeapons().addListener((ListChangeListener<WeaponRowViewModel>) c -> refreshWeaponsList());
        refreshWeaponsList();

        Button addWeaponBtn = new Button("+ Add Weapon");
        addWeaponBtn.getStyleClass().add("add-btn");
        addWeaponBtn.getStyleClass().add("action-btn");
        addWeaponBtn.setOnAction(e -> showWeaponDialog(null));
        weaponsSection.getChildren().addAll(weaponsListContainer, addWeaponBtn);

        // Armor Section
        VBox armorSection = UIUtils.createSection("Armor");
        armorListContainer = new VBox(10);
        armorListContainer.getStyleClass().add("merit-row-container");
        viewModel.getArmors().addListener((ListChangeListener<ArmorRowViewModel>) c -> refreshArmorList());
        refreshArmorList();

        Button addArmorBtn = new Button("+ Add Armor");
        addArmorBtn.getStyleClass().add("add-btn");
        addArmorBtn.getStyleClass().add("action-btn");
        addArmorBtn.setOnAction(e -> showArmorDialog(null));
        armorSection.getChildren().addAll(armorListContainer, addArmorBtn);

        // Hearthstone Section
        VBox hearthstonesSection = UIUtils.createSection("Hearthstones");
        hearthstoneListContainer = new VBox(10);
        hearthstoneListContainer.getStyleClass().add("merit-row-container");
        viewModel.getHearthstones().addListener((ListChangeListener<HearthstoneRowViewModel>) c -> refreshHearthstoneList());
        refreshHearthstoneList();

        Button addHearthstoneBtn = new Button("+ Add Hearthstone");
        addHearthstoneBtn.getStyleClass().add("add-btn");
        addHearthstoneBtn.getStyleClass().add("action-btn");
        addHearthstoneBtn.setOnAction(e -> showHearthstoneDialog(null));
        hearthstonesSection.getChildren().addAll(hearthstoneListContainer, addHearthstoneBtn);

        // Other Equipment Section
        VBox otherSection = UIUtils.createSection("Other Equipment");
        otherEquipmentListContainer = new VBox(10);
        otherEquipmentListContainer.getStyleClass().add("merit-row-container");
        viewModel.getOtherEquipment().addListener((ListChangeListener<OtherEquipmentRowViewModel>) c -> refreshOtherEquipmentList());
        refreshOtherEquipmentList();

        Button addOtherBtn = new Button("+ Add Equipment");
        addOtherBtn.getStyleClass().add("add-btn");
        addOtherBtn.getStyleClass().add("action-btn");
        addOtherBtn.setOnAction(e -> showOtherEquipmentDialog(null));
        otherSection.getChildren().addAll(otherEquipmentListContainer, addOtherBtn);

        content.getChildren().addAll(weaponsSection, armorSection, hearthstonesSection, otherSection);
        return content;
    }

    private void refreshWeaponsList() {
        if (weaponsListContainer == null) return;
        weaponsListContainer.getChildren().clear();
        for (WeaponRowViewModel wvm : viewModel.getWeapons()) {
            HBox row = new HBox(15);
            row.getStyleClass().add("merit-row");
            row.setAlignment(Pos.CENTER_LEFT);

            VBox details = new VBox(5);
            Label nameLabel = new Label(wvm.nameProperty().get());
            if (wvm.getWeapon().getSpecialtyId() != null && !wvm.getWeapon().getSpecialtyId().isEmpty()) {
                Specialty s = viewModel.getCharacterData().getSpecialtyById(wvm.getWeapon().getSpecialtyId());
                if (s != null) {
                    nameLabel.setText(wvm.nameProperty().get() + " (" + s.getName() + ")");
                }
            }
            nameLabel.getStyleClass().add("merit-name");
            
            String statsStr = String.format("Accuracy: %+d | Damage: %d | Defense: %+d | Overwhelming: %d", 
                wvm.accuracyProperty().get(), wvm.damageProperty().get(), wvm.defenseProperty().get(), wvm.overwhelmingProperty().get());
            if (wvm.attunementProperty().get() > 0) statsStr += " | Attunement: " + wvm.attunementProperty().get();
            
            Label statsLabel = new Label(statsStr);
            statsLabel.setStyle("-fx-font-size: 0.9em; -fx-text-fill: #aaa;");
            
            FlowPane tagsPane = new FlowPane(5, 5);
            tagsPane.getChildren().add(new Label("Tags: "));
            for (String tagName : wvm.getWeapon().getTags()) {
                Label tl = new Label(tagName);
                tl.setStyle("-fx-font-style: italic; -fx-font-size: 0.85em; -fx-text-fill: #3498db; -fx-cursor: hand;");
                tl.setTooltip(new Tooltip(viewModel.getTagDescriptions().getOrDefault(tagName, "No description available")));
                tagsPane.getChildren().add(tl);
            }

            details.getChildren().addAll(nameLabel, statsLabel, tagsPane);
            
            Button editBtn = new Button("✏️");
            editBtn.getStyleClass().add("edit-btn");
            editBtn.setStyle("-fx-base: #cea212;");
            editBtn.setOnAction(e -> showWeaponDialog(wvm.getWeapon()));
            
            Button evBtn = new Button("✨");
            evBtn.getStyleClass().add("action-btn-small");
            evBtn.setTooltip(new Tooltip("Evocations"));
            evBtn.setVisible(wvm.isArtifact());
            evBtn.setManaged(wvm.isArtifact());
            evBtn.setOnAction(e -> viewModel.callEvocations(wvm.idProperty().get(), wvm.nameProperty().get()));

            Button delBtn = new Button("🗑");
            delBtn.getStyleClass().add("remove-btn");
            delBtn.setOnAction(e -> viewModel.removeWeapon(wvm.getWeapon()));

            HBox.setHgrow(details, Priority.ALWAYS);
            row.getChildren().addAll(details, editBtn, evBtn, delBtn);
            weaponsListContainer.getChildren().add(row);
        }
    }

    private void showWeaponDialog(Weapon existing) {
        dialogService.showWeaponDialog(
            existing, 
            viewModel.getCharacterData().getSpecialties(), 
            viewModel.getEquipmentService(), 
            viewModel.getTagDescriptions(),
            getScene().getWindow()
        ).ifPresent(weapon -> viewModel.saveWeapon(weapon, existing == null));
    }

    private void refreshArmorList() {
        if (armorListContainer == null) return;
        armorListContainer.getChildren().clear();
        for (ArmorRowViewModel avm : viewModel.getArmors()) {
            HBox row = new HBox(15); row.getStyleClass().add("merit-row"); row.setAlignment(Pos.CENTER_LEFT);
            
            CheckBox equippedCb = new CheckBox();
            equippedCb.setTooltip(new Tooltip("Equipped"));
            equippedCb.selectedProperty().bindBidirectional(avm.equippedProperty());

            VBox details = new VBox(5);
            Label nameLabel = new Label(avm.nameProperty().get()); nameLabel.getStyleClass().add("merit-name");
            String stats = String.format("Type: %s | Soak: +%d | Hardness: %d | Mobility Penalty: %d",
                avm.getArmor().getType(), avm.soakProperty().get(), avm.hardnessProperty().get(), avm.mobilityPenaltyProperty().get());
            if (avm.attunementProperty().get() > 0) stats += " | Attunement: " + avm.attunementProperty().get();
            Label st = new Label(stats); st.setStyle("-fx-font-size: 0.9em; -fx-text-fill: #aaa;");
            
            FlowPane tagsPane = new FlowPane(5, 5);
            tagsPane.getChildren().add(new Label("Tags: "));
            for (String tagName : avm.getArmor().getTags()) {
                Label tl = new Label(tagName);
                tl.setStyle("-fx-font-style: italic; -fx-font-size: 0.85em; -fx-text-fill: #3498db; -fx-cursor: hand;");
                tl.setTooltip(new Tooltip(viewModel.getTagDescriptions().getOrDefault(tagName, "No description available")));
                tagsPane.getChildren().add(tl);
            }

            details.getChildren().addAll(nameLabel, st, tagsPane);
            Button editBtn = new Button("✏️"); editBtn.getStyleClass().add("edit-btn"); editBtn.setStyle("-fx-base: #cea212;");
            editBtn.setOnAction(e -> showArmorDialog(avm.getArmor()));
            
            Button evBtn = new Button("✨");
            evBtn.getStyleClass().add("action-btn-small");
            evBtn.setTooltip(new Tooltip("Evocations"));
            evBtn.setVisible(avm.isArtifact());
            evBtn.setManaged(avm.isArtifact());
            evBtn.setOnAction(e -> viewModel.callEvocations(avm.idProperty().get(), avm.nameProperty().get()));

            Button delBtn = new Button("🗑"); delBtn.getStyleClass().add("remove-btn"); delBtn.setOnAction(e -> viewModel.removeArmor(avm.getArmor()));
            
            HBox.setHgrow(details, Priority.ALWAYS);
            row.getChildren().addAll(equippedCb, details, editBtn, evBtn, delBtn);
            armorListContainer.getChildren().add(row);
        }
    }

    private void showArmorDialog(Armor existing) {
        dialogService.showArmorDialog(existing, viewModel.getTagDescriptions(), getScene().getWindow())
                     .ifPresent(a -> viewModel.saveArmor(a, existing == null));
    }

    private void refreshHearthstoneList() {
        if (hearthstoneListContainer == null) return;
        hearthstoneListContainer.getChildren().clear();
        for (HearthstoneRowViewModel hvm : viewModel.getHearthstones()) {
            HBox row = new HBox(15); row.getStyleClass().add("merit-row"); row.setAlignment(Pos.CENTER_LEFT);
            VBox details = new VBox(5);
            Label nameLabel = new Label(hvm.nameProperty().get()); nameLabel.getStyleClass().add("merit-name");
            Label descLabel = new Label(hvm.descriptionProperty().get()); descLabel.setStyle("-fx-font-size: 0.9em; -fx-text-fill: #aaa;");
            details.getChildren().addAll(nameLabel, descLabel);
            Button editBtn = new Button("✏️"); editBtn.getStyleClass().add("edit-btn"); editBtn.setStyle("-fx-base: #cea212;");
            editBtn.setOnAction(e -> showHearthstoneDialog(hvm.getHearthstone()));
            Button delBtn = new Button("🗑"); delBtn.getStyleClass().add("remove-btn"); delBtn.setOnAction(e -> viewModel.removeHearthstone(hvm.getHearthstone()));
            HBox.setHgrow(details, Priority.ALWAYS);
            row.getChildren().addAll(details, editBtn, delBtn);
            hearthstoneListContainer.getChildren().add(row);
        }
    }

    private void showHearthstoneDialog(Hearthstone existing) {
        dialogService.showHearthstoneDialog(existing, getScene().getWindow())
                     .ifPresent(h -> viewModel.saveHearthstone(h, existing == null));
    }

    private void refreshOtherEquipmentList() {
        if (otherEquipmentListContainer == null) return;
        otherEquipmentListContainer.getChildren().clear();
        for (OtherEquipmentRowViewModel ovm : viewModel.getOtherEquipment()) {
            HBox row = new HBox(15); row.getStyleClass().add("merit-row"); row.setAlignment(Pos.CENTER_LEFT);
            VBox details = new VBox(5);
            Label nameLabel = new Label(ovm.nameProperty().get() + (ovm.artifactProperty().get() ? " (Artifact)" : ""));
            nameLabel.getStyleClass().add("merit-name");
            Label descLabel = new Label(ovm.descriptionProperty().get()); descLabel.setStyle("-fx-font-size: 0.9em; -fx-text-fill: #aaa;");
            details.getChildren().addAll(nameLabel, descLabel);
            Button editBtn = new Button("✏️"); editBtn.getStyleClass().add("edit-btn"); editBtn.setStyle("-fx-base: #cea212;");
            editBtn.setOnAction(e -> showOtherEquipmentDialog(ovm.getOtherEquipment()));
            
            Button evBtn = new Button("✨");
            evBtn.getStyleClass().add("action-btn-small");
            evBtn.setTooltip(new Tooltip("Evocations"));
            evBtn.setVisible(ovm.artifactProperty().get());
            evBtn.setManaged(ovm.artifactProperty().get());
            evBtn.setOnAction(e -> viewModel.callEvocations(ovm.idProperty().get(), ovm.nameProperty().get()));

            Button delBtn = new Button("🗑"); delBtn.getStyleClass().add("remove-btn"); delBtn.setOnAction(e -> viewModel.removeOtherEquipment(ovm.getOtherEquipment()));
            HBox.setHgrow(details, Priority.ALWAYS);
            row.getChildren().addAll(details, editBtn, evBtn, delBtn);
            otherEquipmentListContainer.getChildren().add(row);
        }
    }

    private void showOtherEquipmentDialog(OtherEquipment existing) {
        dialogService.showOtherEquipmentDialog(existing, getScene().getWindow())
                     .ifPresent(o -> viewModel.saveOtherEquipment(o, existing == null));
    }
}
