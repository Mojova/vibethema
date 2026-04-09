package com.vibethema.ui;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.viewmodel.StatsViewModel;
import com.vibethema.viewmodel.stats.*;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import javafx.util.Pair;

public class StatsTab extends ScrollPane implements JavaView<StatsViewModel>, Initializable {

    @InjectViewModel private StatsViewModel viewModel;

    private VBox craftList;
    private VBox specList;
    private VBox trackBoxes;
    private Label totalSoakVal;
    private Label hardnessVal;
    private Label dodgeBaseLabel;
    private Label joinBattleLabel;
    private Label resolveBaseLabel;
    private Label guileBaseLabel;
    private Label personalLabel, peripheralLabel;
    private VBox attackList;

    private boolean contentLoaded = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setFitToWidth(true);
        getStyleClass().add("scroll-pane-custom");

        if (viewModel != null && !contentLoaded) {
            // Fill content staggered over multiple pulses to ensure UI thread remains
            // responsive
            javafx.application.Platform.runLater(
                    () -> {
                        if (contentLoaded) return;

                        VBox content = new VBox(20);
                        content.getStyleClass().add("content-area");
                        content.setPadding(new Insets(20));
                        CharacterData data = viewModel.getData();
                        setContent(content);

                        // Step 1: Top section
                        content.getChildren().add(createBasicAdvantagesSection(data));

                        // Step 2: Attributes (next pulse)
                        javafx.application.Platform.runLater(
                                () -> {
                                    content.getChildren().add(createAttributesSection(data));

                                    // Step 3: Abilities (next pulse)
                                    javafx.application.Platform.runLater(
                                            () -> {
                                                content.getChildren()
                                                        .addAll(
                                                                createAbilitiesAndSideStuff(data),
                                                                new Separator());

                                                // Step 4: Stats and
                                                // Combat (final pulse)
                                                javafx.application.Platform.runLater(
                                                        () -> {
                                                            content.getChildren()
                                                                    .addAll(
                                                                            createStatsRow(data),
                                                                            createAttackPoolsSection(
                                                                                    data),
                                                                            createGreatCurseSection(
                                                                                    data));
                                                            contentLoaded = true;
                                                        });
                                            });
                                });
                    });
        }
    }

    private VBox createBasicAdvantagesSection(CharacterData data) {
        VBox section = new VBox(15);
        Label advTitle = new Label("Advantages");
        advTitle.getStyleClass().add("section-title");

        HBox topRow = new HBox(50);

        VBox essBox = new VBox(5);
        Label essLabel = new Label("Essence");
        essLabel.getStyleClass().add("subsection-title");
        DotSelector essSelector = new DotSelector(essLabel, data.essenceProperty(), 1, 5);
        essSelector.contextIdProperty().set("Stats");
        essSelector.descriptionProperty().set("Essence");
        essSelector.targetIdProperty().set("stats_essence");
        essSelector.setId("stats_essence");
        essSelector
                .disableProperty()
                .bind(viewModel.modeProperty().isEqualTo(CharacterMode.CREATION));
        essBox.getChildren().addAll(essLabel, essSelector);

        VBox motesBox = new VBox(5);
        Label motesLabel = new Label("Mote Pools");
        motesLabel.getStyleClass().add("subsection-title");
        personalLabel = new Label();
        personalLabel
                .textProperty()
                .bind(Bindings.concat("Personal: ", viewModel.personalMotesProperty().asString()));
        peripheralLabel = new Label();
        peripheralLabel
                .textProperty()
                .bind(
                        Bindings.concat(
                                "Peripheral: ", viewModel.peripheralMotesProperty().asString()));
        motesBox.getChildren().addAll(motesLabel, personalLabel, peripheralLabel);

        VBox wpBox = new VBox(5);
        Label wpLabel = new Label("Willpower");
        wpLabel.getStyleClass().add("subsection-title");
        DotSelector wpSelector = new DotSelector(wpLabel, data.willpowerProperty(), 5, 10);
        wpSelector.contextIdProperty().set("Stats");
        wpSelector.descriptionProperty().set("Willpower");
        wpSelector.targetIdProperty().set("stats_willpower");
        wpSelector.setId("stats_willpower");
        wpBox.getChildren().addAll(wpLabel, wpSelector);

        topRow.getChildren().addAll(essBox, motesBox, wpBox);
        section.getChildren().addAll(advTitle, topRow);
        return section;
    }

    private VBox createAttributesSection(CharacterData data) {
        VBox section = new VBox(10);
        Label title = new Label("Attributes");
        title.getStyleClass().add("section-title");

        HBox columns = new HBox(30);
        for (AttributeCategoryViewModel catVm : viewModel.getAttributeCategories()) {
            columns.getChildren().add(new AttributeCategoryView(catVm));
        }

        section.getChildren().addAll(title, columns);
        return section;
    }

    private HBox createAbilitiesAndSideStuff(CharacterData data) {
        VBox abilitiesSection = new VBox(10);
        Label abilTitle = new Label("Abilities");
        abilTitle.getStyleClass().add("section-title");
        abilitiesSection.getChildren().add(abilTitle);

        GridPane abilGrid = new GridPane();
        abilGrid.setHgap(20);
        abilGrid.setVgap(10);

        int rowCount = 0, colCount = 0;
        for (AbilityRowViewModel rowVm : viewModel.getAbilityRows()) {
            Ability abil = rowVm.getAbility();
            if (abil == Ability.CRAFT || abil == Ability.MARTIAL_ARTS) continue;

            AbilityRowView rowView = new AbilityRowView(rowVm);
            rowView.setOnNameClick(
                    e -> {
                        if (e.getClickCount() == 2) viewModel.jumpToCharms(abil.getDisplayName());
                    });

            // We use the minDotsProperty directly from the ViewModel now
            rowView.getSelector().minDotsProperty().bind(rowVm.minDotsProperty());

            abilGrid.add(rowView, colCount, rowCount++);
            if (rowCount >= 13) {
                rowCount = 0;
                colCount++;
            }
        }
        abilitiesSection.getChildren().add(abilGrid);

        VBox sideStuff = new VBox(20);
        sideStuff.setPrefWidth(450);
        sideStuff.getChildren().addAll(createCraftsSection(data), createSpecialtiesSection(data));

        return new HBox(30, abilitiesSection, sideStuff);
    }

    private VBox createCraftsSection(CharacterData data) {
        VBox section = new VBox(10);
        craftList = new VBox(8);

        // Reactively sync craft rows
        syncCraftRows(data);
        viewModel
                .getCraftRows()
                .addListener(
                        (ListChangeListener<? super CraftRowViewModel>) c -> syncCraftRows(data));

        Button addBtn = new Button("+ Add Craft");
        addBtn.getStyleClass().add("action-btn");
        addBtn.setOnAction(
                e -> {
                    showCraftExpertiseDialog("")
                            .ifPresent(
                                    expertise -> {
                                        viewModel.addCraft(expertise);
                                    });
                });

        section.getChildren().addAll(createCraftsHeader(data), craftList, addBtn);
        return section;
    }

    private HBox createCraftsHeader(CharacterData data) {
        AbilityRowViewModel craftVm = viewModel.getCraftAbilityRow();
        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);

        // 1. Caste Indicator/Checkbox
        CheckBox casteBox = new CheckBox("C");
        casteBox.getStyleClass().add("caste-checkbox");
        casteBox.selectedProperty().bindBidirectional(craftVm.isCaste());
        casteBox.disableProperty().bind(craftVm.casteBoxDisabledProperty());

        Label casteLabel = new Label("C");
        casteLabel.getStyleClass().add("caste-marker");
        casteLabel.setStyle(
                "-fx-text-fill: #d4af37; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 0"
                        + " 4 0 4;");

        // Visibility bindings for Caste
        casteBox.visibleProperty().bind(craftVm.modeProperty().isEqualTo(CharacterMode.CREATION));
        casteLabel
                .visibleProperty()
                .bind(
                        Bindings.and(
                                craftVm.modeProperty().isEqualTo(CharacterMode.EXPERIENCED),
                                craftVm.isCaste()));
        casteLabel.managedProperty().bind(casteLabel.visibleProperty());

        StackPane casteContainer = new StackPane(casteBox);
        casteContainer.setMinWidth(25);
        casteContainer
                .managedProperty()
                .bind(craftVm.modeProperty().isEqualTo(CharacterMode.CREATION));
        casteContainer.visibleProperty().bind(casteContainer.managedProperty());

        // 2. Favored Indicator/Checkbox
        CheckBox favoredBox = new CheckBox("F");
        favoredBox.getStyleClass().add("favored-checkbox");
        favoredBox.selectedProperty().bindBidirectional(craftVm.isFavored());
        favoredBox.disableProperty().bind(craftVm.favoredBoxDisabledProperty());

        Label favoredLabel = new Label("F");
        favoredLabel.getStyleClass().add("favored-marker");
        favoredLabel.setStyle(
                "-fx-text-fill: #3498db; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 0"
                        + " 4 0 4;");

        // Visibility bindings for Favored
        favoredBox.visibleProperty().bind(craftVm.modeProperty().isEqualTo(CharacterMode.CREATION));
        favoredLabel
                .visibleProperty()
                .bind(
                        Bindings.and(
                                craftVm.modeProperty().isEqualTo(CharacterMode.EXPERIENCED),
                                craftVm.isFavored()));
        favoredLabel.managedProperty().bind(favoredLabel.visibleProperty());

        StackPane favoredContainer = new StackPane(favoredBox);
        favoredContainer.setMinWidth(25);
        favoredContainer
                .managedProperty()
                .bind(craftVm.modeProperty().isEqualTo(CharacterMode.CREATION));
        favoredContainer.visibleProperty().bind(favoredContainer.managedProperty());

        // Experienced Mode Container (C/F labels)
        StackPane expModeContainer = new StackPane(casteLabel, favoredLabel);
        expModeContainer.setMinWidth(25);
        expModeContainer.setAlignment(Pos.CENTER);
        expModeContainer
                .managedProperty()
                .bind(craftVm.modeProperty().isEqualTo(CharacterMode.EXPERIENCED));
        expModeContainer.visibleProperty().bind(expModeContainer.managedProperty());

        Label title = new Label("Crafts");
        title.getStyleClass().add("section-title");

        header.getChildren()
                .addAll(
                        title,
                        casteContainer,
                        favoredContainer,
                        expModeContainer);
        return header;
    }

    private void syncCraftRows(CharacterData data) {
        craftList.getChildren().clear();
        for (CraftRowViewModel rvm : viewModel.getCraftRows()) {
            CraftRowView view = new CraftRowView(rvm);

            // Configure DotSelector
            DotSelector ds = view.getSelector();
            ds.minDotsProperty()
                    .bind(
                            Bindings.createIntegerBinding(
                                    () -> {
                                        if (!data.getFavoredAbility(Ability.CRAFT).get()) return 0;
                                        int totalDots =
                                                data.getCrafts().stream()
                                                        .mapToInt(CraftAbility::getRating)
                                                        .sum();
                                        return (totalDots - rvm.ratingProperty().get() > 0) ? 0 : 1;
                                    },
                                    data.getFavoredAbility(Ability.CRAFT),
                                    rvm.ratingProperty()));
            ds.contextIdProperty().set("Stats");

            // Configure Buttons
            view.setOnEdit(
                    e -> {
                        showCraftExpertiseDialog(rvm.expertiseProperty().get())
                                .ifPresent(
                                        newExpertise -> {
                                            viewModel.editCraft(rvm.getModel(), newExpertise);
                                        });
                    });

            view.setOnDelete(
                    e -> {
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                        confirm.setTitle("Delete Craft");
                        confirm.setHeaderText("Delete " + rvm.expertiseProperty().get() + "?");
                        confirm.setContentText("Are you sure you want to delete this craft?");
                        confirm.initOwner(getScene().getWindow());
                        confirm.showAndWait()
                                .filter(response -> response == ButtonType.OK)
                                .ifPresent(response -> viewModel.deleteCraft(rvm.getModel()));
                    });

            craftList.getChildren().add(view);
        }
    }

    private Optional<String> showCraftExpertiseDialog(String initialValue) {
        TextInputDialog dialog = new TextInputDialog(initialValue);
        dialog.setTitle(initialValue.isEmpty() ? "Add New Craft" : "Edit Craft Expertise");
        dialog.setHeaderText(initialValue.isEmpty() ? "Add New Craft Expertise" : "Edit Expertise");
        dialog.setContentText("Expertise:");
        dialog.initOwner(getScene().getWindow());

        // Style it to match our premium look
        dialog.getDialogPane().getStyleClass().add("dialog-pane-custom");

        return dialog.showAndWait();
    }

    private VBox createSpecialtiesSection(CharacterData data) {
        VBox section = new VBox(10);
        Label title = new Label("Specialties");
        title.getStyleClass().add("section-title");
        specList = new VBox(8);

        syncSpecialtyRows(data);
        viewModel
                .getSpecialtyRows()
                .addListener(
                        (ListChangeListener<? super SpecialtyRowViewModel>)
                                c -> syncSpecialtyRows(data));

        Button addBtn = new Button("+ Add Specialty");
        addBtn.getStyleClass().add("action-btn");
        addBtn.setOnAction(
                e -> {
                    showSpecialtyDialog("", "")
                            .ifPresent(
                                    result -> {
                                        viewModel.addSpecialty(result.getKey(), result.getValue());
                                    });
                });

        section.getChildren().addAll(title, specList, addBtn);
        return section;
    }

    private void syncSpecialtyRows(CharacterData data) {
        specList.getChildren().clear();
        for (SpecialtyRowViewModel rvm : viewModel.getSpecialtyRows()) {
            SpecialtyRowView view = new SpecialtyRowView(rvm);

            view.setOnEdit(
                    e -> {
                        showSpecialtyDialog(rvm.nameProperty().get(), rvm.abilityProperty().get())
                                .ifPresent(
                                        result -> {
                                            viewModel.editSpecialty(
                                                    rvm.getModel(),
                                                    result.getKey(),
                                                    result.getValue());
                                        });
                    });

            view.setOnDelete(
                    e -> {
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                        confirm.setTitle("Delete Specialty");
                        confirm.setHeaderText("Delete " + rvm.nameProperty().get() + "?");
                        confirm.setContentText("Are you sure you want to delete this specialty?");
                        confirm.initOwner(getScene().getWindow());
                        confirm.showAndWait()
                                .filter(response -> response == ButtonType.OK)
                                .ifPresent(response -> viewModel.deleteSpecialty(rvm.getModel()));
                    });

            specList.getChildren().add(view);
        }
    }

    private Optional<Pair<String, String>> showSpecialtyDialog(
            String initialName, String initialAbility) {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle(initialName.isEmpty() ? "Add New Specialty" : "Edit Specialty");
        dialog.setHeaderText(initialName.isEmpty() ? "Add New Specialty" : "Edit Specialty");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(initialName);
        nameField.setPromptText("Specialty Name");

        ComboBox<String> abilityCombo = new ComboBox<>();
        abilityCombo
                .getItems()
                .addAll(
                        SystemData.ABILITIES.stream()
                                .map(Ability::getDisplayName)
                                .collect(Collectors.toList()));
        abilityCombo.setValue(
                initialAbility.isEmpty()
                        ? SystemData.ABILITIES.get(0).getDisplayName()
                        : initialAbility);

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Ability:"), 0, 1);
        grid.add(abilityCombo, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getStyleClass().add("dialog-pane-custom");

        dialog.setResultConverter(
                dialogButton -> {
                    if (dialogButton == saveButtonType) {
                        return new Pair<>(nameField.getText(), abilityCombo.getValue());
                    }
                    return null;
                });

        return dialog.showAndWait();
    }

    private HBox createStatsRow(CharacterData data) {
        HBox row = new HBox(40);
        row.getChildren()
                .addAll(
                        createHealthSection(data),
                        createCombatStatsSection(data),
                        createSocialStatsSection(data));
        return row;
    }

    private VBox createHealthSection(CharacterData data) {
        VBox healthBox = new VBox(5);
        healthBox.getChildren().add(new Label("Health Levels"));
        trackBoxes = new VBox(8);
        updateHealth();
        viewModel
                .healthLevelsProperty()
                .addListener((ListChangeListener<? super String>) c -> updateHealth());
        healthBox.getChildren().add(trackBoxes);
        return healthBox;
    }

    private void updateHealth() {
        if (trackBoxes == null) return;
        trackBoxes.getChildren().clear();

        List<String> levels = viewModel.healthLevelsProperty();
        // Group by penalty, preserving order as much as possible, or using a fixed
        // order.
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String lvl : levels) {
            counts.put(lvl, counts.getOrDefault(lvl, 0) + 1);
        }

        // Standard Exalted health level order
        String[] order = {"-0", "-1", "-2", "-4", "Incap"};
        for (String penalty : order) {
            if (counts.containsKey(penalty)) {
                trackBoxes.getChildren().add(createHealthLevelRow(penalty, counts.get(penalty)));
            }
        }
    }

    private HBox createHealthLevelRow(String penalty, int boxes) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label pLabel = new Label(penalty + ":");
        pLabel.setMinWidth(45);
        pLabel.getStyleClass().add("merit-name");
        row.getChildren().add(pLabel);

        for (int i = 0; i < boxes; i++) {
            CheckBox cb = new CheckBox();
            cb.getStyleClass().add("health-checkbox");
            row.getChildren().add(cb);
        }
        return row;
    }

    private VBox createCombatStatsSection(CharacterData data) {
        VBox combatBox = new VBox(5);
        Label combatLabel = new Label("Combat Statistics");
        combatLabel.getStyleClass().add("subsection-title");

        VBox statsList = new VBox(5);
        statsList.getStyleClass().add("merit-row-container");
        statsList.setPadding(new Insets(5, 10, 5, 10));

        Label naturalSoak = new Label();
        naturalSoak
                .textProperty()
                .bind(Bindings.concat("Natural Soak: ", data.naturalSoakProperty().asString()));
        Label armorSoak = new Label();
        armorSoak
                .textProperty()
                .bind(Bindings.concat("Armor Soak: +", data.armorSoakProperty().asString()));
        totalSoakVal = new Label();
        totalSoakVal
                .textProperty()
                .bind(Bindings.concat("Total Soak: ", data.totalSoakProperty().asString()));
        totalSoakVal.setStyle("-fx-font-weight: bold; -fx-text-fill: #3498db;");

        hardnessVal = new Label();
        hardnessVal
                .textProperty()
                .bind(Bindings.concat("Hardness: ", data.totalHardnessProperty().asString()));

        dodgeBaseLabel = new Label();
        dodgeBaseLabel
                .textProperty()
                .bind(Bindings.concat("Evasion: ", data.evasionProperty().asString()));

        joinBattleLabel = new Label();
        joinBattleLabel
                .textProperty()
                .bind(Bindings.concat("Join Battle: ", data.joinBattleProperty().asString()));

        statsList
                .getChildren()
                .addAll(
                        naturalSoak,
                        armorSoak,
                        totalSoakVal,
                        hardnessVal,
                        dodgeBaseLabel,
                        joinBattleLabel);
        combatBox.getChildren().addAll(combatLabel, statsList);
        return combatBox;
    }

    private VBox createSocialStatsSection(CharacterData data) {
        VBox box = new VBox(5);
        box.getChildren().add(new Label("Social Stats"));
        resolveBaseLabel = new Label();
        resolveBaseLabel
                .textProperty()
                .bind(Bindings.concat("Resolve: ", data.resolveProperty().asString()));
        guileBaseLabel = new Label();
        guileBaseLabel
                .textProperty()
                .bind(Bindings.concat("Guile: ", data.guileProperty().asString()));
        box.getChildren().addAll(resolveBaseLabel, guileBaseLabel);
        return box;
    }

    private VBox createAttackPoolsSection(CharacterData data) {
        VBox section = new VBox(5);
        Label title = new Label("Attack Pools");
        title.getStyleClass().add("subsection-title");

        attackList = new VBox(8);
        attackList.getStyleClass().add("merit-row-container");
        attackList.setPadding(new Insets(10));

        // Headers row
        HBox headerRow = new HBox(15);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        String[] headers = {"St", "Weapon", "Withering", "Decisive", "Damage", "Parry"};
        int[] widths = {20, 120, 100, 60, 60, 50};
        for (int i = 0; i < headers.length; i++) {
            Label hl = new Label(headers[i]);
            hl.getStyleClass().add("sidebar-stat-header");
            hl.setPrefWidth(widths[i]);
            headerRow.getChildren().add(hl);
        }

        VBox listContainer = new VBox(8);
        Bindings.bindContent(
                listContainer.getChildren(),
                viewModel.getAttackPoolRows().stream()
                        .map(AttackPoolRowView::new)
                        .collect(
                                Collectors.collectingAndThen(
                                        Collectors.toList(), FXCollections::observableArrayList)));

        attackList.getChildren().addAll(headerRow, listContainer);

        section.getChildren().addAll(title, attackList);
        return section;
    }

    private VBox createGreatCurseSection(CharacterData data) {
        VBox section = new VBox(10);
        Label title = new Label("Great Curse");
        title.getStyleClass().add("section-title");

        VBox content = new VBox(8);
        content.getStyleClass().add("social-stats-container");
        content.setPadding(new Insets(10));

        TextArea triggerArea = new TextArea();
        triggerArea.setId("stats_limit_trigger");
        triggerArea.setPromptText("Enter your Limit Trigger...");
        triggerArea.setPrefRowCount(2);
        triggerArea.setWrapText(true);
        triggerArea.textProperty().bindBidirectional(data.limitTriggerProperty());

        HBox limitBox = new HBox(10);
        limitBox.setAlignment(Pos.CENTER_LEFT);
        Label limitLabel = new Label("Limit:");
        DotSelector limitSelector = new DotSelector(limitLabel, data.limitProperty(), 0, 10);
        limitSelector.contextIdProperty().set("Stats");
        limitSelector.descriptionProperty().set("Limit");
        limitSelector.targetIdProperty().set("stats_limit");
        limitSelector.setId("stats_limit");
        limitBox.getChildren().addAll(limitLabel, limitSelector);

        content.getChildren().addAll(new Label("Limit Trigger:"), triggerArea, limitBox);
        section.getChildren().addAll(title, content);
        return section;
    }
}
