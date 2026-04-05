package com.vibethema.ui;

import com.vibethema.model.*;
import com.vibethema.model.logic.CreationRuleEngine;
import com.vibethema.model.logic.CreationRuleEngine.CreationStatus;
import com.vibethema.viewmodel.StatsViewModel;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import javafx.beans.binding.Bindings;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class StatsTab extends ScrollPane implements JavaView<StatsViewModel>, Initializable {

    @InjectViewModel
    private StatsViewModel viewModel;

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
    private GridPane attackGrid;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setFitToWidth(true);
        getStyleClass().add("scroll-pane-custom");
        if (viewModel != null) {
            setContent(createContent());
        }
    }

    private VBox createContent() {
        VBox content = new VBox(20);
        content.getStyleClass().add("content-area");
        content.setPadding(new Insets(20));

        CharacterData data = viewModel.getData();

        content.getChildren().addAll(
                createBasicAdvantagesSection(data),
                createAttributesSection(data),
                createAbilitiesAndSideStuff(data),
                new Separator(),
                createStatsRow(data),
                createAttackPoolsSection(data),
                createGreatCurseSection(data)
        );
        return content;
    }

    private VBox createBasicAdvantagesSection(CharacterData data) {
        VBox section = new VBox(15);
        Label advTitle = new Label("Advantages");
        advTitle.getStyleClass().add("section-title");

        HBox topRow = new HBox(50);

        VBox wpBox = new VBox(5);
        wpBox.getChildren().addAll(new Label("Willpower"), new DotSelector(data.willpowerProperty(), 5, 10));

        VBox essBox = new VBox(5);
        essBox.getChildren().addAll(new Label("Essence"), new DotSelector(data.essenceProperty(), 1, 5));

        VBox motesBox = new VBox(5);
        Label motesLabel = new Label("Mote Pools");
        motesLabel.getStyleClass().add("subsection-title");
        personalLabel = new Label();
        peripheralLabel = new Label();
        motesBox.getChildren().addAll(motesLabel, personalLabel, peripheralLabel);

        updateMotes(data);
        data.essenceProperty().addListener((obs, oldV, newV) -> updateMotes(data));

        topRow.getChildren().addAll(wpBox, essBox, motesBox);
        section.getChildren().addAll(advTitle, topRow);
        return section;
    }

    private void updateMotes(CharacterData data) {
        CreationStatus status = CreationRuleEngine.calculateStatus(data);
        personalLabel.setText("Personal: " + status.personalMotes);
        peripheralLabel.setText("Peripheral: " + status.peripheralMotes);
    }

    private VBox createAttributesSection(CharacterData data) {
        VBox section = new VBox(10);
        Label title = new Label("Attributes");
        title.getStyleClass().add("section-title");

        HBox columns = new HBox(30);
        columns.getChildren().addAll(
                createAttributeColumn("Physical", Attribute.Category.PHYSICAL, SystemData.PHYSICAL_ATTRIBUTES, data),
                createAttributeColumn("Social", Attribute.Category.SOCIAL, SystemData.SOCIAL_ATTRIBUTES, data),
                createAttributeColumn("Mental", Attribute.Category.MENTAL, SystemData.MENTAL_ATTRIBUTES, data)
        );
        section.getChildren().addAll(title, columns);
        return section;
    }

    private VBox createAttributeColumn(String title, Attribute.Category category, List<Attribute> attrs, CharacterData data) {
        VBox col = new VBox(10);
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(title);
        l.getStyleClass().add("subsection-title");
        
        ComboBox<AttributePriority> priorityBox = new ComboBox<>();
        priorityBox.getItems().setAll(AttributePriority.values());
        priorityBox.valueProperty().bindBidirectional(data.getAttributePriority(category));
        priorityBox.visibleProperty().bind(data.modeProperty().isEqualTo(CharacterMode.CREATION));
        priorityBox.managedProperty().bind(priorityBox.visibleProperty());

        Label priorityLabel = new Label();
        priorityLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            AttributePriority p = data.getAttributePriority(category).get();
            return p == null ? "" : p.name();
        }, data.getAttributePriority(category)));
        priorityLabel.visibleProperty().bind(data.modeProperty().isEqualTo(CharacterMode.EXPERIENCED));
        priorityLabel.managedProperty().bind(priorityLabel.visibleProperty());

        header.getChildren().addAll(l, priorityBox, priorityLabel);
        col.getChildren().add(header);

        for (Attribute a : attrs) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            Label name = new Label(a.getDisplayName());
            name.setPrefWidth(80);
            row.getChildren().addAll(name, new DotSelector(data.getAttribute(a), 1));
            col.getChildren().add(row);
        }
        return col;
    }

    private HBox createAbilitiesAndSideStuff(CharacterData data) {
        VBox abilitiesSection = new VBox(10);
        abilitiesSection.getChildren().add(new Label("Abilities (C=Caste, F=Favored)"));
        
        GridPane abilGrid = new GridPane();
        abilGrid.setHgap(20); abilGrid.setVgap(10);

        int rowCount = 0, colCount = 0;
        for (Ability abil : SystemData.ABILITIES) {
            if (abil == Ability.CRAFT || abil == Ability.MARTIAL_ARTS) continue;
            AbilitySelectionComponent comp = new AbilitySelectionComponent(data, abil, data.getAbility(abil), 
                data.getCasteAbility(abil), data.getFavoredAbility(abil));
            comp.setOnNameClick(e -> { if (e.getClickCount() == 2) viewModel.jumpToCharms(abil.getDisplayName()); });
            
            if (abil == Ability.BRAWL) {
                comp.getSelector().minDotsProperty().bind(Bindings.createIntegerBinding(() -> {
                    return (!data.getFavoredAbility(Ability.BRAWL).get() || viewModel.maTotalDotsProperty().get() > 0) ? 0 : 1;
                }, data.getFavoredAbility(Ability.BRAWL), viewModel.maTotalDotsProperty()));
            }
            abilGrid.add(comp, colCount, rowCount++);
            if (rowCount >= 13) { rowCount = 0; colCount++; }
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
        refreshCraftsContent(data);
        data.getCrafts().addListener((javafx.collections.ListChangeListener<? super CraftAbility>) c -> refreshCraftsContent(data));
        
        Button addBtn = new Button("+ Add Craft");
        addBtn.setOnAction(e -> data.getCrafts().add(new CraftAbility("", 0)));
        section.getChildren().addAll(new Label("Crafts"), craftList, addBtn);
        return section;
    }

    private void refreshCraftsContent(CharacterData data) {
        if (craftList == null) return;
        craftList.getChildren().clear();
        int totalDots = data.getCrafts().stream().mapToInt(CraftAbility::getRating).sum();
        for (CraftAbility ca : data.getCrafts()) {
            HBox row = new HBox(12);
            TextField f = new TextField();
            f.setPromptText("Expertise");
            f.textProperty().bindBidirectional(ca.expertiseProperty());
            DotSelector ds = new DotSelector(ca.ratingProperty(), 0);
            ds.minDotsProperty().bind(Bindings.createIntegerBinding(() -> {
                if (!data.getFavoredAbility(Ability.CRAFT).get()) return 0;
                return (totalDots - ca.getRating() > 0) ? 0 : 1;
            }, data.getFavoredAbility(Ability.CRAFT)));
            row.getChildren().addAll(f, ds);
            craftList.getChildren().add(row);
        }
    }

    private VBox createSpecialtiesSection(CharacterData data) {
        VBox section = new VBox(10);
        specList = new VBox(8);
        refreshSpecsContent(data);
        data.getSpecialties().addListener((javafx.collections.ListChangeListener<? super Specialty>) c -> refreshSpecsContent(data));
        
        Button addBtn = new Button("+ Add Specialty");
        addBtn.setOnAction(e -> data.getSpecialties().add(new Specialty("", "")));
        section.getChildren().addAll(new Label("Specialties"), specList, addBtn);
        return section;
    }

    private void refreshSpecsContent(CharacterData data) {
        if (specList == null) return;
        specList.getChildren().clear();
        for (Specialty s : data.getSpecialties()) {
            HBox row = new HBox(12);
            TextField f = new TextField();
            f.textProperty().bindBidirectional(s.nameProperty());
            ComboBox<String> cb = new ComboBox<>();
            cb.getItems().addAll(SystemData.ABILITIES.stream().map(Ability::getDisplayName).collect(Collectors.toList()));
            cb.valueProperty().bindBidirectional(s.abilityProperty());
            row.getChildren().addAll(f, cb);
            specList.getChildren().add(row);
        }
    }

    private HBox createStatsRow(CharacterData data) {
        HBox row = new HBox(40);
        row.getChildren().addAll(createHealthSection(data), createCombatStatsSection(data), createSocialStatsSection(data));
        return row;
    }

    private VBox createHealthSection(CharacterData data) {
        VBox healthBox = new VBox(5);
        healthBox.getChildren().add(new Label("Health Levels"));
        trackBoxes = new VBox(8);
        updateHealth(data);
        data.getAttribute(Attribute.STAMINA).addListener((obs, old, nv) -> updateHealth(data));
        data.getAbility(Ability.RESISTANCE).addListener((obs, old, nv) -> updateHealth(data));
        data.getUnlockedCharms().addListener((javafx.collections.ListChangeListener<? super PurchasedCharm>) c -> updateHealth(data));
        healthBox.getChildren().add(trackBoxes);
        return healthBox;
    }

    private void updateHealth(CharacterData data) {
        if (trackBoxes == null) return;
        trackBoxes.getChildren().clear();
        
        List<String> levels = data.getHealthLevels();
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
        naturalSoak.textProperty().bind(Bindings.concat("Natural Soak: ", data.naturalSoakProperty().asString()));
        Label armorSoak = new Label();
        armorSoak.textProperty().bind(Bindings.concat("Armor Soak: +", data.armorSoakProperty().asString()));
        totalSoakVal = new Label();
        totalSoakVal.textProperty().bind(Bindings.concat("Total Soak: ", data.totalSoakProperty().asString()));
        totalSoakVal.setStyle("-fx-font-weight: bold; -fx-text-fill: #3498db;");

        hardnessVal = new Label();
        hardnessVal.textProperty().bind(Bindings.concat("Hardness: ", data.totalHardnessProperty().asString()));

        dodgeBaseLabel = new Label();
        dodgeBaseLabel.textProperty().bind(Bindings.concat("Evasion: ", data.evasionProperty().asString()));

        joinBattleLabel = new Label();
        joinBattleLabel.textProperty().bind(Bindings.concat("Join Battle: ", data.joinBattleProperty().asString()));

        statsList.getChildren().addAll(naturalSoak, armorSoak, totalSoakVal, hardnessVal, dodgeBaseLabel, joinBattleLabel);
        combatBox.getChildren().addAll(combatLabel, statsList);
        return combatBox;
    }

    private VBox createSocialStatsSection(CharacterData data) {
        VBox box = new VBox(5);
        box.getChildren().add(new Label("Social Stats"));
        resolveBaseLabel = new Label();
        resolveBaseLabel.textProperty().bind(Bindings.concat("Resolve: ", data.resolveProperty().asString()));
        guileBaseLabel = new Label();
        guileBaseLabel.textProperty().bind(Bindings.concat("Guile: ", data.guileProperty().asString()));
        box.getChildren().addAll(resolveBaseLabel, guileBaseLabel);
        return box;
    }

    private VBox createAttackPoolsSection(CharacterData data) {
        VBox section = new VBox(5);
        Label title = new Label("Attack Pools");
        title.getStyleClass().add("subsection-title");

        attackGrid = new GridPane();
        attackGrid.setHgap(20); attackGrid.setVgap(8);
        attackGrid.getStyleClass().add("merit-row-container");
        attackGrid.setPadding(new Insets(10));

        String[] headers = { "Weapon", "Withering", "Decisive", "Damage", "Parry" };
        for (int i = 0; i < headers.length; i++) {
            Label hl = new Label(headers[i]);
            hl.getStyleClass().add("sidebar-stat-header");
            attackGrid.add(hl, i, 0);
        }

        // Simulating the refresh logic from BuilderUI
        // In a full MVVM, this might be a ListView or a repeated component
        // For now, we'll just show a placeholder or fixed rows if possible
        Label placeholder = new Label("Equip weapons in the Equipment tab to see attack pools.");
        placeholder.setStyle("-fx-font-style: italic; -fx-text-fill: #888;");
        attackGrid.add(placeholder, 0, 1, 5, 1);

        section.getChildren().addAll(title, attackGrid);
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
        triggerArea.setPromptText("Enter your Limit Trigger...");
        triggerArea.setPrefRowCount(2);
        triggerArea.setWrapText(true);
        triggerArea.textProperty().bindBidirectional(data.limitTriggerProperty());

        HBox limitBox = new HBox(10);
        limitBox.setAlignment(Pos.CENTER_LEFT);
        limitBox.getChildren().addAll(new Label("Limit:"), new DotSelector(data.limitProperty(), 0, 10));

        content.getChildren().addAll(new Label("Limit Trigger:"), triggerArea, limitBox);
        section.getChildren().addAll(title, content);
        return section;
    }
}
