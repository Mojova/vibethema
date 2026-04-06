package com.vibethema.ui.equipment;

import com.vibethema.ui.util.UIUtils;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.util.StringConverter;
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
import java.util.Optional;

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
        addWeaponBtn.getStyleClass().addAll("add-btn", "action-btn");
        addWeaponBtn.setOnAction(e -> showWeaponDatabaseDialog());

        weaponsSection.getChildren().addAll(weaponsListContainer, addWeaponBtn);

        // Armor Section
        VBox armorSection = UIUtils.createSection("Armor");
        armorListContainer = new VBox(10);
        armorListContainer.getStyleClass().add("merit-row-container");
        viewModel.getArmors().addListener((ListChangeListener<ArmorRowViewModel>) c -> refreshArmorList());
        refreshArmorList();

        Button addArmorBtn = new Button("+ Add Armor");
        addArmorBtn.getStyleClass().addAll("add-btn", "action-btn");
        addArmorBtn.setOnAction(e -> showArmorDatabaseDialog());

        armorSection.getChildren().addAll(armorListContainer, addArmorBtn);

        // Hearthstone Section
        VBox hearthstonesSection = UIUtils.createSection("Hearthstones");
        hearthstoneListContainer = new VBox(10);
        hearthstoneListContainer.getStyleClass().add("merit-row-container");
        viewModel.getHearthstones().addListener((ListChangeListener<HearthstoneRowViewModel>) c -> refreshHearthstoneList());
        refreshHearthstoneList();

        Button addHearthstoneBtn = new Button("+ Add Hearthstone");
        addHearthstoneBtn.getStyleClass().addAll("add-btn", "action-btn");
        addHearthstoneBtn.setOnAction(e -> showHearthstoneDatabaseDialog());

        hearthstonesSection.getChildren().addAll(hearthstoneListContainer, addHearthstoneBtn);

        // Other Equipment Section
        VBox otherSection = UIUtils.createSection("Other Equipment");
        otherEquipmentListContainer = new VBox(10);
        otherEquipmentListContainer.getStyleClass().add("merit-row-container");
        viewModel.getOtherEquipment().addListener((ListChangeListener<OtherEquipmentRowViewModel>) c -> refreshOtherEquipmentList());
        refreshOtherEquipmentList();

        Button addOtherBtn = new Button("+ Add Equipment");
        addOtherBtn.getStyleClass().addAll("add-btn", "action-btn");
        addOtherBtn.setOnAction(e -> showOtherEquipmentDatabaseDialog());

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

            CheckBox equippedCb = new CheckBox();
            equippedCb.setTooltip(new Tooltip("Equipped"));
            equippedCb.selectedProperty().bindBidirectional(wvm.equippedProperty());

            VBox details = new VBox(5);
            Label nameLabel = new Label(wvm.nameProperty().get());
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

            ComboBox<Specialty> specialtyCombo = new ComboBox<>();
            specialtyCombo.setPromptText("Specialty");
            specialtyCombo.setPrefWidth(120);
            specialtyCombo.setConverter(new StringConverter<Specialty>() {
                @Override public String toString(Specialty s) { return s == null ? "None" : s.getName(); }
                @Override public Specialty fromString(String string) { return null; }
            });

            ObservableList<Specialty> specOptions = FXCollections.observableArrayList();
            specOptions.add(null);
            
            boolean melee = wvm.getWeapon().getTags().stream().anyMatch(t -> t.equalsIgnoreCase("Melee"));
            boolean archery = wvm.getWeapon().getTags().stream().anyMatch(t -> t.equalsIgnoreCase("Archery"));
            boolean brawl = wvm.getWeapon().getTags().stream().anyMatch(t -> t.equalsIgnoreCase("Brawl"));

            for (Specialty s : viewModel.getCharacterData().getSpecialties()) {
                if (s == null || s.getName().isEmpty()) continue;
                String abil = s.getAbility();
                if (melee && "Melee".equals(abil)) specOptions.add(s);
                else if (archery && "Archery".equals(abil)) specOptions.add(s);
                else if (brawl && ("Brawl".equals(abil) || (abil != null && abil.contains("Martial Arts")))) specOptions.add(s);
            }
            specialtyCombo.setItems(specOptions);
            
            String currentSpecId = wvm.getWeapon().getSpecialtyId();
            if (currentSpecId != null && !currentSpecId.isEmpty()) {
                specialtyCombo.setValue(viewModel.getCharacterData().getSpecialtyById(currentSpecId));
            }

            specialtyCombo.valueProperty().addListener((obs, ov, nv) -> {
                wvm.getWeapon().setSpecialtyId(nv == null ? "" : nv.getId());
                viewModel.markDirty();
            });
            
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
            row.getChildren().addAll(equippedCb, details, specialtyCombo, editBtn, evBtn, delBtn);
            weaponsListContainer.getChildren().add(row);
        }
    }

    private boolean showWeaponDialog(Weapon existing) {
        Optional<Weapon> result = dialogService.showWeaponDialog(
            existing, 
            viewModel.getEquipmentService(), 
            viewModel.getTagDescriptions(),
            getScene().getWindow()
        );
        result.ifPresent(weapon -> viewModel.saveWeapon(weapon, existing == null));
        return result.isPresent();
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

    private boolean showArmorDialog(Armor existing) {
        Optional<Armor> result = dialogService.showArmorDialog(existing, viewModel.getTagDescriptions(), getScene().getWindow());
        result.ifPresent(a -> viewModel.saveArmor(a, existing == null));
        return result.isPresent();
    }

    private void refreshHearthstoneList() {
        if (hearthstoneListContainer == null) return;
        hearthstoneListContainer.getChildren().clear();
        for (HearthstoneRowViewModel hvm : viewModel.getHearthstones()) {
            HBox row = new HBox(15); row.getStyleClass().add("merit-row"); row.setAlignment(Pos.CENTER_LEFT);
            CheckBox equippedCb = new CheckBox();
            equippedCb.setTooltip(new Tooltip("Equipped"));
            equippedCb.selectedProperty().bindBidirectional(hvm.equippedProperty());

            VBox details = new VBox(5);
            Label nameLabel = new Label(hvm.nameProperty().get()); nameLabel.getStyleClass().add("merit-name");
            Label descLabel = new Label(hvm.descriptionProperty().get()); descLabel.setStyle("-fx-font-size: 0.9em; -fx-text-fill: #aaa;");
            details.getChildren().addAll(nameLabel, descLabel);
            Button editBtn = new Button("✏️"); editBtn.getStyleClass().add("edit-btn"); editBtn.setStyle("-fx-base: #cea212;");
            editBtn.setOnAction(e -> showHearthstoneDialog(hvm.getHearthstone()));
            Button delBtn = new Button("🗑"); delBtn.getStyleClass().add("remove-btn"); delBtn.setOnAction(e -> viewModel.removeHearthstone(hvm.getHearthstone()));
            HBox.setHgrow(details, Priority.ALWAYS);
            row.getChildren().addAll(equippedCb, details, editBtn, delBtn);
            hearthstoneListContainer.getChildren().add(row);
        }
    }

    private boolean showHearthstoneDialog(Hearthstone existing) {
        Optional<Hearthstone> result = dialogService.showHearthstoneDialog(existing, getScene().getWindow());
        result.ifPresent(h -> viewModel.saveHearthstone(h, existing == null));
        return result.isPresent();
    }

    private void refreshOtherEquipmentList() {
        if (otherEquipmentListContainer == null) return;
        otherEquipmentListContainer.getChildren().clear();
        for (OtherEquipmentRowViewModel ovm : viewModel.getOtherEquipment()) {
            HBox row = new HBox(15); row.getStyleClass().add("merit-row"); row.setAlignment(Pos.CENTER_LEFT);
            CheckBox equippedCb = new CheckBox();
            equippedCb.setTooltip(new Tooltip("Equipped"));
            equippedCb.selectedProperty().bindBidirectional(ovm.equippedProperty());

            VBox details = new VBox(5);
            String name = ovm.nameProperty().get();
            if (ovm.artifactProperty().get()) {
                name += " (Artifact)";
                if (ovm.attunementProperty().get() > 0) {
                    name += " | Attunement: " + ovm.attunementProperty().get();
                }
            }
            Label nameLabel = new Label(name);
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
            row.getChildren().addAll(equippedCb, details, editBtn, evBtn, delBtn);
            otherEquipmentListContainer.getChildren().add(row);
        }
    }

    private boolean showOtherEquipmentDialog(OtherEquipment existing) {
        Optional<OtherEquipment> result = dialogService.showOtherEquipmentDialog(existing, getScene().getWindow());
        result.ifPresent(o -> viewModel.saveOtherEquipment(o, existing == null));
        return result.isPresent();
    }

    private void showWeaponDatabaseDialog() {
        showGenericDatabaseDialog("Weapon", viewModel.getGlobalWeapons(), 
            () -> showWeaponDialog(null), 
            item -> viewModel.addWeaponFromDatabase((Weapon)item));
    }

    private void showArmorDatabaseDialog() {
        showGenericDatabaseDialog("Armor", viewModel.getGlobalArmors(), 
            () -> showArmorDialog(null), 
            item -> viewModel.addArmorFromDatabase((Armor)item));
    }

    private void showHearthstoneDatabaseDialog() {
        showGenericDatabaseDialog("Hearthstone", viewModel.getGlobalHearthstones(), 
            () -> showHearthstoneDialog(null), 
            item -> viewModel.addHearthstoneFromDatabase((Hearthstone)item));
    }

    private void showOtherEquipmentDatabaseDialog() {
        showGenericDatabaseDialog("Equipment", viewModel.getGlobalOtherEquipment(), 
            () -> showOtherEquipmentDialog(null), 
            item -> viewModel.addOtherEquipmentFromDatabase((OtherEquipment)item));
    }

    private void showGenericDatabaseDialog(String type, java.util.Collection<?> items, java.util.function.BooleanSupplier onCreateNew, java.util.function.Consumer<Object> onSelect) {
        de.saxsys.mvvmfx.ViewTuple<EquipmentDatabaseView, com.vibethema.viewmodel.equipment.EquipmentDatabaseViewModel> tuple = 
            de.saxsys.mvvmfx.FluentViewLoader.javaView(EquipmentDatabaseView.class).load();
        
        EquipmentDatabaseView view = (EquipmentDatabaseView) tuple.getView();
        com.vibethema.viewmodel.equipment.EquipmentDatabaseViewModel dbVm = tuple.getViewModel();
        
        dbVm.titleProperty().set("Equipment Database: " + type + "s");
        dbVm.createNewTextProperty().set("+ Create New " + type);
        dbVm.setItems(items);
        
        Dialog<Object> dialog = new Dialog<>();
        dialog.setTitle(dbVm.titleProperty().get());
        dialog.initOwner(getScene().getWindow());
        dialog.getDialogPane().setContent(view);
        dialog.getDialogPane().getStyleClass().add("dialog-pane-custom");
        if (getScene() != null && getScene().getStylesheets() != null) {
            dialog.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        }

        // Handle "Create New" - it will open the creation dialog
        dbVm.setCreateNewAction(() -> {
            boolean success = onCreateNew.getAsBoolean();
            if (success) {
                dialog.setResult(Boolean.TRUE);
                dialog.close();
            }
        });

        // Handle "Add Selected"
        view.getAddSelectedBtn().setOnAction(e -> {
            Object selected = dbVm.getSelectedItem();
            if (selected != null) {
                dialog.setResult(selected);
                dialog.close();
                javafx.application.Platform.runLater(() -> onSelect.accept(selected));
            }
        });

        // Add standard Cancel button to the dialog pane
        // This also enables the ESC key dismissal automatically in JavaFX dialogs
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        
        dialog.showAndWait();
    }
}
