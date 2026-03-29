package com.vibethema.ui;

import com.vibethema.model.CharacterData;
import com.vibethema.model.Charm;
import com.vibethema.model.PurchasedCharm;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.scene.shape.CubicCurve;
import javafx.beans.binding.Bindings;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.stage.FileChooser;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCombination;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import com.vibethema.model.CharacterSaveState;
import com.google.gson.GsonBuilder;
import com.vibethema.model.Keyword;

public class BuilderUI extends BorderPane {
    private CharacterData data;
    private File currentFile = null;
    
    private Label physicalLabel = new Label();
    private Label socialLabel = new Label();
    private Label mentalLabel = new Label();
    private Label abilitiesLabel = new Label();
    private Label casteLabel = new Label();
    private Label favoredLabel = new Label();
    private Label charmsLabel = new Label();
    private Label bpLabel = new Label();
    
    private Pane charmCanvas;
    private Map<String, VBox> charmNodeMap = new HashMap<>();
    private List<Charm> currentAbilityCharms = new ArrayList<>();
    private Map<String, String> keywordDefs = new HashMap<>();

    public BuilderUI(CharacterData data) {
        this.data = data;
        getStyleClass().add("main-pane");
        loadKeywords();
        
        setTop(createTopSection());
        
        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("tab-pane");
        
        Tab statsTab = new Tab("Stats");
        statsTab.setClosable(false);
        ScrollPane statsScroll = new ScrollPane(createContent());
        statsScroll.setFitToWidth(true);
        statsScroll.getStyleClass().add("scroll-pane-custom");
        statsTab.setContent(statsScroll);
        
        Tab charmsTab = new Tab("Charms");
        charmsTab.setClosable(false);
        charmsTab.setContent(createCharmsContent());
        
        tabPane.getTabs().addAll(statsTab, charmsTab);
        setCenter(tabPane);
        
        setBottom(createFooter());
        
        setupListeners();
        updateFooter();
        
        data.dirtyProperty().addListener((obs, oldV, newV) -> updateWindowTitle());
    }

    private VBox createTopSection() {
        VBox topContainer = new VBox();
        MenuBar menuBar = new MenuBar();
        menuBar.setUseSystemMenuBar(true);
        
        Menu fileMenu = new Menu("File");
        
        MenuItem saveItem = new MenuItem("Save");
        saveItem.setAccelerator(KeyCombination.keyCombination("Shortcut+S"));
        saveItem.setOnAction(e -> saveCharacter());
        
        MenuItem saveAsItem = new MenuItem("Save As...");
        saveAsItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Shift+S"));
        saveAsItem.setOnAction(e -> saveCharacterAs());
        
        MenuItem loadItem = new MenuItem("Load Character");
        loadItem.setAccelerator(KeyCombination.keyCombination("Shortcut+O"));
        loadItem.setOnAction(e -> loadCharacter());
        
        fileMenu.getItems().addAll(saveItem, saveAsItem, loadItem);
        menuBar.getMenus().add(fileMenu);
        
        topContainer.getChildren().addAll(menuBar, createHeader());
        return topContainer;
    }

    private void saveCharacter() {
        if (currentFile != null) {
            performSave(currentFile);
        } else {
            saveCharacterAs();
        }
    }

    private void saveCharacterAs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Character As");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Vibethema Save File", "*.vbtm"));
        // Default filename based on character name if present
        if (data.nameProperty().get() != null && !data.nameProperty().get().isEmpty()) {
            fileChooser.setInitialFileName(data.nameProperty().get());
        }
        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            currentFile = file;
            performSave(file);
            updateWindowTitle();
        }
    }

    private void performSave(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            CharacterSaveState state = data.exportState();
            new GsonBuilder().setPrettyPrinting().create().toJson(state, writer);
            data.setDirty(false);
            updateWindowTitle();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private void loadCharacter() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Character");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Vibethema Save File", "*.vbtm"));
        File file = fileChooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            try (FileReader reader = new FileReader(file)) {
                CharacterSaveState state = new Gson().fromJson(reader, CharacterSaveState.class);
                if (state != null) {
                    data.importState(state);
                    currentFile = file;
                    updateWindowTitle();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void updateWindowTitle() {
        if (getScene() != null && getScene().getWindow() instanceof javafx.stage.Stage stage) {
            String title = "Vibethema";
            if (currentFile != null) {
                title += " - " + currentFile.getName();
            }
            if (data.isDirty()) {
                title += "*";
            }
            stage.setTitle(title);
        }
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.getStyleClass().add("header");
        
        Text title = new Text("Exalted 3rd Edition Solar Builder");
        title.getStyleClass().add("title-text");
        
        HBox basicInfo = new HBox(15);
        basicInfo.getStyleClass().add("info-bar");
        
        TextField nameField = new TextField();
        nameField.setPromptText("Character Name");
        nameField.textProperty().bindBidirectional(data.nameProperty());
        
        ComboBox<CharacterData.Caste> casteBox = new ComboBox<>();
        casteBox.getItems().addAll(CharacterData.Caste.values());
        casteBox.setValue(CharacterData.Caste.NONE);
        data.casteProperty().bindBidirectional(casteBox.valueProperty());
        
        ComboBox<String> supernalDropdown = new ComboBox<>();
        supernalDropdown.setPrefWidth(120);
        
        Runnable updateSupernalDropdown = () -> {
            String current = data.supernalAbilityProperty().get();
            List<String> options = new ArrayList<>();
            options.add(""); // None
            for (String abil : CharacterData.ABILITIES) {
                if (data.getCasteAbility(abil).get()) {
                    options.add(abil);
                }
            }
            supernalDropdown.getItems().setAll(options);
            if (options.contains(current)) {
                supernalDropdown.setValue(current);
            } else {
                supernalDropdown.setValue("");
                data.supernalAbilityProperty().set("");
            }
        };
        
        supernalDropdown.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && supernalDropdown.getItems().contains(newV)) {
                data.supernalAbilityProperty().set(newV);
            } else {
                data.supernalAbilityProperty().set("");
            }
        });
        
        data.supernalAbilityProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && !newV.equals(supernalDropdown.getValue())) {
                if (supernalDropdown.getItems().contains(newV)) {
                    supernalDropdown.setValue(newV);
                } else if (newV.isEmpty()) {
                    supernalDropdown.setValue("");
                }
            }
        });
        
        for (String abil : CharacterData.ABILITIES) {
            data.getCasteAbility(abil).addListener((obs, oldV, newV) -> updateSupernalDropdown.run());
        }
        updateSupernalDropdown.run();
        
        basicInfo.getChildren().addAll(new Label("Name:"), nameField, new Label("Caste:"), casteBox, new Label("Supernal:"), supernalDropdown);
        
        header.getChildren().addAll(title, basicInfo);
        return header;
    }
    
    private VBox createFooter() {
        VBox footer = new VBox(10);
        footer.getStyleClass().add("footer");
        
        HBox row1 = new HBox(20);
        HBox row2 = new HBox(20);
        
        bpLabel.getStyleClass().add("bp-label");
        
        row1.getChildren().addAll(
            new Label("Attributes (8/6/4):"), physicalLabel, socialLabel, mentalLabel,
            new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }},
            casteLabel, favoredLabel
        );
        
        row2.getChildren().addAll(
            new Label("Pools:"), abilitiesLabel, charmsLabel,
            new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }},
            bpLabel
        );
        
        footer.getChildren().addAll(row1, row2);
        return footer;
    }
    
    private void setupListeners() {
        for (String attr : CharacterData.ATTRIBUTES) {
            data.getAttribute(attr).addListener((obs, oldV, newV) -> updateFooter());
        }
        for (String abil : CharacterData.ABILITIES) {
            data.getAbility(abil).addListener((obs, oldV, newV) -> updateFooter());
            data.getCasteAbility(abil).addListener((obs, oldV, newV) -> updateFooter());
            data.getFavoredAbility(abil).addListener((obs, oldV, newV) -> updateFooter());
        }
        data.willpowerProperty().addListener((obs, oldV, newV) -> updateFooter());
        data.casteProperty().addListener((obs, oldV, newV) -> {
            if (oldV != newV) {
                for (String abil : CharacterData.ABILITIES) {
                    data.getCasteAbility(abil).set(false);
                }
                data.supernalAbilityProperty().set("");
            }
            updateFooter();
        });
        
        data.supernalAbilityProperty().addListener((obs, oldV, newV) -> {
            updateFooter();
            updateWebNodeStyles();
        });
        
        data.getUnlockedCharms().addListener((javafx.collections.ListChangeListener.Change<? extends PurchasedCharm> c) -> {
            updateFooter();
            updateWebNodeStyles();
        });
    }

    private void updateFooter() {
        int phys = data.getAttributeTotal(CharacterData.PHYSICAL_ATTRIBUTES);
        int soc = data.getAttributeTotal(CharacterData.SOCIAL_ATTRIBUTES);
        int ment = data.getAttributeTotal(CharacterData.MENTAL_ATTRIBUTES);
        int abils = data.getAbilityTotal();
        int bp = data.getBonusPointsSpent();
        
        long casteCount = CharacterData.ABILITIES.stream().filter(a -> data.getCasteAbility(a).get()).count();
        long favoredCount = CharacterData.ABILITIES.stream().filter(a -> data.getFavoredAbility(a).get()).count();
        int charmsCount = data.getUnlockedCharms().size();
        
        physicalLabel.setText("Phys: " + phys);
        socialLabel.setText("Soc: " + soc);
        mentalLabel.setText("Ment: " + ment);
        
        abilitiesLabel.setText("Abils: " + abils + "/28");
        charmsLabel.setText("Charms: " + charmsCount + "/15");
        casteLabel.setText("Caste: " + casteCount + "/5");
        favoredLabel.setText("Favored: " + favoredCount + "/5");
        bpLabel.setText("Bonus Points: " + bp + "/15");
        
        updateWebNodeStyles();
    }

    private VBox createContent() {
        VBox content = new VBox(20);
        content.getStyleClass().add("content-area");
        content.setPadding(new Insets(20));
        
        VBox advantagesSection = createAdvantagesSection();
        
        VBox attributesSection = new VBox(10);
        Label attrTitle = new Label("Attributes");
        attrTitle.getStyleClass().add("section-title");
        
        HBox attrColumns = new HBox(30);
        attrColumns.getChildren().addAll(
            createAttributeColumn("Physical", CharacterData.PHYSICAL_ATTRIBUTES),
            createAttributeColumn("Social", CharacterData.SOCIAL_ATTRIBUTES),
            createAttributeColumn("Mental", CharacterData.MENTAL_ATTRIBUTES)
        );
        attributesSection.getChildren().addAll(attrTitle, attrColumns);
        
        VBox abilitiesSection = new VBox(10);
        Label abilTitle = new Label("Abilities (C=Caste, F=Favored)");
        abilTitle.getStyleClass().add("section-title");
        
        GridPane abilGrid = new GridPane();
        abilGrid.setHgap(20);
        abilGrid.setVgap(10);
        
        javafx.beans.property.IntegerProperty casteCount = new javafx.beans.property.SimpleIntegerProperty(0);
        javafx.beans.property.IntegerProperty favoredCount = new javafx.beans.property.SimpleIntegerProperty(0);
        
        Runnable updateCounts = () -> {
            int c = 0;
            int f = 0;
            for (String abil : CharacterData.ABILITIES) {
                if (data.getCasteAbility(abil).get()) c++;
                if (data.getFavoredAbility(abil).get()) f++;
            }
            casteCount.set(c);
            favoredCount.set(f);
        };
        for (String abil : CharacterData.ABILITIES) {
            data.getCasteAbility(abil).addListener((obs, old, nv) -> updateCounts.run());
            data.getFavoredAbility(abil).addListener((obs, old, nv) -> updateCounts.run());
        }
        updateCounts.run();
        
        int row = 0;
        int col = 0;
        for (String ability : CharacterData.ABILITIES) {
            HBox rowBox = new HBox(6);
            rowBox.setAlignment(Pos.CENTER_LEFT);
            
            CheckBox casteBox = new CheckBox("C");
            casteBox.getStyleClass().add("caste-checkbox");
            casteBox.selectedProperty().bindBidirectional(data.getCasteAbility(ability));
            casteBox.disableProperty().bind(Bindings.createBooleanBinding(() -> {
                CharacterData.Caste c = data.casteProperty().get();
                boolean notInCasteList = c == null || c == CharacterData.Caste.NONE || !CharacterData.CASTE_OPTIONS.get(c).contains(ability);
                boolean atLimit = casteCount.get() >= 5 && !data.getCasteAbility(ability).get();
                return notInCasteList || atLimit;
            }, data.casteProperty(), casteCount, data.getCasteAbility(ability)));

            CheckBox favoredBox = new CheckBox("F");
            favoredBox.getStyleClass().add("favored-checkbox");
            favoredBox.selectedProperty().bindBidirectional(data.getFavoredAbility(ability));
            favoredBox.disableProperty().bind(Bindings.createBooleanBinding(() -> {
                boolean isCaste = data.getCasteAbility(ability).get();
                boolean atLimit = favoredCount.get() >= 5 && !data.getFavoredAbility(ability).get();
                return isCaste || atLimit;
            }, data.getCasteAbility(ability), favoredCount, data.getFavoredAbility(ability)));

            data.getCasteAbility(ability).addListener((obs, oldV, newV) -> {
                if (newV) {
                    favoredBox.setSelected(false);
                }
            });
            
            data.getFavoredAbility(ability).addListener((obs, oldV, newV) -> {
                if (newV && data.getAbility(ability).get() == 0) {
                    data.getAbility(ability).set(1);
                }
            });
            
            Label abLabel = new Label(ability);
            abLabel.setPrefWidth(95);
            
            data.casteProperty().addListener((obs, oldV, newV) -> {
                boolean isOption = newV != null && newV != CharacterData.Caste.NONE && CharacterData.CASTE_OPTIONS.get(newV).contains(ability);
                if (isOption) {
                    if (!abLabel.getStyleClass().contains("caste-option")) {
                        abLabel.getStyleClass().add("caste-option");
                    }
                } else {
                    abLabel.getStyleClass().remove("caste-option");
                }
            });
            
            Runnable updateExcellency = () -> {
                if (data.hasExcellency(ability)) {
                    abLabel.setText(ability + " [E]");
                    if (!abLabel.getStyleClass().contains("excellency-label")) {
                        abLabel.getStyleClass().add("excellency-label");
                    }
                } else {
                    abLabel.setText(ability);
                    abLabel.getStyleClass().remove("excellency-label");
                }
            };
            
            data.getAbility(ability).addListener((obs, oldV, newV) -> updateExcellency.run());
            data.getCasteAbility(ability).addListener((obs, oldV, newV) -> updateExcellency.run());
            data.getFavoredAbility(ability).addListener((obs, oldV, newV) -> updateExcellency.run());
            data.supernalAbilityProperty().addListener((obs, oldV, newV) -> updateExcellency.run());
            data.getUnlockedCharms().addListener((javafx.collections.ListChangeListener.Change<? extends PurchasedCharm> change) -> {
                updateExcellency.run();
            });
            updateExcellency.run();
            
            DotSelector selector = new DotSelector(data.getAbility(ability), 0);
            rowBox.getChildren().addAll(casteBox, favoredBox, abLabel, selector);
            
            abilGrid.add(rowBox, col, row);
            row++;
            if (row >= 13) {
                row = 0;
                col++;
            }
        }
        abilitiesSection.getChildren().addAll(abilTitle, abilGrid);
        
        content.getChildren().addAll(advantagesSection, attributesSection, abilitiesSection);
        return content;
    }
    
    private VBox createAdvantagesSection() {
        VBox advantagesSection = new VBox(15);
        Label advTitle = new Label("Advantages");
        advTitle.getStyleClass().add("section-title");
        
        HBox topRow = new HBox(50);
        
        VBox wpBox = new VBox(5);
        Label wpLabel = new Label("Willpower");
        wpLabel.getStyleClass().add("subsection-title");
        DotSelector wpSelector = new DotSelector(data.willpowerProperty(), 5, 10);
        wpBox.getChildren().addAll(wpLabel, wpSelector);
        
        VBox essBox = new VBox(5);
        Label essLabel = new Label("Essence");
        essLabel.getStyleClass().add("subsection-title");
        DotSelector essSelector = new DotSelector(data.essenceProperty(), 1, 5);
        essBox.getChildren().addAll(essLabel, essSelector);
        
        VBox motesBox = new VBox(5);
        Label motesLabel = new Label("Mote Pools");
        motesLabel.getStyleClass().add("subsection-title");
        
        Label personalLabel = new Label();
        Label peripheralLabel = new Label();
        personalLabel.getStyleClass().add("label");
        peripheralLabel.getStyleClass().add("label");
        
        Runnable updateMotes = () -> {
            personalLabel.setText("Personal: " + data.getPersonalMotes());
            peripheralLabel.setText("Peripheral: " + data.getPeripheralMotes());
            updateFooter();
        };
        data.essenceProperty().addListener((obs, oldV, newV) -> updateMotes.run());
        updateMotes.run();
        
        motesBox.getChildren().addAll(motesLabel, personalLabel, peripheralLabel);
        
        topRow.getChildren().addAll(wpBox, essBox, motesBox);
        
        VBox healthBox = new VBox(5);
        Label healthLabel = new Label("Health Levels (Click to toggle damage)");
        healthLabel.getStyleClass().add("subsection-title");
        
        HBox trackBoxes = new HBox(20);
        trackBoxes.getChildren().addAll(
            createHealthLevel("-0", 1),
            createHealthLevel("-1", 2),
            createHealthLevel("-2", 2),
            createHealthLevel("-4", 1),
            createHealthLevel("Incap", 1)
        );
        healthBox.getChildren().addAll(healthLabel, trackBoxes);
        
        advantagesSection.getChildren().addAll(advTitle, topRow, healthBox);
        return advantagesSection;
    }
    
    private HBox createHealthLevel(String label, int count) {
        HBox box = new HBox(5);
        box.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label);
        l.getStyleClass().add("label");
        box.getChildren().add(l);
        for(int i=0; i<count; i++) {
            StackPane pane = new StackPane();
            javafx.scene.shape.Rectangle rect = new javafx.scene.shape.Rectangle(16, 16);
            rect.setFill(javafx.scene.paint.Color.web("#2d2d2d"));
            rect.setStroke(javafx.scene.paint.Color.web("#d4af37"));
            rect.setStrokeWidth(1.0);
            
            Label mark = new Label("");
            mark.setStyle("-fx-text-fill: #f9f6e6; -fx-font-weight: bold; -fx-font-size: 14px;");
            
            pane.getChildren().addAll(rect, mark);
            pane.setCursor(javafx.scene.Cursor.HAND);
            pane.setOnMouseClicked(e -> {
                String current = mark.getText();
                switch (current) {
                    case "": mark.setText("/"); break;
                    case "/": mark.setText("X"); break;
                    case "X": mark.setText("*"); break;
                    case "*": mark.setText(""); break;
                }
            });
            box.getChildren().add(pane);
        }
        return box;
    }
    
    private VBox createAttributeColumn(String title, List<String> attrs) {
        VBox box = new VBox(8);
        box.getStyleClass().add("attribute-column");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("subsection-title");
        box.getChildren().add(titleLabel);
        
        for (String attr : attrs) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            Label lbl = new Label(attr);
            lbl.setPrefWidth(90);
            DotSelector selector = new DotSelector(data.getAttribute(attr), 1);
            row.getChildren().addAll(lbl, selector);
            box.getChildren().add(row);
        }
        return box;
    }

    private SplitPane createCharmsContent() {
        SplitPane splitPane = new SplitPane();
        splitPane.getStyleClass().add("charms-split-pane");
        
        // --- Left Side: Tree Web Area ---
        VBox leftPane = new VBox(15);
        leftPane.setPadding(new Insets(20));
        
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER_LEFT);
        Label comboLabel = new Label("Ability:");
        comboLabel.getStyleClass().add("label");
        ComboBox<String> abilityCombo = new ComboBox<>();
        abilityCombo.getItems().addAll(CharacterData.ABILITIES);
        abilityCombo.setValue("Archery");
        controls.getChildren().addAll(comboLabel, abilityCombo);
        
        Label title = new Label("Charms Web");
        title.getStyleClass().add("section-title");
        
        charmCanvas = new Pane();
        ScrollPane charmScroll = new ScrollPane(charmCanvas);
        charmScroll.setPannable(true);
        charmScroll.getStyleClass().add("scroll-pane-custom");
        VBox.setVgrow(charmScroll, Priority.ALWAYS);
        
        leftPane.getChildren().addAll(controls, title, charmScroll);
        
        // --- Right Side: Sidebar Details ---
        VBox rightPane = new VBox(15);
        rightPane.setPadding(new Insets(20));
        rightPane.getStyleClass().add("charms-sidebar");
        
        Label detailTitle = new Label("No Charm Selected");
        detailTitle.getStyleClass().add("sidebar-title");
        detailTitle.setWrapText(true);
        
        Label detailReqs = new Label();
        detailReqs.getStyleClass().add("sidebar-reqs");
        detailReqs.setWrapText(true);
        
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(15);
        statsGrid.setVgap(10);
        
        Label costLabel = new Label();
        Label typeLabel = new Label();
        Label durationLabel = new Label();
        FlowPane kwFlow = new FlowPane(3, 3);
        
        costLabel.getStyleClass().add("sidebar-stat");
        typeLabel.getStyleClass().add("sidebar-stat");
        durationLabel.getStyleClass().add("sidebar-stat");
        kwFlow.setPrefWrapLength(200);
        
        statsGrid.add(new Label("Cost:"), 0, 0);
        statsGrid.add(costLabel, 1, 0);
        statsGrid.add(new Label("Type:"), 0, 1);
        statsGrid.add(typeLabel, 1, 1);
        statsGrid.add(new Label("Duration:"), 0, 2);
        statsGrid.add(durationLabel, 1, 2);
        statsGrid.add(new Label("Keywords:"), 0, 3);
        statsGrid.add(kwFlow, 1, 3);
        
        for(javafx.scene.Node n : statsGrid.getChildren()) {
            if (GridPane.getColumnIndex(n) == 0 && n instanceof Label) {
                n.getStyleClass().add("sidebar-stat-header");
            }
        }
        
        Label descriptionLabel = new Label();
        descriptionLabel.getStyleClass().add("sidebar-desc");
        descriptionLabel.setWrapText(true);
        ScrollPane descScroll = new ScrollPane(descriptionLabel);
        descScroll.setFitToWidth(true);
        descScroll.getStyleClass().add("scroll-pane-custom");
        VBox.setVgrow(descScroll, Priority.ALWAYS);
        
        Button toggleBtn = new Button("Purchase Charm");
        toggleBtn.getStyleClass().add("charm-btn");
        toggleBtn.setMaxWidth(Double.MAX_VALUE);
        toggleBtn.setVisible(false);
        
        rightPane.getChildren().addAll(detailTitle, detailReqs, toggleBtn, statsGrid, descScroll);
        
        Runnable loadCharms = () -> {
            charmCanvas.getChildren().clear();
            charmNodeMap.clear();
            currentAbilityCharms.clear();
            
            try {
                String abilityKey = abilityCombo.getValue().toLowerCase().replace(" ", "-");
                InputStream is = getClass().getResourceAsStream("/charms/" + abilityKey + ".json");
                if (is != null) {
                    try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        Type listType = new TypeToken<ArrayList<Charm>>(){}.getType();
                        List<Charm> loaded = new Gson().fromJson(reader, listType);
                        if (loaded != null) {
                            currentAbilityCharms.addAll(loaded);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            drawCharmWeb(currentAbilityCharms, title, abilityCombo.getValue(), detailTitle, detailReqs, toggleBtn, costLabel, typeLabel, durationLabel, kwFlow, descriptionLabel);
            title.setText(abilityCombo.getValue() + " Charms Web (" + currentAbilityCharms.size() + ")");
            
            detailTitle.setText("No Charm Selected");
            detailReqs.setText("");
            costLabel.setText("");
            typeLabel.setText("");
            durationLabel.setText("");
            kwFlow.getChildren().clear();
            descriptionLabel.setText("");
            toggleBtn.setVisible(false);
        };
        
        abilityCombo.valueProperty().addListener((obs, oldV, newV) -> loadCharms.run());
        loadCharms.run();
        
        splitPane.getItems().addAll(leftPane, rightPane);
        splitPane.setDividerPositions(0.7);
        return splitPane;
    }
    
    private void drawCharmWeb(List<Charm> charms, Label titleLabel, String ability, 
                              Label detailTitle, Label detailReqs, Button toggleBtn, 
                              Label costLabel, Label typeLabel, Label durLabel, 
                              FlowPane kwFlow, Label descLabel) {
        
        // Calculate Topological depth
        Map<String, Integer> charmDepth = new HashMap<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Charm c : charms) {
                int currentDepth = charmDepth.getOrDefault(c.getName(), 0);
                int reqDepth = 0;
                if (c.getPrerequisites() != null) {
                    for (String req : c.getPrerequisites()) {
                        reqDepth = Math.max(reqDepth, charmDepth.getOrDefault(req, 0) + 1);
                    }
                }
                if (reqDepth > currentDepth) {
                    charmDepth.put(c.getName(), reqDepth);
                    changed = true;
                }
            }
        }
        
        // Group by Depth Level
        Map<Integer, List<Charm>> levels = new HashMap<>();
        int maxDepth = 0;
        for (Charm c : charms) {
            int depth = charmDepth.getOrDefault(c.getName(), 0);
            levels.computeIfAbsent(depth, k -> new ArrayList<>()).add(c);
            if (depth > maxDepth) maxDepth = depth;
        }
        
        double boxWidth = 220;
        double boxHeight = 70;
        double gapX = 40;
        double gapY = 80;
        
        // Determine layout span
        double maxRowWidth = 0;
        for (int d = 0; d <= maxDepth; d++) {
            List<Charm> rowCharms = levels.getOrDefault(d, new ArrayList<>());
            double rowWidth = rowCharms.size() * boxWidth + Math.max(0, rowCharms.size() - 1) * gapX;
            if (rowWidth > maxRowWidth) maxRowWidth = rowWidth;
        }
        
        // Ensure scroll content respects constraints cleanly
        double virtualCanvasWidth = Math.max(800, maxRowWidth + 100); 
        
        // Draw Nodes
        for (int d = 0; d <= maxDepth; d++) {
            List<Charm> rowCharms = levels.getOrDefault(d, new ArrayList<>());
            double rowWidth = rowCharms.size() * boxWidth + Math.max(0, rowCharms.size() - 1) * gapX;
            
            // Center the entire row geometrically
            double startX = (virtualCanvasWidth - rowWidth) / 2;
            double y = 40 + d * (boxHeight + gapY);
            
            for (int i = 0; i < rowCharms.size(); i++) {
                Charm c = rowCharms.get(i);
                double x = startX + i * (boxWidth + gapX);
                
                VBox box = new VBox(5);
                box.setAlignment(Pos.CENTER);
                box.setPrefSize(boxWidth, boxHeight);
                box.getStyleClass().add("charm-node"); 
                
                Label nameLbl = new Label(c.getName());
                nameLbl.getStyleClass().add("charm-node-title");
                nameLbl.setWrapText(true);
                
                Label reqLbl = new Label(c.getAbility() + " " + c.getMinAbility() + ", Ess " + c.getMinEssence());
                reqLbl.getStyleClass().add("charm-node-reqs");
                
                box.getChildren().addAll(nameLbl, reqLbl);
                box.setLayoutX(x);
                box.setLayoutY(y);
                
                charmNodeMap.put(c.getName(), box);
                charmCanvas.getChildren().add(box);
                
                box.setOnMouseClicked(e -> {
                    detailTitle.setText(c.getName());
                    String prereqStr = c.getPrerequisites() == null || c.getPrerequisites().isEmpty() ? "None" : String.join(", ", c.getPrerequisites());
                    detailReqs.setText("Mins: " + c.getAbility() + " " + c.getMinAbility() + ", Ess: " + c.getMinEssence() + "\nPrereqs: " + prereqStr);
                    costLabel.setText(c.getCost() != null ? c.getCost() : "");
                    typeLabel.setText(c.getType() != null ? c.getType() : "");
                    durLabel.setText(c.getDuration() != null ? c.getDuration() : "");
                    updateKeywords(c.getKeywords(), kwFlow);
                    descLabel.setText(c.getFullText() != null ? c.getFullText() : "");
                    
                    toggleBtn.setVisible(true);
                    updateSidebarButton(c, toggleBtn);
                    
                    if (e.getClickCount() == 2) {
                        // Delay execution slightly to ensure correct visual state selection before firing
                        javafx.application.Platform.runLater(() -> toggleBtn.fire());
                    }
                });
            }
        }
        
        // Connect the specific button behavior directly
        toggleBtn.setOnAction(ev -> {
            String selectedName = detailTitle.getText();
            Charm c = charms.stream().filter(ch -> ch.getName().equals(selectedName)).findFirst().orElse(null);
            if (c != null) {
                if (data.hasCharm(c.getName())) {
                    data.removeCharm(c.getName());
                } else if (c.isEligible(data)) {
                    data.addCharm(new PurchasedCharm(c.getName(), c.getAbility()));
                }
                updateSidebarButton(c, toggleBtn);
                updateWebNodeStyles();
            }
        });
        
        // Draw Connecting Lines
        for (Charm c : charms) {
            VBox targetBox = charmNodeMap.get(c.getName());
            if (targetBox != null && c.getPrerequisites() != null) {
                for (String req : c.getPrerequisites()) {
                    VBox sourceBox = charmNodeMap.get(req);
                    if (sourceBox != null) {
                        double startX = sourceBox.getLayoutX() + boxWidth / 2;
                        double startY = sourceBox.getLayoutY() + boxHeight;
                        double endX = targetBox.getLayoutX() + boxWidth / 2;
                        double endY = targetBox.getLayoutY();
                        
                        CubicCurve curve = new CubicCurve(
                            startX, startY,
                            startX, startY + gapY / 1.5,
                            endX, endY - gapY / 1.5,
                            endX, endY
                        );
                        curve.getStyleClass().add("charm-line");
                        charmCanvas.getChildren().add(curve);
                        curve.toBack();
                    }
                }
            }
        }
        
        // Resize actual canvas bounds to match contents
        double minHeight = (maxDepth + 1) * (boxHeight + gapY) + 40;
        charmCanvas.setPrefSize(virtualCanvasWidth, minHeight);
        charmCanvas.setMinSize(virtualCanvasWidth, minHeight);
        
        // Apply immediate styling
        updateWebNodeStyles();
    }
    
    private void updateSidebarButton(Charm c, Button btn) {
        if (data.hasCharm(c.getName())) {
            btn.setText("Refund Charm");
            btn.setDisable(false);
            btn.setStyle("-fx-base: #a03030;"); 
        } else if (c.isEligible(data)) {
            btn.setText("Purchase Charm");
            btn.setDisable(false);
            btn.setStyle("-fx-base: #d4af37;"); 
        } else {
            btn.setText("Requirements Not Met");
            btn.setDisable(true);
            btn.setStyle("-fx-base: #444444;"); 
        }
    }

    private void loadKeywords() {
        try (InputStream is = getClass().getResourceAsStream("/charms/keywords.json")) {
            if (is != null) {
                try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    Type type = new TypeToken<List<Keyword>>() {}.getType();
                    List<Keyword> keywords = new Gson().fromJson(reader, type);
                    if (keywords != null) {
                        for (Keyword kw : keywords) {
                            keywordDefs.put(kw.getName(), kw.getDescription());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load keywords: " + e.getMessage());
        }
    }

    private void updateKeywords(String keywordsStr, FlowPane kwFlow) {
        kwFlow.getChildren().clear();
        if (keywordsStr == null || keywordsStr.isEmpty() || keywordsStr.equalsIgnoreCase("None")) {
            return;
        }
        
        String[] parts = keywordsStr.split(",");
        for (int i = 0; i < parts.length; i++) {
            String name = parts[i].trim();
            if (name.isEmpty()) continue;
            
            Label label = new Label(name);
            label.getStyleClass().add("sidebar-stat");
            label.getStyleClass().add("keyword-label");
            
            String def = keywordDefs.get(name);
            if (def != null) {
                Tooltip tooltip = new Tooltip(def);
                tooltip.setWrapText(true);
                tooltip.setMaxWidth(300);
                label.setTooltip(tooltip);
                label.getStyleClass().add("keyword-with-def");
            }
            
            kwFlow.getChildren().add(label);
            
            if (i < parts.length - 1) {
                Label comma = new Label(", ");
                comma.getStyleClass().add("sidebar-stat");
                kwFlow.getChildren().add(comma);
            }
        }
    }

    private void updateWebNodeStyles() {
        for (Charm c : currentAbilityCharms) {
            VBox box = charmNodeMap.get(c.getName());
            if (box != null) {
                box.getStyleClass().removeAll("charm-node-unselected", "charm-node-selected", "charm-node-ineligible");
                if (data.hasCharm(c.getName())) {
                    box.getStyleClass().add("charm-node-selected");
                } else if (c.isEligible(data)) {
                    box.getStyleClass().add("charm-node-unselected");
                } else {
                    box.getStyleClass().add("charm-node-ineligible");
                }
            }
        }
    }
}
