package com.vibethema.ui;

import com.vibethema.model.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;
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
            // Fill content staggered over multiple pulses to ensure UI thread remains responsive
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

                                                // Step 4: Stats and Combat (final pulse)
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
        DotSelector essSelector = new DotSelector(data.essenceProperty(), 1, 5);
        essSelector.contextIdProperty().set("Stats");
        essSelector.descriptionProperty().set("Change Essence");
        essSelector.targetIdProperty().set("stats.essence");
        essSelector.setId("stats.essence");
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
        DotSelector wpSelector = new DotSelector(data.willpowerProperty(), 5, 10);
        wpSelector.contextIdProperty().set("Stats");
        wpSelector.descriptionProperty().set("Change Willpower");
        wpSelector.targetIdProperty().set("stats.willpower");
        wpSelector.setId("stats.willpower");
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
        abilitiesSection.getChildren().add(new Label("Abilities (C=Caste, F=Favored)"));

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

        Bindings.bindContent(
                craftList.getChildren(),
                viewModel.getCraftRows().stream()
                        .map(
                                rvm -> {
                                    CraftRowView view = new CraftRowView(rvm);
                                    view.getChildren().stream()
                                            .filter(n -> n instanceof DotSelector)
                                            .map(n -> (DotSelector) n)
                                            .findFirst()
                                            .ifPresent(
                                                    ds -> {
                                                        ds.minDotsProperty()
                                                                .bind(
                                                                        Bindings.createIntegerBinding(
                                                                                () -> {
                                                                                    if (!data.getFavoredAbility(
                                                                                                    Ability.CRAFT)
                                                                                            .get()) return 0;
                                                                                    int totalDots =
                                                                                            data.getCrafts()
                                                                                                    .stream()
                                                                                                    .mapToInt(
                                                                                                            CraftAbility
                                                                                                                    ::getRating)
                                                                                                    .sum();
                                                                                    return (totalDots
                                                                                                            - rvm.ratingProperty()
                                                                                                                    .get()
                                                                                                    > 0)
                                                                                            ? 0
                                                                                            : 1;
                                                                                },
                                                                                data.getFavoredAbility(
                                                                                        Ability.CRAFT),
                                                                                rvm.ratingProperty()));
                                                        ds.contextIdProperty().set("Stats");
                                                        ds.descriptionProperty().set(
                                                                "Change Craft (" + rvm.expertiseProperty().get() + ")");
                                                    });
                                    return view;
                                })
                        .collect(
                                Collectors.collectingAndThen(
                                        Collectors.toList(), FXCollections::observableArrayList)));

        Button addBtn = new Button("+ Add Craft");
        addBtn.setOnAction(e -> data.getCrafts().add(new CraftAbility("", 0)));
        section.getChildren().addAll(new Label("Crafts"), craftList, addBtn);
        return section;
    }

    private VBox createSpecialtiesSection(CharacterData data) {
        VBox section = new VBox(10);
        specList = new VBox(8);

        // Use declarative binding for the specialties list
        Bindings.bindContent(
                specList.getChildren(),
                viewModel.getSpecialtyRows().stream()
                        .map(SpecialtyRowView::new)
                        .collect(
                                Collectors.collectingAndThen(
                                        Collectors.toList(), FXCollections::observableArrayList)));

        Button addBtn = new Button("+ Add Specialty");
        addBtn.setOnAction(e -> data.getSpecialties().add(new Specialty("", "")));
        section.getChildren().addAll(new Label("Specialties"), specList, addBtn);
        return section;
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
        // Group by penalty, preserving order as much as possible, or using a fixed order.
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
        triggerArea.setId("stats.limit_trigger");
        triggerArea.setPromptText("Enter your Limit Trigger...");
        triggerArea.setPrefRowCount(2);
        triggerArea.setWrapText(true);
        triggerArea.textProperty().bindBidirectional(data.limitTriggerProperty());

        HBox limitBox = new HBox(10);
        limitBox.setAlignment(Pos.CENTER_LEFT);
        DotSelector limitSelector = new DotSelector(data.limitProperty(), 0, 10);
        limitSelector.contextIdProperty().set("Stats");
        limitSelector.descriptionProperty().set("Change Limit");
        limitSelector.targetIdProperty().set("stats.limit");
        limitSelector.setId("stats.limit");
        limitBox.getChildren()
                .addAll(new Label("Limit:"), limitSelector);

        content.getChildren().addAll(new Label("Limit Trigger:"), triggerArea, limitBox);
        section.getChildren().addAll(title, content);
        return section;
    }
}
