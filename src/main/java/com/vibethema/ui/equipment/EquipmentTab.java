package com.vibethema.ui.equipment;

import com.vibethema.model.Armor;
import com.vibethema.model.CharacterData;
import com.vibethema.model.Hearthstone;
import com.vibethema.model.OtherEquipment;
import com.vibethema.model.Specialty;
import com.vibethema.model.Weapon;
import com.vibethema.service.CharmDataService;
import com.vibethema.service.EquipmentDataService;
import com.vibethema.ui.util.UIUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * A dedicated component for managing character equipment.
 */
public class EquipmentTab extends ScrollPane {
    
    private final CharacterData data;
    private final EquipmentDataService equipmentService;
    private final CharmDataService dataService;
    private final Map<String, String> tagDescriptions;
    private final Runnable refreshSummary;
    private final BiConsumer<String, String> evocationsCaller;

    private VBox weaponsListContainer;
    private VBox armorListContainer;
    private VBox hearthstoneListContainer;
    private VBox otherEquipmentListContainer;

    public EquipmentTab(CharacterData data, EquipmentDataService equipmentService, CharmDataService dataService, 
                        Map<String, String> tagDescriptions, Runnable refreshSummary, 
                        BiConsumer<String, String> evocationsCaller) {
        this.data = data;
        this.equipmentService = equipmentService;
        this.dataService = dataService;
        this.tagDescriptions = tagDescriptions;
        this.refreshSummary = refreshSummary;
        this.evocationsCaller = evocationsCaller;

        setFitToWidth(true);
        getStyleClass().add("scroll-pane-custom");
        
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
        data.getWeapons().addListener((javafx.collections.ListChangeListener.Change<? extends Weapon> c) -> refreshWeaponsList());
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
        data.getArmors().addListener((javafx.collections.ListChangeListener.Change<? extends Armor> c) -> refreshArmorList());
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
        data.getHearthstones().addListener((javafx.collections.ListChangeListener.Change<? extends Hearthstone> c) -> refreshHearthstoneList());
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
        data.getOtherEquipment().addListener((javafx.collections.ListChangeListener.Change<? extends OtherEquipment> c) -> refreshOtherEquipmentList());
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
        for (Weapon w : data.getWeapons()) {
            HBox row = new HBox(15);
            row.getStyleClass().add("merit-row");
            row.setAlignment(Pos.CENTER_LEFT);

            VBox details = new VBox(5);
            Label nameLabel = new Label(w.getName());
            if (w.getSpecialtyId() != null && !w.getSpecialtyId().isEmpty()) {
                Specialty s = data.getSpecialtyById(w.getSpecialtyId());
                if (s != null) {
                    nameLabel.setText(w.getName() + " (" + s.getName() + ")");
                }
            }
            nameLabel.getStyleClass().add("merit-name");
            
            String statsStr = String.format("Accuracy: %+d | Damage: %d | Defense: %+d | Overwhelming: %d", 
                w.getAccuracy(), w.getDamage(), w.getDefense(), w.getOverwhelming());
            if (w.getAttunement() > 0) statsStr += " | Attunement: " + w.getAttunement();
            
            Label statsLabel = new Label(statsStr);
            statsLabel.setStyle("-fx-font-size: 0.9em; -fx-text-fill: #aaa;");
            
            FlowPane tagsPane = new FlowPane(5, 5);
            tagsPane.getChildren().add(new Label("Tags: "));
            for (String tagName : w.getTags()) {
                Label tl = new Label(tagName);
                tl.setStyle("-fx-font-style: italic; -fx-font-size: 0.85em; -fx-text-fill: #3498db; -fx-cursor: hand;");
                tl.setTooltip(new Tooltip(tagDescriptions.getOrDefault(tagName, "No description available")));
                tagsPane.getChildren().add(tl);
            }

            details.getChildren().addAll(nameLabel, statsLabel, tagsPane);
            
            Button editBtn = new Button("✏️");
            editBtn.getStyleClass().add("edit-btn");
            editBtn.setStyle("-fx-base: #cea212;");
            editBtn.setOnAction(e -> showWeaponDialog(w));
            
            Button evBtn = new Button("✨");
            evBtn.getStyleClass().add("action-btn-small");
            evBtn.setTooltip(new Tooltip("Evocations"));
            evBtn.setVisible(w.getType() == Weapon.WeaponType.ARTIFACT);
            evBtn.setManaged(w.getType() == Weapon.WeaponType.ARTIFACT);
            evBtn.setOnAction(e -> evocationsCaller.accept(w.getId(), w.getName()));

            Button delBtn = new Button("🗑");
            delBtn.getStyleClass().add("remove-btn");
            delBtn.setOnAction(e -> data.getWeapons().remove(w));

            HBox.setHgrow(details, Priority.ALWAYS);
            row.getChildren().addAll(details, editBtn, evBtn, delBtn);
            weaponsListContainer.getChildren().add(row);
        }
    }

    private void showWeaponDialog(Weapon existing) {
        Dialog<Weapon> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add New Weapon" : "Edit Weapon");
        dialog.initOwner(getScene().getWindow());

        ButtonType saveBtnType = new ButtonType(existing == null ? "Add" : "Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(existing == null ? "" : existing.getName());
        nameField.setPromptText("Weapon Name (e.g. Reaper Daiklave)");
        
        ComboBox<Weapon.WeaponRange> rangeCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(Weapon.WeaponRange.values()));
        rangeCombo.setValue(existing == null ? Weapon.WeaponRange.CLOSE : existing.getRange());

        ComboBox<Weapon.WeaponType> typeCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(Weapon.WeaponType.values()));
        typeCombo.setValue(existing == null ? Weapon.WeaponType.MORTAL : existing.getType());

        ComboBox<Weapon.WeaponCategory> categoryCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(Weapon.WeaponCategory.values()));
        categoryCombo.setValue(existing == null ? Weapon.WeaponCategory.MEDIUM : existing.getCategory());

        ListView<EquipmentDataService.Tag> tagsList = new ListView<>();
        tagsList.setPrefHeight(150);
        javafx.collections.ObservableList<EquipmentDataService.Tag> selectedTags = javafx.collections.FXCollections.observableArrayList();
        
        ComboBox<Specialty> specialtyCombo = new ComboBox<>();
        specialtyCombo.setPromptText("Select Specialty");
        specialtyCombo.setConverter(new StringConverter<Specialty>() {
            @Override public String toString(Specialty s) { return (s == null) ? "None" : s.getName(); }
            @Override public Specialty fromString(String string) { return null; }
        });

        Runnable updateSpecs = () -> {
            Specialty current = specialtyCombo.getValue();
            javafx.collections.ObservableList<Specialty> options = javafx.collections.FXCollections.observableArrayList();
            options.add(null);
            
            boolean melee = selectedTags.stream().anyMatch(t -> t.getName().equalsIgnoreCase("Melee"));
            boolean archery = selectedTags.stream().anyMatch(t -> t.getName().equalsIgnoreCase("Archery"));
            boolean brawl = selectedTags.stream().anyMatch(t -> t.getName().equalsIgnoreCase("Brawl"));
            
            for (Specialty s : data.getSpecialties()) {
                String abil = s.getAbility();
                if (melee && "Melee".equals(abil)) options.add(s);
                else if (archery && "Archery".equals(abil)) options.add(s);
                else if (brawl && ("Brawl".equals(abil) || data.isMartialArtsStyle(abil))) options.add(s);
            }
            specialtyCombo.setItems(options);
            if (options.contains(current)) specialtyCombo.setValue(current);
            else specialtyCombo.setValue(null);
        };

        selectedTags.addListener((javafx.collections.ListChangeListener<? super EquipmentDataService.Tag>) c -> updateSpecs.run());

        if (existing != null) {
            String specId = existing.getSpecialtyId();
            if (specId != null && !specId.isEmpty()) {
                specialtyCombo.setValue(data.getSpecialtyById(specId));
            }
            String cat = existing.getRange() == Weapon.WeaponRange.CLOSE ? "melee" : 
                         (existing.getRange() == Weapon.WeaponRange.THROWN ? "thrown" : "archery");
            List<EquipmentDataService.Tag> availableTags = equipmentService.getTagsForCategory(cat);
            for (EquipmentDataService.Tag t : availableTags) {
                if (existing.getTags().contains(t.getName())) {
                    selectedTags.add(t);
                }
            }
        }
        updateSpecs.run();

        tagsList.setCellFactory(lv -> new ListCell<>() {
            private final CheckBox cb = new CheckBox();
            @Override
            protected void updateItem(EquipmentDataService.Tag item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    cb.setText(item.getName());
                    cb.setTooltip(new Tooltip(item.getDescription()));
                    cb.setSelected(selectedTags.stream().anyMatch(st -> st.getName().equals(item.getName())));
                    cb.setOnAction(e -> {
                        if (cb.isSelected()) {
                            if (selectedTags.stream().noneMatch(st -> st.getName().equals(item.getName()))) {
                                selectedTags.add(item);
                            }
                        } else {
                            selectedTags.removeIf(st -> st.getName().equals(item.getName()));
                        }
                    });
                    setGraphic(cb);
                }
            }
        });

        Runnable updateTagsList = () -> {
            String cat = rangeCombo.getValue() == Weapon.WeaponRange.CLOSE ? "melee" : 
                         (rangeCombo.getValue() == Weapon.WeaponRange.THROWN ? "thrown" : "archery");
            List<EquipmentDataService.Tag> available = equipmentService.getTagsForCategory(cat);
            tagsList.getItems().setAll(available);
            tagsList.refresh();
        };

        rangeCombo.setOnAction(e -> {
            selectedTags.clear();
            updateTagsList.run();
        });
        updateTagsList.run();

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Range:"), 0, 1);
        grid.add(rangeCombo, 1, 1);
        grid.add(new Label("Type:"), 0, 2);
        grid.add(typeCombo, 1, 2);
        grid.add(new Label("Category:"), 0, 3);
        grid.add(categoryCombo, 1, 3);
        grid.add(new Label("Tags:"), 0, 4);
        grid.add(tagsList, 1, 4);
        grid.add(new Label("Specialty:"), 0, 5);
        grid.add(specialtyCombo, 1, 5);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(nameField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveBtnType) {
                Weapon nw = (existing != null) ? existing : new Weapon(nameField.getText());
                nw.setName(nameField.getText());
                nw.setRange(rangeCombo.getValue());
                nw.setType(typeCombo.getValue());
                nw.setCategory(categoryCombo.getValue());
                nw.getTags().setAll(selectedTags.stream().map(EquipmentDataService.Tag::getName).collect(Collectors.toList()));
                nw.setSpecialtyId(specialtyCombo.getValue() == null ? "" : specialtyCombo.getValue().getId());
                return nw;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(weapon -> {
            if (existing == null) {
                data.getWeapons().add(weapon);
            } else {
                if (weapon.getType() == Weapon.WeaponType.ARTIFACT) {
                    try {
                        dataService.updateEvocationCollectionName(weapon.getId(), weapon.getName());
                    } catch (IOException ex) {
                        System.err.println("Failed to update evocation name: " + ex.getMessage());
                    }
                }
                refreshWeaponsList();
                data.setDirty(true);
            }
            if (refreshSummary != null) refreshSummary.run();
        });
    }

    private void refreshArmorList() {
        if (armorListContainer == null) return;
        armorListContainer.getChildren().clear();
        for (Armor a : data.getArmors()) {
            HBox row = new HBox(15); row.getStyleClass().add("merit-row"); row.setAlignment(Pos.CENTER_LEFT);
            
            CheckBox equippedCb = new CheckBox();
            equippedCb.setTooltip(new Tooltip("Equipped"));
            equippedCb.selectedProperty().bindBidirectional(a.equippedProperty());

            VBox details = new VBox(5);
            Label nameLabel = new Label(a.getName()); nameLabel.getStyleClass().add("merit-name");
            String stats = String.format("Type: %s | Soak: +%d | Hardness: %d | Mobility Penalty: %d",
                a.getType(), a.getSoak(), a.getHardness(), a.getMobilityPenalty());
            if (a.getAttunement() > 0) stats += " | Attunement: " + a.getAttunement();
            Label st = new Label(stats); st.setStyle("-fx-font-size: 0.9em; -fx-text-fill: #aaa;");
            
            FlowPane tagsPane = new FlowPane(5, 5);
            tagsPane.getChildren().add(new Label("Tags: "));
            for (String tagName : a.getTags()) {
                Label tl = new Label(tagName);
                tl.setStyle("-fx-font-style: italic; -fx-font-size: 0.85em; -fx-text-fill: #3498db; -fx-cursor: hand;");
                tl.setTooltip(new Tooltip(tagDescriptions.getOrDefault(tagName, "No description available")));
                tagsPane.getChildren().add(tl);
            }

            details.getChildren().addAll(nameLabel, st, tagsPane);
            Button editBtn = new Button("✏️"); editBtn.getStyleClass().add("edit-btn"); editBtn.setStyle("-fx-base: #cea212;");
            editBtn.setOnAction(e -> showArmorDialog(a));
            
            Button evBtn = new Button("✨");
            evBtn.getStyleClass().add("action-btn-small");
            evBtn.setTooltip(new Tooltip("Evocations"));
            evBtn.setVisible(a.getType() == Armor.ArmorType.ARTIFACT);
            evBtn.setManaged(a.getType() == Armor.ArmorType.ARTIFACT);
            evBtn.setOnAction(e -> evocationsCaller.accept(a.getId(), a.getName()));

            Button delBtn = new Button("🗑"); delBtn.getStyleClass().add("remove-btn"); delBtn.setOnAction(e -> data.getArmors().remove(a));
            
            HBox.setHgrow(details, Priority.ALWAYS);
            row.getChildren().addAll(equippedCb, details, editBtn, evBtn, delBtn);
            armorListContainer.getChildren().add(row);
        }
    }

    private void showArmorDialog(Armor existing) {
        Dialog<Armor> dialog = new Dialog<>(); dialog.setTitle(existing == null ? "Add Armor" : "Edit Armor");
        dialog.initOwner(getScene().getWindow());
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        TextField name = new TextField(); 
        ComboBox<Armor.ArmorType> type = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(Armor.ArmorType.values()));
        if (existing != null) { name.setText(existing.getName()); type.setValue(existing.getType()); }
        grid.add(new Label("Name:"), 0, 0); grid.add(name, 1, 0); grid.add(new Label("Type:"), 0, 1); grid.add(type, 1, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(b -> {
            if (b == saveBtn) {
                Armor a = (existing != null) ? existing : new Armor(name.getText());
                a.setName(name.getText()); a.setType(type.getValue()); return a;
            } return null;
        });
        dialog.showAndWait().ifPresent(a -> { 
            if (existing == null) data.getArmors().add(a); else refreshArmorList();
            if (refreshSummary != null) refreshSummary.run();
        });
    }

    private void refreshHearthstoneList() {
        if (hearthstoneListContainer == null) return;
        hearthstoneListContainer.getChildren().clear();
        for (Hearthstone h : data.getHearthstones()) {
            HBox row = new HBox(15); row.getStyleClass().add("merit-row"); row.setAlignment(Pos.CENTER_LEFT);
            VBox details = new VBox(5);
            Label nameLabel = new Label(h.getName()); nameLabel.getStyleClass().add("merit-name");
            Label descLabel = new Label(h.getDescription()); descLabel.setStyle("-fx-font-size: 0.9em; -fx-text-fill: #aaa;");
            details.getChildren().addAll(nameLabel, descLabel);
            Button editBtn = new Button("✏️"); editBtn.getStyleClass().add("edit-btn"); editBtn.setStyle("-fx-base: #cea212;");
            editBtn.setOnAction(e -> showHearthstoneDialog(h));
            Button delBtn = new Button("🗑"); delBtn.getStyleClass().add("remove-btn"); delBtn.setOnAction(e -> data.getHearthstones().remove(h));
            HBox.setHgrow(details, Priority.ALWAYS);
            row.getChildren().addAll(details, editBtn, delBtn);
            hearthstoneListContainer.getChildren().add(row);
        }
    }

    private void showHearthstoneDialog(Hearthstone existing) {
        Dialog<Hearthstone> dialog = new Dialog<>(); dialog.setTitle(existing == null ? "Add Hearthstone" : "Edit Hearthstone");
        dialog.initOwner(getScene().getWindow());
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        VBox root = new VBox(10); root.setPadding(new Insets(10));
        TextField name = new TextField(); if (existing != null) name.setText(existing.getName());
        TextArea desc = new TextArea(); if (existing != null) desc.setText(existing.getDescription());
        root.getChildren().addAll(new Label("Name:"), name, new Label("Description:"), desc);
        dialog.getDialogPane().setContent(root);
        dialog.setResultConverter(b -> {
            if (b == saveBtn) {
                Hearthstone h = (existing != null) ? existing : new Hearthstone(name.getText(), desc.getText());
                h.setName(name.getText()); h.setDescription(desc.getText()); return h;
            } return null;
        });
        dialog.showAndWait().ifPresent(h -> { if (existing == null) data.getHearthstones().add(h); else refreshHearthstoneList(); });
    }

    private void refreshOtherEquipmentList() {
        if (otherEquipmentListContainer == null) return;
        otherEquipmentListContainer.getChildren().clear();
        for (OtherEquipment o : data.getOtherEquipment()) {
            HBox row = new HBox(15); row.getStyleClass().add("merit-row"); row.setAlignment(Pos.CENTER_LEFT);
            VBox details = new VBox(5);
            Label nameLabel = new Label(o.getName() + (o.isArtifact() ? " (Artifact)" : ""));
            nameLabel.getStyleClass().add("merit-name");
            Label descLabel = new Label(o.getDescription()); descLabel.setStyle("-fx-font-size: 0.9em; -fx-text-fill: #aaa;");
            details.getChildren().addAll(nameLabel, descLabel);
            Button editBtn = new Button("✏️"); editBtn.getStyleClass().add("edit-btn"); editBtn.setStyle("-fx-base: #cea212;");
            editBtn.setOnAction(e -> showOtherEquipmentDialog(o));
            
            Button evBtn = new Button("✨");
            evBtn.getStyleClass().add("action-btn-small");
            evBtn.setTooltip(new Tooltip("Evocations"));
            evBtn.setVisible(o.isArtifact());
            evBtn.setManaged(o.isArtifact());
            evBtn.setOnAction(e -> evocationsCaller.accept(o.getId(), o.getName()));

            Button delBtn = new Button("🗑"); delBtn.getStyleClass().add("remove-btn"); delBtn.setOnAction(e -> data.getOtherEquipment().remove(o));
            HBox.setHgrow(details, Priority.ALWAYS);
            row.getChildren().addAll(details, editBtn, evBtn, delBtn);
            otherEquipmentListContainer.getChildren().add(row);
        }
    }

    private void showOtherEquipmentDialog(OtherEquipment existing) {
        Dialog<OtherEquipment> dialog = new Dialog<>(); dialog.setTitle(existing == null ? "Add Equipment" : "Edit Equipment");
        dialog.initOwner(getScene().getWindow());
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        VBox root = new VBox(10); root.setPadding(new Insets(10));
        TextField name = new TextField(); if (existing != null) name.setText(existing.getName());
        TextArea desc = new TextArea(); if (existing != null) desc.setText(existing.getDescription());
        CheckBox artifact = new CheckBox("Artifact Status"); if (existing != null) artifact.setSelected(existing.isArtifact());
        root.getChildren().addAll(new Label("Name:"), name, artifact, new Label("Description:"), desc);
        dialog.getDialogPane().setContent(root);
        dialog.setResultConverter(b -> {
            if (b == saveBtn) {
                OtherEquipment o = (existing != null) ? existing : new OtherEquipment(name.getText(), desc.getText());
                o.setName(name.getText()); o.setDescription(desc.getText()); o.setArtifact(artifact.isSelected()); return o;
            } return null;
        });
        dialog.showAndWait().ifPresent(o -> { if (existing == null) data.getOtherEquipment().add(o); else refreshOtherEquipmentList(); });
    }
}
