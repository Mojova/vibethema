package com.vibethema.ui;

import com.vibethema.service.CharmDataService;
import com.vibethema.service.EquipmentDataService;
import com.vibethema.model.CharacterData;
import com.vibethema.model.Charm;
import com.vibethema.model.SolarCharm;
import com.vibethema.model.Evocation;
import com.vibethema.model.Intimacy;
import com.vibethema.model.MartialArtsStyle;
import com.vibethema.model.PurchasedCharm;
import com.vibethema.model.Specialty;
import com.vibethema.model.Weapon;
import com.vibethema.model.ShapingRitual;
import com.vibethema.model.Spell;
import com.vibethema.ui.charms.CharmTreeComponent;
import com.vibethema.ui.equipment.EquipmentTab;
import com.vibethema.ui.sorcery.SorceryTab;
import com.vibethema.ui.util.UIUtils;
import com.google.gson.Gson;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.text.Text;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.scene.Cursor;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCombination;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.io.IOException;
import com.vibethema.model.CharacterSaveState;
import com.google.gson.GsonBuilder;
import com.vibethema.model.CraftAbility;
import com.vibethema.model.Keyword;
import com.vibethema.model.Merit;

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
    private Label meritsLabel = new Label();
    private Label specialtiesLabel = new Label();
    private Label bpLabel = new Label();

    private Map<String, String> keywordDefs = new HashMap<>();
    private CharmDataService dataService;
    private EquipmentDataService equipmentService = new EquipmentDataService();
    private List<CharmTreeComponent> charmTrees = new java.util.ArrayList<>();
    private Map<String, String> tagDescriptions = new java.util.HashMap<>();
    private TabPane mainTabPane;
    private Tab charmsTab;
    private Tab martialArtsTab;
    private ComboBox<String> charmsAbilityCombo;
    private ComboBox<String> stylePicker;
    private VBox principlesListContainer;
    private VBox tiesListContainer;
    
    
    private final CharmTreeComponent.CharmTreeListener charmTreeListener = new CharmTreeComponent.CharmTreeListener() {
        @Override
        public void onCreateCharm(String contextId, String contextName, String filterType, Runnable onSave) {
            showCreateCharmDialog(contextId, contextName, filterType, onSave);
        }

        @Override
        public void onEditCharm(Charm charm, String contextName, String filterType, Runnable onSave) {
            showEditCharmDialog(charm, contextName, filterType, onSave);
        }

        @Override
        public void onRefreshAllRequested() {
            refreshCharms();
        }
    };

    private void loadTagDescriptions() {
        Map<String, List<EquipmentDataService.Tag>> allTags = equipmentService.loadEquipmentTags();
        if (allTags != null) {
            for (List<EquipmentDataService.Tag> list : allTags.values()) {
                if (list != null) {
                    for (EquipmentDataService.Tag t : list) {
                        tagDescriptions.put(t.getName(), t.getDescription());
                    }
                }
            }
        }
    }

    public BuilderUI(CharacterData data) {
        this(data, null);
    }

    public BuilderUI(CharacterData data, File currentFile) {
        this.data = data;
        this.currentFile = currentFile;
        this.dataService = new CharmDataService();
        loadTagDescriptions();
        getStyleClass().add("main-pane");
        loadKeywords();


        setTop(createTopSection());
        mainTabPane = new TabPane();
        mainTabPane.getStyleClass().add("tab-pane");

        Tab statsTab = new Tab("Stats");
        statsTab.setClosable(false);
        ScrollPane statsScroll = new ScrollPane(createContent());
        statsScroll.setFitToWidth(true);
        statsScroll.getStyleClass().add("scroll-pane-custom");
        statsTab.setContent(statsScroll);

        Tab meritsTab = new Tab("Merits");
        meritsTab.setClosable(false);
        ScrollPane meritsScroll = new ScrollPane(createMeritsContent());
        meritsScroll.setFitToWidth(true);
        meritsScroll.getStyleClass().add("scroll-pane-custom");
        meritsTab.setContent(meritsScroll);

        Tab equipmentTab = new Tab("Equipment");
        equipmentTab.setClosable(false);
        ScrollPane equipmentScroll = new ScrollPane(createEquipmentContent());
        equipmentScroll.setFitToWidth(true);
        equipmentScroll.getStyleClass().add("scroll-pane-custom");
        equipmentTab.setContent(equipmentScroll);

        charmsTab = new Tab("Charms");
        charmsTab.setClosable(false);
        charmsTab.setContent(createCharmsContent());

        martialArtsTab = new Tab("Martial Arts");
        martialArtsTab.setClosable(false);
        martialArtsTab.setContent(createMartialArtsContent());

        Tab intimaciesTab = new Tab("Intimacies");
        intimaciesTab.setClosable(false);
        intimaciesTab.setContent(createIntimaciesContent());

        Tab sorceryTab = new Tab("Sorcery");
        sorceryTab.setClosable(false);
        sorceryTab.setContent(createSorceryContent());

        mainTabPane.getTabs().addAll(statsTab, meritsTab, equipmentTab, charmsTab, martialArtsTab, intimaciesTab, sorceryTab);

        setCenter(mainTabPane);

        setBottom(createFooter());

        setupListeners();
        updateFooter();

        data.dirtyProperty().addListener((obs, oldV, newV) -> updateWindowTitle());
        
        sceneProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                setupGlobalShortcuts(newV);
            }
        });
    }

    private void setupGlobalShortcuts(javafx.scene.Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            // General Ctrl+Tab cycling (all platforms)
            if (event.isControlDown() && event.getCode() == KeyCode.TAB) {
                cycleTab(event.isShiftDown() ? -1 : 1);
                event.consume();
            } 
            // Mac-style Cmd+Option+Arrows
            else if (event.isShortcutDown() && event.isAltDown()) {
                if (event.getCode() == KeyCode.RIGHT) {
                    cycleTab(1);
                    event.consume();
                } else if (event.getCode() == KeyCode.LEFT) {
                    cycleTab(-1);
                    event.consume();
                }
            }
            // Win/Linux style Shortcut+PgUp/PgDn
            else if (event.isShortcutDown()) {
                if (event.getCode() == KeyCode.PAGE_DOWN) {
                    cycleTab(1);
                    event.consume();
                } else if (event.getCode() == KeyCode.PAGE_UP) {
                    cycleTab(-1);
                    event.consume();
                }
            }
        });
    }

    private VBox createTopSection() {
        VBox topContainer = new VBox();
        MenuBar menuBar = new MenuBar();
        menuBar.setUseSystemMenuBar(true);

        Menu fileMenu = new Menu("File");

        MenuItem newItem = new MenuItem("New Character");
        newItem.setAccelerator(KeyCombination.keyCombination("Shortcut+N"));
        newItem.setOnAction(e -> {
            if (confirmDiscardChanges()) {
                data = new CharacterData();
                currentFile = null;
                getScene().setRoot(new BuilderUI(data));
            }
        });

        MenuItem saveItem = new MenuItem("Save");
        saveItem.setAccelerator(KeyCombination.keyCombination("Shortcut+S"));
        saveItem.setOnAction(e -> saveCharacter());

        MenuItem saveAsItem = new MenuItem("Save As...");
        saveAsItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Shift+S"));
        saveAsItem.setOnAction(e -> saveCharacterAs());

        MenuItem loadItem = new MenuItem("Load Character");
        loadItem.setAccelerator(KeyCombination.keyCombination("Shortcut+O"));
        loadItem.setOnAction(e -> loadCharacter());

        MenuItem importPdfItem = new MenuItem("Import Core PDF...");
        importPdfItem.setOnAction(e -> importPdf());

        MenuItem importMosePdfItem = new MenuItem("Import Miracles of the Solar Exalted...");
        importMosePdfItem.setOnAction(e -> importMosePdf());

        MenuItem quitItem = new MenuItem("Quit");
        quitItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Q"));
        quitItem.setOnAction(e -> {
            if (confirmDiscardChanges()) {
                Platform.exit();
            }
        });

        fileMenu.getItems().addAll(newItem, new SeparatorMenuItem(), saveItem, saveAsItem, loadItem, new SeparatorMenuItem(), importPdfItem, importMosePdfItem, new SeparatorMenuItem(), quitItem);
        
        Menu viewMenu = new Menu("View");
        
        MenuItem statsTabItem = new MenuItem("Stats");
        statsTabItem.setAccelerator(KeyCombination.keyCombination("Shortcut+1"));
        statsTabItem.setOnAction(e -> selectAndFocusTab(0));
        
        MenuItem meritsTabItem = new MenuItem("Merits");
        meritsTabItem.setAccelerator(KeyCombination.keyCombination("Shortcut+2"));
        meritsTabItem.setOnAction(e -> selectAndFocusTab(1));
        
        MenuItem equipmentTabItem = new MenuItem("Equipment");
        equipmentTabItem.setAccelerator(KeyCombination.keyCombination("Shortcut+3"));
        equipmentTabItem.setOnAction(e -> selectAndFocusTab(2));
        
        MenuItem charmsTabItem = new MenuItem("Charms");
        charmsTabItem.setAccelerator(KeyCombination.keyCombination("Shortcut+4"));
        charmsTabItem.setOnAction(e -> selectAndFocusTab(3));
        
        MenuItem martialArtsTabItem = new MenuItem("Martial Arts");
        martialArtsTabItem.setAccelerator(KeyCombination.keyCombination("Shortcut+5"));
        martialArtsTabItem.setOnAction(e -> selectAndFocusTab(4));

        MenuItem intimaciesTabItem = new MenuItem("Intimacies");
        intimaciesTabItem.setAccelerator(KeyCombination.keyCombination("Shortcut+6"));
        intimaciesTabItem.setOnAction(e -> selectAndFocusTab(5));

        MenuItem sorceryTabItem = new MenuItem("Sorcery");
        sorceryTabItem.setAccelerator(KeyCombination.keyCombination("Shortcut+7"));
        sorceryTabItem.setOnAction(e -> selectAndFocusTab(6));

        MenuItem nextTabItem = new MenuItem("Next Tab");
        nextTabItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Shift+]"));
        nextTabItem.setOnAction(e -> cycleTab(1));

        MenuItem prevTabItem = new MenuItem("Previous Tab");
        prevTabItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Shift+["));
        prevTabItem.setOnAction(e -> cycleTab(-1));

        viewMenu.getItems().addAll(
            statsTabItem, meritsTabItem, equipmentTabItem, charmsTabItem, martialArtsTabItem, intimaciesTabItem, sorceryTabItem,
            new SeparatorMenuItem(),
            nextTabItem, prevTabItem
        );

        menuBar.getMenus().addAll(fileMenu, viewMenu);

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
        if (!confirmDiscardChanges()) return;
        
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

    public boolean confirmDiscardChanges() {
        if (!data.isDirty()) return true;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("Your character has unsaved changes.");
        alert.setContentText("Would you like to save before continuing?");

        ButtonType saveBtn = new ButtonType("Save");
        ButtonType discardBtn = new ButtonType("Discard");
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(saveBtn, discardBtn, cancelBtn);

        java.util.Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == saveBtn) {
                saveCharacter();
                return !data.isDirty(); 
            } else if (result.get() == discardBtn) {
                return true;
            }
        }
        return false;
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
        casteBox.setValue(data.casteProperty().get());
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

        basicInfo.getChildren().addAll(new Label("Name:"), nameField, new Label("Caste:"), casteBox,
                new Label("Supernal:"), supernalDropdown);

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
                new Region() {
                    {
                        HBox.setHgrow(this, Priority.ALWAYS);
                    }
                },
                casteLabel, favoredLabel);

        row2.getChildren().addAll(
                new Label("Pools:"), abilitiesLabel, charmsLabel, meritsLabel, specialtiesLabel,
                new Region() {
                    {
                        HBox.setHgrow(this, Priority.ALWAYS);
                    }
                },
                bpLabel);

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
            if (oldV != newV && !data.isImporting()) {
                for (String abil : CharacterData.ABILITIES) {
                    data.getCasteAbility(abil).set(false);
                }
                data.supernalAbilityProperty().set("");
            }
            updateFooter();
        });

        data.supernalAbilityProperty().addListener((obs, oldV, newV) -> {
            updateFooter();
            updateAllWebNodeStyles();
        });

        data.getUnlockedCharms()
                .addListener((javafx.collections.ListChangeListener.Change<? extends PurchasedCharm> c) -> {
                    updateFooter();
                    updateAllWebNodeStyles();
                });

        data.getMerits().addListener((javafx.collections.ListChangeListener.Change<? extends Merit> c) -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (Merit m : c.getAddedSubList()) {
                        m.ratingProperty().addListener((obs, ov, nv) -> updateFooter());
                    }
                }
            }
            updateFooter();
        });

        data.getSpecialties().addListener((javafx.collections.ListChangeListener<? super Specialty>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (Specialty s : c.getAddedSubList()) {
                        s.nameProperty().addListener((obs, ov, nv) -> updateFooter());
                        s.abilityProperty().addListener((obs, ov, nv) -> updateFooter());
                    }
                }
            }
            updateFooter();
        });

        data.getCrafts().addListener((javafx.collections.ListChangeListener<? super CraftAbility>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (CraftAbility ca : c.getAddedSubList()) {
                        ca.expertiseProperty().addListener((obs, ov, nv) -> updateFooter());
                        ca.ratingProperty().addListener((obs, ov, nv) -> updateFooter());
                    }
                }
            }
            updateFooter();
        });

        data.getMartialArtsStyles().addListener((javafx.collections.ListChangeListener<? super MartialArtsStyle>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (MartialArtsStyle mas : c.getAddedSubList()) {
                        mas.ratingProperty().addListener((obs, ov, nv) -> updateFooter());
                    }
                }
            }
            updateFooter();
        });

        // Initial listeners
        for (Merit m : data.getMerits()) {
            m.ratingProperty().addListener((obs, ov, nv) -> updateFooter());
        }
        for (Specialty s : data.getSpecialties()) {
            s.nameProperty().addListener((obs, ov, nv) -> updateFooter());
            s.abilityProperty().addListener((obs, ov, nv) -> updateFooter());
        }
        for (CraftAbility ca : data.getCrafts()) {
            ca.expertiseProperty().addListener((obs, ov, nv) -> updateFooter());
            ca.ratingProperty().addListener((obs, ov, nv) -> updateFooter());
        }
        for (MartialArtsStyle mas : data.getMartialArtsStyles()) {
            mas.ratingProperty().addListener((obs, ov, nv) -> updateFooter());
        }
        
        data.getIntimacies().addListener((javafx.collections.ListChangeListener<? super Intimacy>) c -> {
            boolean structuralChange = false;
            while (c.next()) {
                if (c.wasAdded() || c.wasRemoved() || c.wasPermutated()) {
                    structuralChange = true;
                }
            }
            if (structuralChange) {
                refreshIntimaciesList();
            }
            updateFooter();
        });
        
        data.getShapingRituals().addListener((javafx.collections.ListChangeListener<? super ShapingRitual>) c -> updateFooter());
        data.getSpells().addListener((javafx.collections.ListChangeListener<? super Spell>) c -> updateFooter());
        data.getUnlockedCharms().addListener((javafx.collections.ListChangeListener<? super PurchasedCharm>) c -> updateFooter());
    }

    private void updateFooter() {
        int phys = data.getAttributeTotal(CharacterData.PHYSICAL_ATTRIBUTES);
        int soc = data.getAttributeTotal(CharacterData.SOCIAL_ATTRIBUTES);
        int ment = data.getAttributeTotal(CharacterData.MENTAL_ATTRIBUTES);
        int abils = data.getAbilityTotal();
        int bp = data.getBonusPointsSpent();

        long casteCount = CharacterData.ABILITIES.stream()
                .filter(a -> !"Martial Arts".equals(a) && data.getCasteAbility(a).get()).count();
        long favoredCount = CharacterData.ABILITIES.stream()
                .filter(a -> !"Martial Arts".equals(a) && data.getFavoredAbility(a).get()).count();
        int charmsCount = data.getTotalCharmPoolUsage();

        physicalLabel.setText("Phys: " + phys);
        socialLabel.setText("Soc: " + soc);
        mentalLabel.setText("Ment: " + ment);

        abilitiesLabel.setText("Abils: " + Math.min(28, abils) + "/28");
        charmsLabel.setText("Charms: " + charmsCount + "/15");
        meritsLabel.setText("Merits: " + data.getMeritTotal() + "/10");
        specialtiesLabel.setText("Specialties: " + data.getSpecialties().size() + "/4");
        casteLabel.setText("Caste: " + casteCount + "/5");
        favoredLabel.setText("Favored: " + favoredCount + "/5");
        bpLabel.setText("Bonus Points: " + bp + "/15");
        if (bp > 15) {
            bpLabel.setStyle("-fx-text-fill: #ff4d4d; -fx-font-weight: bold;");
        } else {
            bpLabel.setStyle("");
        }

        updateAllWebNodeStyles();
    }

    private void updateAllWebNodeStyles() {
        for (CharmTreeComponent tree : charmTrees) {
            tree.updateWebNodeStyles();
        }
    }

    private EquipmentTab createEquipmentContent() {
        return new EquipmentTab(data, equipmentService, dataService, tagDescriptions, this::updateFooter, this::showEvocationsDialog);
    }



    private VBox createContent() {
        VBox content = new VBox(20);
        content.getStyleClass().add("content-area");
        content.setPadding(new Insets(20));

        VBox basicAdvSection = createBasicAdvantagesSection();

        VBox attributesSection = new VBox(10);
        Label attrTitle = new Label("Attributes");
        attrTitle.getStyleClass().add("section-title");

        HBox attrColumns = new HBox(30);
        attrColumns.getChildren().addAll(
                createAttributeColumn("Physical", CharacterData.PHYSICAL_ATTRIBUTES),
                createAttributeColumn("Social", CharacterData.SOCIAL_ATTRIBUTES),
                createAttributeColumn("Mental", CharacterData.MENTAL_ATTRIBUTES));
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
                if ("Martial Arts".equals(abil)) continue;
                if (data.getCasteAbility(abil).get())
                    c++;
                if (data.getFavoredAbility(abil).get())
                    f++;
            }
            casteCount.set(c);
            favoredCount.set(f);
        };
        for (String abil : CharacterData.ABILITIES) {
            data.getCasteAbility(abil).addListener((obs, old, nv) -> updateCounts.run());
            data.getFavoredAbility(abil).addListener((obs, old, nv) -> updateCounts.run());
        }
        updateCounts.run();

        int rowCount = 0;
        int colCount = 0;
        for (String ability : CharacterData.ABILITIES) {
            if ("Craft".equals(ability) || "Martial Arts".equals(ability))
                continue;
            HBox rowBox = new HBox(6);
            rowBox.setAlignment(Pos.CENTER_LEFT);

            CheckBox casteBox = new CheckBox("C");
            casteBox.getStyleClass().add("caste-checkbox");
            casteBox.selectedProperty().bindBidirectional(data.getCasteAbility(ability));
            casteBox.disableProperty().bind(Bindings.createBooleanBinding(() -> {
                if ("Martial Arts".equals(ability))
                    return true; // Synced with Brawl
                CharacterData.Caste c = data.casteProperty().get();
                boolean notInCasteList = c == null || c == CharacterData.Caste.NONE
                        || !CharacterData.CASTE_OPTIONS.get(c).contains(ability);
                boolean atLimit = casteCount.get() >= 5 && !data.getCasteAbility(ability).get();
                return (notInCasteList || atLimit) && !data.getCasteAbility(ability).get();
            }, data.casteProperty(), casteCount, data.getCasteAbility(ability)));

            CheckBox favoredBox = new CheckBox("F");
            favoredBox.getStyleClass().add("favored-checkbox");
            favoredBox.selectedProperty().bindBidirectional(data.getFavoredAbility(ability));
            favoredBox.disableProperty().bind(Bindings.createBooleanBinding(() -> {
                if ("Martial Arts".equals(ability))
                    return true; // Synced with Brawl
                boolean isCaste = data.getCasteAbility(ability).get();
                boolean atLimit = favoredCount.get() >= 5 && !data.getFavoredAbility(ability).get();
                return (isCaste || atLimit) && !data.getFavoredAbility(ability).get();
            }, data.getCasteAbility(ability), favoredCount, data.getFavoredAbility(ability)));

            data.getCasteAbility(ability).addListener((obs, oldV, newV) -> {
                if (newV) {
                    favoredBox.setSelected(false);
                }
            });

            data.getFavoredAbility(ability).addListener((obs, oldV, newV) -> {
                if (newV && data.getAbility(ability).get() == 0) {
                    if ("Brawl".equals(ability)) {
                        boolean maHasDots = data.getMartialArtsStyles().stream().anyMatch(s -> s.getRating() > 0);
                        if (!maHasDots) data.getAbility(ability).set(1);
                    } else if ("Craft".equals(ability)) {
                        if (data.getCrafts().isEmpty()) {
                            CraftAbility ca = new CraftAbility("General", 1);
                            ca.setCaste(data.getCasteAbility("Craft").get());
                            ca.setFavored(true);
                            data.getCrafts().add(ca);
                        } else {
                            boolean craftHasDots = data.getCrafts().stream().anyMatch(c -> c.getRating() > 0);
                            if (!craftHasDots) data.getCrafts().get(0).ratingProperty().set(1);
                        }
                    } else {
                        data.getAbility(ability).set(1);
                    }
                }
            });

            Label abLabel = new Label(ability);
            abLabel.setPrefWidth(95);
            abLabel.setCursor(Cursor.HAND);
            abLabel.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) jumpToCharmAbility(ability);
            });
            if ("Martial Arts".equals(ability)) {
                Tooltip.install(abLabel, new Tooltip("Caste/Favored status linked to Brawl."));
            }

            data.casteProperty().addListener((obs, oldV, newV) -> {
                boolean isOption = newV != null && newV != CharacterData.Caste.NONE
                        && CharacterData.CASTE_OPTIONS.get(newV).contains(ability);
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
            data.getUnlockedCharms()
                    .addListener((javafx.collections.ListChangeListener.Change<? extends PurchasedCharm> change) -> {
                        updateExcellency.run();
                    });
            updateExcellency.run();

            DotSelector selector = new DotSelector(data.getAbility(ability), 0);
            if ("Brawl".equals(ability)) {
                // To trigger re-evaluation on MA rating changes, we need to bind to the item properties too.
                // A simpler way is to have a listener on MA styles that triggers a dummy property update or just re-binds.
                SimpleIntegerProperty maTotalDots = new SimpleIntegerProperty(0);
                Runnable updateMaTotal = () -> {
                    int total = data.getMartialArtsStyles().stream().mapToInt(MartialArtsStyle::getRating).sum();
                    maTotalDots.set(total);
                };
                data.getMartialArtsStyles().addListener((javafx.collections.ListChangeListener<? super MartialArtsStyle>) c -> {
                    updateMaTotal.run();
                    while (c.next()) {
                        if (c.wasAdded()) {
                            for (MartialArtsStyle mas : c.getAddedSubList()) {
                                mas.ratingProperty().addListener((obs, ov, nv) -> updateMaTotal.run());
                            }
                        }
                    }
                });
                for (MartialArtsStyle mas : data.getMartialArtsStyles()) {
                    mas.ratingProperty().addListener((obs, ov, nv) -> updateMaTotal.run());
                }
                updateMaTotal.run();

                selector.minDotsProperty().bind(Bindings.createIntegerBinding(() -> {
                    if (!data.getFavoredAbility("Brawl").get()) return 0;
                    return maTotalDots.get() > 0 ? 0 : 1;
                }, data.getFavoredAbility("Brawl"), maTotalDots));
            } else {
                selector.minDotsProperty().bind(Bindings.when(data.getFavoredAbility(ability)).then(1).otherwise(0));
            }
            rowBox.getChildren().addAll(casteBox, favoredBox, abLabel, selector);

            abilGrid.add(rowBox, colCount, rowCount);
            rowCount++;
            if (rowCount >= 13) {
                rowCount = 0;
                colCount++;
            }
        }
        abilitiesSection.getChildren().addAll(abilTitle, abilGrid);

        VBox craftsSection = createCraftsSection(casteCount, favoredCount);
        VBox specialtiesSection = createSpecialtiesSection();

        VBox sideStuff = new VBox(20);
        sideStuff.setPrefWidth(450);
        sideStuff.getChildren().addAll(craftsSection, specialtiesSection);

        HBox abilAndSide = new HBox(30);
        abilAndSide.getChildren().addAll(abilitiesSection, sideStuff);

        HBox statsRow = new HBox(40);
        statsRow.getChildren().addAll(createHealthSection(), createCombatStatsSection(), createSocialStatsSection(), createGreatCurseSection());

        content.getChildren().addAll(basicAdvSection, attributesSection, abilAndSide, new Separator(), statsRow, createAttackPoolsSection());
        return content;
    }

    private ScrollPane createMartialArtsContent() {
        VBox content = new VBox(20);
        content.getStyleClass().add("content-area");
        content.setPadding(new Insets(20));

        VBox section = new VBox(10);

        HBox titleRow = new HBox(15);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Martial Arts");
        title.getStyleClass().add("section-title");

        Label statusInfo = new Label("(Linked to Brawl)");
        statusInfo.getStyleClass().add("label-small");

        titleRow.getChildren().addAll(title, statusInfo);

        VBox styleList = new VBox(8);

        stylePicker = new ComboBox<>();
        stylePicker.setPromptText("Select Style to View Charms");
        stylePicker.setPrefWidth(220);

        Runnable refreshStylePicker = () -> {
            List<String> names = new java.util.ArrayList<>();
            for (MartialArtsStyle mas : data.getMartialArtsStyles()) {
                if (mas.getStyleName() != null && !mas.getStyleName().isEmpty() && !names.contains(mas.getStyleName())) {
                    names.add(mas.getStyleName());
                }
            }
            String currentSelection = stylePicker.getValue();
            stylePicker.getItems().setAll(names);
            if (names.contains(currentSelection)) {
                stylePicker.setValue(currentSelection);
            } else if (!names.isEmpty() && (currentSelection == null || currentSelection.isEmpty())) {
                stylePicker.setValue(names.get(0));
            }
        };

        Runnable refreshStyles = () -> {
            styleList.getChildren().clear();
            
            // To track total dots in all MA styles for minimum dots enforcement
            SimpleIntegerProperty maTotalDots = new SimpleIntegerProperty(0);
            Runnable updateMaTotal = () -> {
                int total = data.getMartialArtsStyles().stream().mapToInt(MartialArtsStyle::getRating).sum();
                maTotalDots.set(total);
            };
            for (MartialArtsStyle mas : data.getMartialArtsStyles()) {
                mas.ratingProperty().addListener((obs, ov, nv) -> updateMaTotal.run());
            }
            updateMaTotal.run();

            for (MartialArtsStyle mas : data.getMartialArtsStyles()) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("merit-row");
                row.setPadding(new Insets(8));

                Label nameLabel = new Label(mas.getStyleName());
                nameLabel.getStyleClass().add("label");
                nameLabel.setPrefWidth(180);
                nameLabel.setCursor(Cursor.HAND);
                nameLabel.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2) jumpToCharmAbility(mas.getStyleName());
                });

                DotSelector selector = new DotSelector(mas.ratingProperty(), 0, 5);
                
                // If Brawl is favored, at least one dot across Brawl + MA must exist.
                selector.minDotsProperty().bind(Bindings.createIntegerBinding(() -> {
                    if (!data.getFavoredAbility("Brawl").get()) return 0;
                    int brawlDots = data.getAbility("Brawl").get();
                    int otherMaDots = maTotalDots.get() - mas.getRating();
                    return (brawlDots == 0 && otherMaDots == 0) ? 1 : 0;
                }, data.getFavoredAbility("Brawl"), data.getAbility("Brawl"), maTotalDots));

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button removeBtn = new Button("✕");
                removeBtn.getStyleClass().add("remove-btn");
                removeBtn.setOnAction(e -> data.getMartialArtsStyles().remove(mas));

                row.getChildren().addAll(nameLabel, spacer, selector, removeBtn);
                styleList.getChildren().add(row);
            }
            refreshStylePicker.run();
        };

        data.getMartialArtsStyles().addListener(
                (javafx.collections.ListChangeListener<? super MartialArtsStyle>) c -> {
                    boolean structuralChange = false;
                    while (c.next()) {
                        if (c.wasAdded() || c.wasRemoved() || c.wasPermutated()) {
                            structuralChange = true;
                        }
                        if (c.wasAdded()) {
                            for (MartialArtsStyle mas : c.getAddedSubList()) {
                                mas.styleNameProperty().addListener((obs, ov, nv) -> refreshStylePicker.run());
                            }
                        }
                    }
                    if (structuralChange) {
                        refreshStyles.run();
                    }
                });
        for (MartialArtsStyle mas : data.getMartialArtsStyles()) {
            mas.styleNameProperty().addListener((obs, ov, nv) -> refreshStylePicker.run());
        }
        refreshStyles.run();

        Button addBtn = new Button("+ Add Martial Arts Style");
        addBtn.getStyleClass().add("action-btn");
        addBtn.setOnAction(e -> {
            List<String> available = dataService.getAvailableMartialArtsStyles();
            // Filter out already added styles
            List<String> current = data.getMartialArtsStyles().stream().map(MartialArtsStyle::getStyleName).toList();
            available.removeIf(current::contains);

            if (available.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "No new styles found. Create a new one first!", ButtonType.OK);
                alert.showAndWait();
                return;
            }

            ChoiceDialog<String> dialog = new ChoiceDialog<>(available.get(0), available);
            dialog.setTitle("Add Martial Arts Style");
            dialog.setHeaderText("Select a style to add to your character:");
            dialog.setContentText("Style:");
            dialog.showAndWait().ifPresent(selection -> {
                MartialArtsStyle mas = new MartialArtsStyle(null, selection, 1);
                data.getMartialArtsStyles().add(mas);
            });
        });

        Button createBtn = new Button("Create New Style");
        createBtn.getStyleClass().add("action-btn");
        createBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Create Martial Arts Style");
            dialog.setHeaderText("Create a new style definition:");
            dialog.setContentText("Style Name (e.g. Tiger Style):");
            dialog.showAndWait().ifPresent(name -> {
                if (!name.trim().isEmpty()) {
                    try {
                        dataService.createNewMartialArtsStyle(name.trim());
                        // Automatically add it if they created it
                        data.getMartialArtsStyles().add(new MartialArtsStyle(null, name.trim(), 0));
                    } catch (java.io.IOException ex) {
                        new Alert(Alert.AlertType.ERROR, "Failed to create style: " + ex.getMessage(), ButtonType.OK).showAndWait();
                    }
                }
            });
        });

        HBox maButtons = new HBox(10, addBtn, createBtn);
        section.getChildren().addAll(titleRow, styleList, maButtons);
        content.getChildren().add(section);

        Separator sep = new Separator();
        sep.setPadding(new Insets(20, 0, 20, 0));
        content.getChildren().add(sep);

        Label treeTitle = new Label("Style Charm Tree");
        treeTitle.getStyleClass().add("section-title");
        
        HBox treeControls = new HBox(15);
        treeControls.setAlignment(Pos.CENTER_LEFT);
        treeControls.getChildren().addAll(new Label("Select Style:"), stylePicker);
        
        content.getChildren().addAll(treeTitle, treeControls);

        CharmTreeComponent maCharmTree = new CharmTreeComponent(data, dataService, keywordDefs, charmTreeListener, stylePicker, "Martial Arts Style");
        charmTrees.add(maCharmTree);
        
        VBox fullLayout = new VBox(0);
        fullLayout.getChildren().addAll(content, maCharmTree, createPurchasedCharmsSummary(true));
        VBox.setVgrow(maCharmTree, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane(fullLayout);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane-custom");
        return scrollPane;
    }

    private ScrollPane createIntimaciesContent() {
        VBox content = new VBox(25);
        content.getStyleClass().add("content-area");
        content.setPadding(new Insets(20));

        VBox principlesSection = UIUtils.createSection("Principles");
        principlesListContainer = new VBox(10);
        principlesListContainer.getStyleClass().add("merit-row-container");

        Button addPrincipleBtn = new Button("+ Add Principle");
        addPrincipleBtn.getStyleClass().add("add-btn");
        addPrincipleBtn.setOnAction(e -> {
            Intimacy i = new Intimacy(java.util.UUID.randomUUID().toString(), "New Principle", Intimacy.Type.PRINCIPLE, Intimacy.Intensity.MINOR);
            data.getIntimacies().add(i);
        });
        principlesSection.getChildren().addAll(principlesListContainer, addPrincipleBtn);

        VBox tiesSection = UIUtils.createSection("Ties");
        tiesListContainer = new VBox(10);
        tiesListContainer.getStyleClass().add("merit-row-container");

        Button addTieBtn = new Button("+ Add Tie");
        addTieBtn.getStyleClass().add("add-btn");
        addTieBtn.setOnAction(e -> {
            Intimacy i = new Intimacy(java.util.UUID.randomUUID().toString(), "New Tie", Intimacy.Type.TIE, Intimacy.Intensity.MINOR);
            data.getIntimacies().add(i);
        });
        tiesSection.getChildren().addAll(tiesListContainer, addTieBtn);

        content.getChildren().addAll(principlesSection, tiesSection);
        refreshIntimaciesList();

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane-custom");
        return scrollPane;
    }

    private void refreshIntimaciesList() {
        if (principlesListContainer == null || tiesListContainer == null) return;
        principlesListContainer.getChildren().clear();
        tiesListContainer.getChildren().clear();

        for (Intimacy i : data.getIntimacies()) {
            HBox row = new HBox(15);
            row.getStyleClass().add("merit-row");
            row.setAlignment(Pos.CENTER_LEFT);

            TextField nameField = new TextField(i.getName());
            nameField.setPromptText(i.getType() == Intimacy.Type.PRINCIPLE ? "Belief statement..." : "Relationship...");
            HBox.setHgrow(nameField, Priority.ALWAYS);
            nameField.textProperty().addListener((obs, ov, nv) -> i.setName(nv));

            ComboBox<Intimacy.Intensity> intensityBox = new ComboBox<>();
            intensityBox.getItems().addAll(Intimacy.Intensity.values());
            intensityBox.setValue(i.getIntensity());
            intensityBox.valueProperty().addListener((obs, ov, nv) -> i.setIntensity(nv));

            Button delBtn = new Button("🗑");
            delBtn.getStyleClass().add("remove-btn");
            delBtn.setOnAction(e -> data.getIntimacies().remove(i));

            row.getChildren().addAll(nameField, intensityBox, delBtn);

            if (i.getType() == Intimacy.Type.PRINCIPLE) {
                principlesListContainer.getChildren().add(row);
            } else {
                tiesListContainer.getChildren().add(row);
            }
        }
    }

    private javafx.scene.Node createSorceryContent() {
        return new SorceryTab(data, dataService, this::updateFooter);
    }

    private VBox createCraftsSection(javafx.beans.property.IntegerProperty casteCount,
            javafx.beans.property.IntegerProperty favoredCount) {
        VBox section = new VBox(10);

        HBox titleRow = new HBox(15);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Crafts");
        title.getStyleClass().add("section-title");

        // Add C/F checkboxes for the global Craft status
        HBox statusBox = new HBox(6);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        CheckBox casteBox = new CheckBox("C");
        casteBox.getStyleClass().add("caste-checkbox");
        casteBox.selectedProperty().bindBidirectional(data.getCasteAbility("Craft"));
        casteBox.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            CharacterData.Caste c = data.casteProperty().get();
            boolean notInCasteList = c == null || c == CharacterData.Caste.NONE
                    || !CharacterData.CASTE_OPTIONS.get(c).contains("Craft");
            boolean atLimit = casteCount.get() >= 5 && !data.getCasteAbility("Craft").get();
            return (notInCasteList || atLimit) && !data.getCasteAbility("Craft").get();
        }, data.casteProperty(), casteCount, data.getCasteAbility("Craft")));

        CheckBox favoredBox = new CheckBox("F");
        favoredBox.getStyleClass().add("favored-checkbox");
        favoredBox.selectedProperty().bindBidirectional(data.getFavoredAbility("Craft"));
        favoredBox.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            boolean isCaste = data.getCasteAbility("Craft").get();
            boolean atLimit = favoredCount.get() >= 5 && !data.getFavoredAbility("Craft").get();
            return (isCaste || atLimit) && !data.getFavoredAbility("Craft").get();
        }, data.getCasteAbility("Craft"), favoredCount, data.getFavoredAbility("Craft")));

        data.getCasteAbility("Craft").addListener((obs, oldV, newV) -> {
            if (newV)
                favoredBox.setSelected(false);
        });

        statusBox.getChildren().addAll(casteBox, favoredBox);
        titleRow.getChildren().addAll(title, statusBox);

        VBox craftList = new VBox(8);

        Runnable refreshCrafts = () -> {
            craftList.getChildren().clear();
            
            SimpleIntegerProperty craftTotalDots = new SimpleIntegerProperty(0);
            Runnable updateCraftTotal = () -> {
                int total = data.getCrafts().stream().mapToInt(CraftAbility::getRating).sum();
                craftTotalDots.set(total);
            };
            for (CraftAbility ca : data.getCrafts()) {
                ca.ratingProperty().addListener((obs, ov, nv) -> updateCraftTotal.run());
            }
            updateCraftTotal.run();

            for (CraftAbility ca : data.getCrafts()) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("merit-row");
                row.setPadding(new Insets(8));

                TextField expertiseField = new TextField();
                expertiseField.setPromptText("Expertise (e.g. Metallurgy)");
                expertiseField.textProperty().bindBidirectional(ca.expertiseProperty());
                expertiseField.setPrefWidth(180);

                Button jumpBtn = new Button("🔍");
                jumpBtn.getStyleClass().add("action-btn-small");
                jumpBtn.setTooltip(new Tooltip("Jump to Craft Charms"));
                jumpBtn.setOnAction(e -> jumpToCharmAbility("Craft"));

                DotSelector selector = new DotSelector(ca.ratingProperty(), 0, 5);
                selector.minDotsProperty().bind(Bindings.createIntegerBinding(() -> {
                    if (!data.getFavoredAbility("Craft").get()) return 0;
                    int otherDots = craftTotalDots.get() - ca.getRating();
                    return otherDots > 0 ? 0 : 1;
                }, data.getFavoredAbility("Craft"), craftTotalDots));

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button removeBtn = new Button("✕");
                removeBtn.getStyleClass().add("remove-btn");
                removeBtn.setOnAction(e -> data.getCrafts().remove(ca));

                row.getChildren().addAll(expertiseField, jumpBtn, spacer, selector, removeBtn);
                craftList.getChildren().add(row);
            }
        };

        data.getCrafts().addListener((javafx.collections.ListChangeListener<? super CraftAbility>) c -> {
            boolean needsRefresh = false;
            while (c.next()) {
                if (c.wasAdded() || c.wasRemoved() || c.wasPermutated()) {
                    needsRefresh = true;
                    break;
                }
            }
            if (needsRefresh) refreshCrafts.run();
        });
        refreshCrafts.run();

        Button addBtn = new Button("+ Add Craft Ability");
        addBtn.getStyleClass().add("action-btn");
        addBtn.setOnAction(e -> data.getCrafts().add(new CraftAbility("", 0)));

        section.getChildren().addAll(titleRow, craftList, addBtn);
        return section;
    }

    private VBox createSpecialtiesSection() {
        VBox section = new VBox(10);
        Label title = new Label("Specialties");
        title.getStyleClass().add("section-title");

        VBox specList = new VBox(8);

        Runnable refreshSpecs = () -> {
            specList.getChildren().clear();
            for (Specialty s : data.getSpecialties()) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("merit-row"); // reuse style
                row.setPadding(new Insets(8));

                TextField nameField = new TextField();
                nameField.setPromptText("Specialty Name");
                nameField.textProperty().bindBidirectional(s.nameProperty());
                nameField.setPrefWidth(200);

                ComboBox<String> abilPicker = new ComboBox<>();
                abilPicker.getItems().addAll(CharacterData.ABILITIES);
                abilPicker.valueProperty().bindBidirectional(s.abilityProperty());
                abilPicker.setPromptText("Select Ability");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button removeBtn = new Button("✕");
                removeBtn.getStyleClass().add("remove-btn");
                removeBtn.setOnAction(e -> data.getSpecialties().remove(s));

                row.getChildren().addAll(nameField, abilPicker, spacer, removeBtn);
                specList.getChildren().add(row);
            }
        };

        data.getSpecialties().addListener((javafx.collections.ListChangeListener<? super Specialty>) c -> {
            boolean needsRefresh = false;
            while (c.next()) {
                if (c.wasAdded() || c.wasRemoved() || c.wasPermutated()) {
                    needsRefresh = true;
                    break;
                }
            }
            if (needsRefresh) refreshSpecs.run();
        });
        refreshSpecs.run();

        Button addBtn = new Button("+ Add Specialty");
        addBtn.getStyleClass().add("action-btn");
        addBtn.setOnAction(e -> data.getSpecialties().add(new Specialty("", "")));

        section.getChildren().addAll(title, specList, addBtn);
        return section;
    }

    private VBox createBasicAdvantagesSection() {
        VBox section = new VBox(15);
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
        section.getChildren().addAll(advTitle, topRow);
        return section;
    }

    private VBox createHealthSection() {
        VBox healthBox = new VBox(5);
        Label healthLabel = new Label("Health Levels");
        healthLabel.getStyleClass().add("subsection-title");

        VBox trackBoxes = new VBox(8);
        trackBoxes.setAlignment(Pos.CENTER_LEFT);

        Runnable updateHealth = () -> {
            trackBoxes.getChildren().clear();
            List<String> levels = data.getHealthLevels();
            Map<String, Integer> counts = new java.util.LinkedHashMap<>();
            // Ensure order -0, -1, -2, -4, Incap
            for (String lv : new String[] { "-0", "-1", "-2", "-4", "Incap" })
                counts.put(lv, 0);
            for (String lv : levels)
                counts.put(lv, counts.getOrDefault(lv, 0) + 1);

            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                if (entry.getValue() > 0) {
                    trackBoxes.getChildren().add(createHealthLevel(entry.getKey(), entry.getValue()));
                }
            }
        };

        data.getAttribute("Stamina").addListener((obs, old, nv) -> updateHealth.run());
        data.getAbility("Resistance").addListener((obs, old, nv) -> updateHealth.run());
        data.getUnlockedCharms().addListener(
                (javafx.collections.ListChangeListener.Change<? extends PurchasedCharm> c) -> updateHealth.run());
        updateHealth.run();

        healthBox.getChildren().addAll(healthLabel, trackBoxes);
        return healthBox;
    }

    private VBox createCombatStatsSection() {
        VBox combatBox = new VBox(5);
        Label combatLabel = new Label("Combat Statistics");
        combatLabel.getStyleClass().add("subsection-title");
        
        VBox statsList = new VBox(5);
        statsList.getStyleClass().add("merit-row-container");
        statsList.setPadding(new Insets(5, 10, 5, 10));
        
        Label naturalSoakVal = new Label();
        naturalSoakVal.textProperty().bind(Bindings.concat("Natural Soak: ", data.naturalSoakProperty().asString()));
        naturalSoakVal.getStyleClass().add("merit-name");
        
        Label armorSoakVal = new Label();
        armorSoakVal.textProperty().bind(Bindings.concat("Armor Soak: +", data.armorSoakProperty().asString()));
        armorSoakVal.getStyleClass().add("merit-name");
        
        Label totalSoakVal = new Label();
        totalSoakVal.textProperty().bind(Bindings.concat("Total Soak: ", data.totalSoakProperty().asString()));
        totalSoakVal.setStyle("-fx-font-weight: bold; -fx-text-fill: #3498db; -fx-font-size: 1.1em;");
        
        Label hardnessVal = new Label();
        hardnessVal.textProperty().bind(Bindings.concat("Hardness: ", data.totalHardnessProperty().asString()));
        hardnessVal.getStyleClass().add("merit-name");

        HBox dodgeBox = new HBox(0);
        dodgeBox.setAlignment(Pos.CENTER_LEFT);
        Label dodgeBaseLabel = new Label();
        dodgeBaseLabel.getStyleClass().add("merit-name");
        Label dodgeBonusLabel = new Label();
        dodgeBonusLabel.getStyleClass().add("merit-name");
        dodgeBonusLabel.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
        Tooltip dodgeTooltip = new Tooltip("If dodge specialty applies");
        
        dodgeBox.getChildren().addAll(dodgeBaseLabel, dodgeBonusLabel);

        Runnable updateEvasion = () -> {
            int dex = data.getAttribute("Dexterity").get();
            int dodge = data.getAbility("Dodge").get();
            int base = (int) Math.ceil((dex + dodge) / 2.0);
            int total = (int) Math.ceil((dex + dodge + 1) / 2.0);
            int bonus = total - base;
            
            boolean hasDodgeSpec = data.getSpecialties().stream()
                    .anyMatch(s -> "Dodge".equals(s.getAbility()) && s.getName() != null && !s.getName().trim().isEmpty());
            
            dodgeBaseLabel.setText("Evasion: " + base);
            if (hasDodgeSpec) {
                dodgeBonusLabel.setText(" + " + bonus);
                Tooltip.install(dodgeBonusLabel, dodgeTooltip);
            } else {
                dodgeBonusLabel.setText("");
                Tooltip.uninstall(dodgeBonusLabel, dodgeTooltip);
            }
        };
        data.getAttribute("Dexterity").addListener((obs, oldVal, newVal) -> updateEvasion.run());
        data.getAbility("Dodge").addListener((obs, oldVal, newVal) -> updateEvasion.run());
        data.getSpecialties().addListener((javafx.collections.ListChangeListener<? super Specialty>) c -> updateEvasion.run());
        updateEvasion.run();

        HBox joinBattleBox = new HBox(0);
        joinBattleBox.setAlignment(Pos.CENTER_LEFT);
        Label joinBattleLabel = new Label();
        joinBattleLabel.getStyleClass().add("merit-name");
        Label joinBattleBonusLabel = new Label();
        joinBattleBonusLabel.getStyleClass().add("merit-name");
        joinBattleBonusLabel.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
        Tooltip awarenessTooltip = new Tooltip("If awareness specialty applies");

        joinBattleBox.getChildren().addAll(joinBattleLabel, joinBattleBonusLabel);

        Runnable updateJoinBattle = () -> {
            int wits = data.getAttribute("Wits").get();
            int awareness = data.getAbility("Awareness").get();
            int base = wits + awareness;
            
            boolean hasAwarenessSpec = data.getSpecialties().stream()
                    .anyMatch(s -> "Awareness".equals(s.getAbility()) && s.getName() != null && !s.getName().trim().isEmpty());
            
            joinBattleLabel.setText("Join Battle: " + base);
            if (hasAwarenessSpec) {
                joinBattleBonusLabel.setText(" + 1");
                Tooltip.install(joinBattleBonusLabel, awarenessTooltip);
            } else {
                joinBattleBonusLabel.setText("");
                Tooltip.uninstall(joinBattleBonusLabel, awarenessTooltip);
            }
        };
        data.getAttribute("Wits").addListener((obs, oldVal, newVal) -> updateJoinBattle.run());
        data.getAbility("Awareness").addListener((obs, oldVal, newVal) -> updateJoinBattle.run());
        data.getSpecialties().addListener((javafx.collections.ListChangeListener<? super Specialty>) c -> updateJoinBattle.run());
        updateJoinBattle.run();
        
        statsList.getChildren().addAll(naturalSoakVal, armorSoakVal, totalSoakVal, hardnessVal, dodgeBox, joinBattleBox);
        combatBox.getChildren().addAll(combatLabel, statsList);
        return combatBox;
    }

    private VBox createGreatCurseSection() {
        VBox section = new VBox(10);
        Label title = new Label("Great Curse");
        title.getStyleClass().add("section-title");

        VBox content = new VBox(8);
        content.getStyleClass().add("social-stats-container"); // Reuse style
        content.setPadding(new Insets(10));

        Label triggerLabel = new Label("Limit Trigger:");
        triggerLabel.getStyleClass().add("sidebar-stat-header");
        
        TextArea triggerArea = new TextArea();
        triggerArea.setPromptText("Enter your Limit Trigger...");
        triggerArea.setPrefRowCount(2);
        triggerArea.setWrapText(true);
        triggerArea.textProperty().bindBidirectional(data.limitTriggerProperty());
        triggerArea.getStyleClass().add("text-area-custom");

        HBox limitBox = new HBox(10);
        limitBox.setAlignment(Pos.CENTER_LEFT);
        Label lLabel = new Label("Limit:");
        lLabel.getStyleClass().add("sidebar-stat-header");
        DotSelector limitSelector = new DotSelector(data.limitProperty(), 0, 10);
        limitBox.getChildren().addAll(lLabel, limitSelector);

        content.getChildren().addAll(triggerLabel, triggerArea, limitBox);
        section.getChildren().addAll(title, content);
        return section;
    }

    private VBox createSocialStatsSection() {
        VBox section = new VBox(5);
        Label title = new Label("Social Statistics");
        title.getStyleClass().add("subsection-title");
        
        VBox statsList = new VBox(5);
        statsList.getStyleClass().add("merit-row-container");
        statsList.setPadding(new Insets(5, 10, 5, 10));
        
        // Resolve
        HBox resolveBox = new HBox(0);
        resolveBox.setAlignment(Pos.CENTER_LEFT);
        Label resolveBaseLabel = new Label();
        resolveBaseLabel.getStyleClass().add("merit-name");
        Label resolveBonusLabel = new Label();
        resolveBonusLabel.getStyleClass().add("merit-name");
        resolveBonusLabel.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
        Tooltip resolveTooltip = new Tooltip("If integrity specialty applies");
        resolveBox.getChildren().addAll(resolveBaseLabel, resolveBonusLabel);
        
        // No more double-click

        Runnable updateResolve = () -> {
            int wits = data.getAttribute("Wits").get();
            int integrity = data.getAbility("Integrity").get();
            int base = (int) Math.ceil((wits + integrity) / 2.0);
            int total = (int) Math.ceil((wits + integrity + 1) / 2.0);
            int bonus = total - base;
            
            boolean hasSpec = data.getSpecialties().stream()
                    .anyMatch(s -> "Integrity".equals(s.getAbility()) && s.getName() != null && !s.getName().trim().isEmpty());
            
            resolveBaseLabel.setText("Resolve: " + base);
            if (hasSpec) {
                resolveBonusLabel.setText(" + " + bonus);
                Tooltip.install(resolveBonusLabel, resolveTooltip);
            } else {
                resolveBonusLabel.setText("");
                Tooltip.uninstall(resolveBonusLabel, resolveTooltip);
            }
        };

        // Guile
        HBox guileBox = new HBox(0);
        guileBox.setAlignment(Pos.CENTER_LEFT);
        Label guileBaseLabel = new Label();
        guileBaseLabel.getStyleClass().add("merit-name");
        Label guileBonusLabel = new Label();
        guileBonusLabel.getStyleClass().add("merit-name");
        guileBonusLabel.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
        Tooltip guileTooltip = new Tooltip("If socialize specialty applies");
        guileBox.getChildren().addAll(guileBaseLabel, guileBonusLabel);
        
        // No more double-click

        Runnable updateGuile = () -> {
            int manipulation = data.getAttribute("Manipulation").get();
            int socialize = data.getAbility("Socialize").get();
            int base = (int) Math.ceil((manipulation + socialize) / 2.0);
            int total = (int) Math.ceil((manipulation + socialize + 1) / 2.0);
            int bonus = total - base;
            
            boolean hasSpec = data.getSpecialties().stream()
                    .anyMatch(s -> "Socialize".equals(s.getAbility()) && s.getName() != null && !s.getName().trim().isEmpty());
            
            guileBaseLabel.setText("Guile: " + base);
            if (hasSpec) {
                guileBonusLabel.setText(" + " + bonus);
                Tooltip.install(guileBonusLabel, guileTooltip);
            } else {
                guileBonusLabel.setText("");
                Tooltip.uninstall(guileBonusLabel, guileTooltip);
            }
        };

        data.getAttribute("Wits").addListener((obs, old, nv) -> updateResolve.run());
        data.getAbility("Integrity").addListener((obs, old, nv) -> updateResolve.run());
        data.getAttribute("Manipulation").addListener((obs, old, nv) -> updateGuile.run());
        data.getAbility("Socialize").addListener((obs, old, nv) -> updateGuile.run());
        data.getSpecialties().addListener((javafx.collections.ListChangeListener<? super Specialty>) c -> {
            updateResolve.run();
            updateGuile.run();
        });

        updateResolve.run();
        updateGuile.run();
        
        statsList.getChildren().addAll(resolveBox, guileBox);
        section.getChildren().addAll(title, statsList);
        return section;
    }

    private VBox createAttackPoolsSection() {
        VBox section = new VBox(5);
        Label title = new Label("Attack Pools");
        title.getStyleClass().add("subsection-title");
        
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(8);
        grid.getStyleClass().add("merit-row-container");
        grid.setPadding(new Insets(10));
        
        String[] headers = {"Weapon", "Withering", "Decisive", "Damage", "Parry"};
        for (int i = 0; i < headers.length; i++) {
            Label hl = new Label(headers[i]);
            hl.getStyleClass().add("sidebar-stat-header");
            grid.add(hl, i, 0);
        }

        Runnable refresh = () -> {
            grid.getChildren().removeIf(node -> GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) > 0);
            int row = 1;
            int dex = data.getAttribute("Dexterity").get();
            int str = data.getAttribute("Strength").get();
            
            for (Weapon w : data.getWeapons()) {
                String abilityName = "Melee";
                if (w.getRange() == Weapon.WeaponRange.ARCHERY) abilityName = "Archery";
                else if (w.getRange() == Weapon.WeaponRange.THROWN) abilityName = "Thrown";
                else if (w.getTags().contains("Brawl")) abilityName = "Brawl";
                
                int abil = data.getAbilityRating(abilityName);
                int spec = (w.getSpecialtyId() != null && !w.getSpecialtyId().isEmpty()) ? 1 : 0;
                
                String witheringStr;
                if (w.getRange() == Weapon.WeaponRange.CLOSE) {
                    int withering = dex + abil + spec + w.getAccuracy();
                    witheringStr = String.valueOf(withering);
                } else {
                    int base = dex + abil + spec;
                    witheringStr = String.format("C:%d | S:%d | M:%d | L:%d | E:%d",
                            base + w.getCloseRangeBonus(),
                            base + w.getShortRangeBonus(),
                            base + w.getMediumRangeBonus(),
                            base + w.getLongRangeBonus(),
                            base + w.getExtremeRangeBonus());
                }
                
                int decisive = dex + abil + spec;
                int damage = str + w.getDamage();
                int parryPool = dex + abil + spec;
                int parry = (int) Math.ceil(parryPool / 2.0) + w.getDefense();
                
                grid.add(new Label(w.getName()), 0, row);
                grid.add(new Label(witheringStr), 1, row);
                grid.add(new Label(String.valueOf(decisive)), 2, row);
                grid.add(new Label(String.valueOf(damage)), 3, row);
                grid.add(new Label(String.valueOf(parry)), 4, row);
                
                row++;
            }
            
            for (javafx.scene.Node n : grid.getChildren()) {
                if (n instanceof Label l && GridPane.getRowIndex(n) != null && GridPane.getRowIndex(n) > 0) {
                    l.getStyleClass().add("merit-name");
                }
            }
        };

        // Listeners for all dependencies
        data.getAttribute("Dexterity").addListener((obs, old, nv) -> refresh.run());
        data.getAttribute("Strength").addListener((obs, old, nv) -> refresh.run());
        data.getAbility("Melee").addListener((obs, old, nv) -> refresh.run());
        data.getAbility("Archery").addListener((obs, old, nv) -> refresh.run());
        data.getAbility("Brawl").addListener((obs, old, nv) -> refresh.run());
        data.getWeapons().addListener((javafx.collections.ListChangeListener<? super Weapon>) c -> refresh.run());
        data.getSpecialties().addListener((javafx.collections.ListChangeListener<? super Specialty>) c -> refresh.run());
        data.getMartialArtsStyles().addListener((javafx.collections.ListChangeListener<? super MartialArtsStyle>) c -> refresh.run());
        
        refresh.run();
        
        section.getChildren().addAll(title, grid);
        return section;
    }

    private HBox createHealthLevel(String label, int count) {
        HBox box = new HBox(5);
        box.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label);
        l.getStyleClass().add("label");
        l.setPrefWidth(40);
        box.getChildren().add(l);
        for (int i = 0; i < count; i++) {
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
                    case "":
                        mark.setText("/");
                        break;
                    case "/":
                        mark.setText("X");
                        break;
                    case "X":
                        mark.setText("*");
                        break;
                    case "*":
                        mark.setText("");
                        break;
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

    private VBox createMeritsContent() {
        VBox content = new VBox(20);
        content.getStyleClass().add("content-area");
        content.setPadding(new Insets(20));

        Label title = new Label("Merits");
        title.getStyleClass().add("section-title");

        VBox meritsList = new VBox(12);
        meritsList.getStyleClass().add("merits-list");

        Runnable refreshMerits = () -> {
            meritsList.getChildren().clear();
            for (Merit merit : data.getMerits()) {
                HBox row = new HBox(15);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("merit-row");
                row.setPadding(new Insets(10));

                TextField nameField = new TextField();
                nameField.setPromptText("Merit Name (e.g. Artifact)");
                nameField.textProperty().bindBidirectional(merit.nameProperty());
                nameField.setPrefWidth(250);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                DotSelector selector = new DotSelector(merit.ratingProperty(), 1, 5);

                Button removeBtn = new Button("✕");
                removeBtn.getStyleClass().add("remove-btn");
                removeBtn.setTooltip(new Tooltip("Remove Merit"));
                removeBtn.setStyle(
                        "-fx-text-fill: #ff4444; -fx-background-color: transparent; -fx-font-weight: bold; -fx-font-size: 14px;");
                removeBtn.setOnAction(e -> data.getMerits().remove(merit));

                row.getChildren().addAll(nameField, spacer, selector, removeBtn);
                meritsList.getChildren().add(row);
            }
        };

        data.getMerits().addListener((javafx.collections.ListChangeListener<? super Merit>) c -> {
            boolean needsRefresh = false;
            while (c.next()) {
                if (c.wasAdded() || c.wasRemoved() || c.wasPermutated()) {
                    needsRefresh = true;
                    break;
                }
            }
            if (needsRefresh) refreshMerits.run();
        });
        refreshMerits.run();

        Button addBtn = new Button("+ Add New Merit");
        addBtn.getStyleClass().add("action-btn");
        addBtn.setPrefWidth(200);
        addBtn.setOnAction(e -> data.getMerits().add(new Merit("", 1)));

        content.getChildren().addAll(title, meritsList, addBtn);
        return content;
    }

    private javafx.scene.Node createCharmsContent() {
        charmsAbilityCombo = new ComboBox<>();
        charmsAbilityCombo.getItems().addAll(
            CharacterData.ABILITIES.stream()
                .filter(a -> !a.equals("Martial Arts"))
                .collect(Collectors.toList())
        );
        charmsAbilityCombo.setValue("Archery");
        
        CharmTreeComponent charmsView = new CharmTreeComponent(data, dataService, keywordDefs, charmTreeListener, charmsAbilityCombo, "Ability");
        charmTrees.add(charmsView);
        
        VBox layout = new VBox(0);
        layout.getChildren().addAll(charmsView, createPurchasedCharmsSummary(false));
        VBox.setVgrow(charmsView, Priority.ALWAYS);
        
        return layout;
    }

    private VBox createPurchasedCharmsSummary(boolean martialArtsMode) {
        VBox summary = new VBox(10);
        summary.getStyleClass().add("purchased-charms-summary");
        summary.setPadding(new Insets(15));
        summary.setStyle("-fx-background-color: #1a1a1a; -fx-border-color: #333; -fx-border-width: 1 0 0 0;");

        Label title = new Label("Purchased Charms Summary");
        title.getStyleClass().add("subsection-title");
        summary.getChildren().add(title);

        FlowPane flow = new FlowPane(15, 10);
        summary.getChildren().add(flow);

        Runnable refreshSummary = () -> {
            flow.getChildren().clear();
            
            // Filter by mode (Solar vs Martial Arts)
            List<com.vibethema.model.PurchasedCharm> filtered = data.getUnlockedCharms().stream()
                .filter(pc -> data.isMartialArtsStyle(pc.ability()) == martialArtsMode)
                .collect(Collectors.toList());

            // Group by Ability
            Map<String, List<com.vibethema.model.PurchasedCharm>> groupedByAbility = filtered.stream()
                .collect(Collectors.groupingBy(com.vibethema.model.PurchasedCharm::ability));

            List<String> sortedAbilities = new ArrayList<>(groupedByAbility.keySet());
            Collections.sort(sortedAbilities);

            for (String ability : sortedAbilities) {
                VBox groupContainer = new VBox(5);
                Label abLabel = new Label(ability.toUpperCase());
                abLabel.getStyleClass().add("sidebar-stat-header");
                abLabel.setStyle("-fx-text-fill: #d4af37; -fx-font-size: 11px;");
                groupContainer.getChildren().add(abLabel);

                // Group by Charm within the ability to consolidate stackables
                Map<com.vibethema.model.PurchasedCharm, Long> counts = groupedByAbility.get(ability).stream()
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

                List<com.vibethema.model.PurchasedCharm> uniqueCharms = new ArrayList<>(counts.keySet());
                uniqueCharms.sort((a, b) -> a.name().compareTo(b.name()));

                for (com.vibethema.model.PurchasedCharm pc : uniqueCharms) {
                    long count = counts.get(pc);
                    String displayName = pc.name();
                    if (count > 1) displayName += " (x" + count + ")";
                    
                    Label charmLabel = new Label(displayName);
                    charmLabel.getStyleClass().add("charm-summary-item");
                    charmLabel.setStyle("-fx-text-fill: #f9f6e6; -fx-cursor: hand;");
                    
                    charmLabel.setOnMouseClicked(e -> {
                        if (e.getClickCount() == 2) {
                            jumpToCharm(pc.id(), pc.ability());
                        }
                    });
                    
                    Tooltip.install(charmLabel, new Tooltip("Double-click to view charm in tree"));
                    groupContainer.getChildren().add(charmLabel);
                }
                flow.getChildren().add(groupContainer);
            }
        };

        data.getUnlockedCharms().addListener((javafx.collections.ListChangeListener<? super com.vibethema.model.PurchasedCharm>) c -> refreshSummary.run());
        refreshSummary.run();

        return summary;
    }


    private void cycleTab(int direction) {
        if (mainTabPane == null) return;
        int count = mainTabPane.getTabs().size();
        if (count == 0) return;
        int current = mainTabPane.getSelectionModel().getSelectedIndex();
        int next = (current + direction + count) % count;
        selectAndFocusTab(next);
    }

    private void selectAndFocusTab(int index) {
        if (mainTabPane == null || index < 0 || index >= mainTabPane.getTabs().size()) return;
        Tab tab = mainTabPane.getTabs().get(index);
        mainTabPane.getSelectionModel().select(tab);
        focusFirstInTab(tab);
    }

    private void focusFirstInTab(Tab tab) {
        if (tab == null || tab.getContent() == null) return;
        javafx.scene.Node content = tab.getContent();
        if (content instanceof ScrollPane sp) {
            content = sp.getContent();
        }
        
        javafx.scene.Node first = findFirstFocusable(content);
        if (first != null) {
            Platform.runLater(first::requestFocus);
        }
    }

    private javafx.scene.Node findFirstFocusable(javafx.scene.Node node) {
        if (node instanceof Control c && !c.isDisabled() && c.isFocusTraversable() && c.isVisible()) {
            return c;
        }
        if (node instanceof DotSelector && !node.isDisabled() && node.isVisible()) {
            return node;
        }
        if (node instanceof Parent p) {
            for (javafx.scene.Node child : p.getChildrenUnmodifiable()) {
                javafx.scene.Node f = findFirstFocusable(child);
                if (f != null) return f;
            }
        }
        return null;
    }

    private void jumpToCharmAbility(String abilityName) {
        if (mainTabPane == null) return;
        
        if (data.isMartialArtsStyle(abilityName)) {
            if (martialArtsTab != null && stylePicker != null) {
                mainTabPane.getSelectionModel().select(martialArtsTab);
                stylePicker.setValue(abilityName);
            }
        } else if ("Martial Arts".equals(abilityName)) {
            if (martialArtsTab != null) {
                mainTabPane.getSelectionModel().select(martialArtsTab);
            }
        } else {
            if (charmsTab != null && charmsAbilityCombo != null) {
                mainTabPane.getSelectionModel().select(charmsTab);
                charmsAbilityCombo.setValue(abilityName);
            }
        }
    }

    private void jumpToCharm(String charmId, String ability) {
        jumpToCharmAbility(ability);
        for (CharmTreeComponent tree : charmTrees) {
            // Find the tree that is currently active for this ability/style
            if (ability.equals(tree.getCurrentFilterValue())) {
                tree.selectCharm(charmId);
                break;
            }
        }
    }

    private void loadKeywords() {
        if (dataService == null) return;
        List<Keyword> keywords = dataService.loadKeywords();
        for (Keyword kw : keywords) {
            keywordDefs.put(kw.getName(), kw.getDescription());
        }
    }

    private void importPdf() {
        PdfImportHelper.importCorePdf(getScene().getWindow(), this::refreshCharms);
    }

    private void importMosePdf() {
        PdfImportHelper.importMosePdf(getScene().getWindow(), this::refreshCharms);
    }

    private void refreshCharms() {
        loadKeywords();
        for (CharmTreeComponent tree : charmTrees) {
            tree.refresh();
        }
    }



    private void showEvocationsDialog(String id, String name) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(getScene().getWindow());
        dialog.setTitle("Evocations: " + name);

        CharmTreeComponent tree = new CharmTreeComponent(data, dataService, keywordDefs, charmTreeListener, null, "Evocation", id, name);
        
        VBox root = new VBox(tree);
        VBox.setVgrow(tree, Priority.ALWAYS);
        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().addAll(getScene().getStylesheets());
        dialog.setScene(scene);
        dialog.show();
    }

    private void showCreateCharmDialog(String contextId, String contextName, String filterType, Runnable onSave) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(getScene().getWindow());
        String term = "Evocation".equals(filterType) ? "Evocation" : "Charm";
        dialog.setTitle("Create Custom " + term);

        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-pane");
        root.setStyle("-fx-background-color: #1e1e1e;");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(120);
        grid.getColumnConstraints().add(labelCol);

        TextField nameField = new TextField();
        nameField.setPromptText(term + " Name");
        
        ComboBox<String> abCombo = new ComboBox<>();
        if ("Martial Arts Style".equals(filterType)) {
            for (MartialArtsStyle mas : data.getMartialArtsStyles()) {
                if (mas.getStyleName() != null && !mas.getStyleName().isEmpty() && !abCombo.getItems().contains(mas.getStyleName())) {
                    abCombo.getItems().add(mas.getStyleName());
                }
            }
        } else if ("Evocation".equals(filterType)) {
            abCombo.getItems().add(contextName);
            abCombo.setDisable(true);
        } else {
            abCombo.getItems().addAll(CharacterData.ABILITIES);
        }
        abCombo.setValue(contextName != null ? contextName : "Archery");

        Spinner<Integer> minAb = new Spinner<>(0, 5, 0);
        minAb.setEditable(true);
        Spinner<Integer> minEss = new Spinner<>(1, 5, 1);
        minEss.setEditable(true);
        
        TextField costField = new TextField();
        costField.setPromptText("e.g. 5m");
        
        TextField typeField = new TextField();
        typeField.setPromptText("e.g. Simple");
        
        TextField durationField = new TextField();
        durationField.setPromptText("e.g. Instant");
        
        ObservableList<String> selectedKeywords = FXCollections.observableArrayList();
        FlowPane kwDisplay = new FlowPane(5, 5);
        kwDisplay.setPrefWrapLength(300);
        
        Runnable updateKwUI = () -> {
            kwDisplay.getChildren().clear();
            for (String kw : selectedKeywords) {
                Label l = new Label(kw);
                l.setStyle("-fx-background-color: #3d3d3d; -fx-padding: 2 8 2 8; -fx-background-radius: 10; -fx-text-fill: white; -fx-font-size: 0.9em;");
                kwDisplay.getChildren().add(l);
            }
            if (selectedKeywords.isEmpty()) {
                Label none = new Label("None selected");
                none.setStyle("-fx-text-fill: #888888; -fx-font-style: italic;");
                kwDisplay.getChildren().add(none);
            }
        };
        updateKwUI.run();

        Button selectKwBtn = new Button("Select Keywords...");
        selectKwBtn.getStyleClass().add("secondary-btn");
        selectKwBtn.setOnAction(e -> UIUtils.showKeywordSelectionDialog(getScene().getWindow(), dataService, new ArrayList<>(selectedKeywords), result -> {
            selectedKeywords.setAll(result);
            updateKwUI.run();
        }));
        
        VBox kwBox = new VBox(5, kwDisplay, selectKwBtn);

        TextArea descArea = new TextArea();
        descArea.setPromptText("Full " + term + " Description...");
        descArea.setPrefRowCount(5);
        descArea.setWrapText(true);

        ListView<String> prereqList = new ListView<>();
        prereqList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        prereqList.setPrefHeight(100);

        Map<String, String> nameToId = new HashMap<>();
        Runnable updatePrereqs = () -> {
            String selectedAb = abCombo.getValue();
            List<Charm> charms;
            if ("Evocation".equals(filterType)) {
                CharmDataService.EvocationCollection col = dataService.loadEvocations(selectedAb);
                charms = col != null ? col.evocations : new ArrayList<>();
            } else {
                charms = dataService.loadCharmsForAbility(selectedAb);
            }
            nameToId.clear();
            for (Charm c : charms) nameToId.put(c.getName(), c.getId());
            prereqList.getItems().setAll(charms.stream().map(Charm::getName).collect(Collectors.toList()));
        };
        abCombo.valueProperty().addListener((obs, oldV, newV) -> updatePrereqs.run());
        updatePrereqs.run();

        Label abLabel = new Label("Ability:");
        Label minAbLabel = new Label("Min Ability:");
        
        if ("Evocation".equals(filterType)) {
            abLabel.setVisible(false); abLabel.setManaged(false);
            abCombo.setVisible(false); abCombo.setManaged(false);
            minAbLabel.setVisible(false); minAbLabel.setManaged(false);
            minAb.setVisible(false); minAb.setManaged(false);
        }

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(abLabel, 0, 1);
        grid.add(abCombo, 1, 1);
        grid.add(minAbLabel, 0, 2);
        grid.add(minAb, 1, 2);
        grid.add(new Label("Min Essence:"), 0, 3);
        grid.add(minEss, 1, 3);
        grid.add(new Label("Cost:"), 0, 4);
        grid.add(costField, 1, 4);
        grid.add(new Label("Type:"), 0, 5);
        grid.add(typeField, 1, 5);
        grid.add(new Label("Duration:"), 0, 6);
        grid.add(durationField, 1, 6);
        grid.add(new Label("Keywords:"), 0, 7);
        grid.add(kwBox, 1, 7);
        grid.add(new Label("Prerequisites:"), 2, 0);
        grid.add(prereqList, 2, 1, 1, 7);

        VBox descBox = new VBox(5, new Label("Description:"), descArea);
        
        for (javafx.scene.Node n : grid.getChildren()) {
            if (n instanceof Label l) l.setStyle("-fx-text-fill: #f9f6e6;");
        }
        ((Label)descBox.getChildren().get(0)).setStyle("-fx-text-fill: #f9f6e6;");

        Button saveBtn = new Button("Save " + term);
        saveBtn.getStyleClass().add("action-btn");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> {
            if (nameField.getText().isEmpty()) {
                new Alert(Alert.AlertType.ERROR, term + " name cannot be empty.").showAndWait();
                return;
            }
            
            Charm nc = "Evocation".equals(filterType) ? new Evocation() : new SolarCharm();
            nc.setName(nameField.getText());
            nc.setAbility("Evocation".equals(filterType) ? contextId : abCombo.getValue());
            nc.setMinAbility(minAb.getValue());
            nc.setMinEssence(minEss.getValue());
            nc.setCost(costField.getText());
            nc.setType(typeField.getText());
            nc.setDuration(durationField.getText());
            nc.setFullText(descArea.getText());
            nc.setCategory("Evocation".equals(filterType) ? "evocation" : "solar");
            
            nc.setKeywords(new ArrayList<>(selectedKeywords));
            
            List<String> selectedIds = prereqList.getSelectionModel().getSelectedItems().stream()
                .map(nameToId::get)
                .collect(Collectors.toList());
            nc.setPrerequisites(selectedIds);
            nc.setCustom(true);

            try {
                if ("Evocation".equals(filterType)) {
                    dataService.saveEvocation(contextId, contextName, nc);
                } else {
                    dataService.saveCharm(nc);
                }
                data.setDirty(true);
                if (onSave != null) onSave.run();
                refreshCharms();
                dialog.close();
            } catch (IOException ex) {
                new Alert(Alert.AlertType.ERROR, "Error saving " + term + ": " + ex.getMessage()).showAndWait();
            }
        });

        root.getChildren().addAll(grid, descBox, saveBtn);
        Scene scene = new Scene(root, 700, 750);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.show();
    }

    private void showEditCharmDialog(Charm charm, String contextName, String filterType, Runnable onSave) {
        Dialog<ButtonType> dialog = new Dialog<>();
        String term = "Evocation".equals(filterType) || "evocation".equals(charm.getCategory()) ? "Evocation" : "Charm";
        dialog.setTitle("Edit " + term + ": " + charm.getName());
        dialog.setHeaderText("Modify charm details and prerequisites.");
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane-custom");
        dialogPane.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        dialogPane.setStyle("-fx-background-color: #1e1e1e;");

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(charm.getName());
        ComboBox<String> abCombo = new ComboBox<>();
        abCombo.getItems().addAll(CharacterData.ABILITIES);
        abCombo.getItems().addAll(dataService.getAvailableMartialArtsStyles());
        abCombo.setValue(charm.getAbility());
        
        Spinner<Integer> minAb = new Spinner<>(0, 5, charm.getMinAbility());
        Spinner<Integer> minEss = new Spinner<>(1, 5, charm.getMinEssence());
        TextField costField = new TextField(charm.getCost());
        TextField typeField = new TextField(charm.getType());
        String initialKeywords = charm.getKeywords() != null ? String.join(", ", charm.getKeywords()) : "";
        TextField keywordsField = new TextField(initialKeywords);
        TextField durationField = new TextField(charm.getDuration());
        TextArea descArea = new TextArea(charm.getFullText());
        descArea.setWrapText(true);
        descArea.setPrefRowCount(10);

        TextArea rawDataArea = new TextArea(charm.getRawData() != null ? charm.getRawData() : "No raw data available.");
        rawDataArea.setEditable(false);
        rawDataArea.setWrapText(true);
        rawDataArea.setPrefRowCount(8);
        rawDataArea.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 11px; -fx-opacity: 0.8;");

        CheckBox problemCheck = new CheckBox("Problematic Import (⚠️ warning icon)");
        problemCheck.setSelected(charm.isPotentiallyProblematicImport());
        problemCheck.setStyle("-fx-text-fill: #f9f6e6;");

        ListView<String> prereqList = new ListView<>();
        prereqList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        prereqList.setPrefHeight(120);

        Map<String, String> nameToId = new HashMap<>();
        Map<String, String> idToNameLocal = new HashMap<>(); // For initial selection
        Runnable updatePrereqs = () -> {
            String selectedAb = abCombo.getValue();
            List<Charm> charms;
            if ("Evocation".equals(filterType) || "evocation".equals(charm.getCategory())) {
                CharmDataService.EvocationCollection col = dataService.loadEvocations(selectedAb);
                charms = col != null ? col.evocations : new ArrayList<>();
            } else {
                charms = dataService.loadCharmsForAbility(selectedAb);
            }
            nameToId.clear();
            for (Charm c : charms) {
                if (!c.getId().equals(charm.getId())) { // Don't allow self-prerequisite
                    nameToId.put(c.getName(), c.getId());
                    idToNameLocal.put(c.getId(), c.getName());
                }
            }
            prereqList.getItems().setAll(nameToId.keySet().stream().sorted().collect(Collectors.toList()));
            
            // Re-select existing prerequisites
            if (charm.getPrerequisites() != null) {
                for (String rid : charm.getPrerequisites()) {
                    String rname = idToNameLocal.get(rid);
                    if (rname != null) {
                        prereqList.getSelectionModel().select(rname);
                    }
                }
            }
        };
        abCombo.valueProperty().addListener((obs, oldV, newV) -> updatePrereqs.run());
        updatePrereqs.run();

        grid.add(new Label("Name:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Ability:"), 0, 1); grid.add(abCombo, 1, 1);
        grid.add(new Label("Min Ability:"), 0, 2); grid.add(minAb, 1, 2);
        grid.add(new Label("Min Essence:"), 0, 3); grid.add(minEss, 1, 3);
        grid.add(new Label("Cost:"), 0, 4); grid.add(costField, 1, 4);
        grid.add(new Label("Type:"), 0, 5); grid.add(typeField, 1, 5);
        grid.add(new Label("Keywords:"), 0, 6); grid.add(keywordsField, 1, 6);
        grid.add(new Label("Duration:"), 0, 7); grid.add(durationField, 1, 7);
        grid.add(problemCheck, 1, 8);
        grid.add(new Label("Prerequisites:"), 2, 0); grid.add(prereqList, 2, 1, 1, 8);

        VBox rightColumn = new VBox(10);
        rightColumn.getChildren().addAll(new Label("Description:"), descArea, new Label("Raw Import Data (Read Only):"), rawDataArea);
        
        for (javafx.scene.Node n : grid.getChildren()) {
            if (n instanceof Label l) l.setStyle("-fx-text-fill: #f9f6e6; -fx-font-weight: bold;");
        }
        for (javafx.scene.Node n : rightColumn.getChildren()) {
            if (n instanceof Label l) l.setStyle("-fx-text-fill: #f9f6e6; -fx-font-weight: bold;");
        }

        HBox mainContent = new HBox(20, grid, rightColumn);
        dialogPane.setContent(mainContent);

        ButtonType saveType = new ButtonType("Save Edits", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == saveType) {
                charm.setName(nameField.getText());
                charm.setAbility(abCombo.getValue());
                charm.setMinAbility(minAb.getValue());
                charm.setMinEssence(minEss.getValue());
                charm.setCost(costField.getText());
                charm.setType(typeField.getText());
                
                List<String> kwList = new ArrayList<>();
                if (!keywordsField.getText().trim().isEmpty()) {
                    for (String kw : keywordsField.getText().split(",")) {
                        String t = kw.trim();
                        if (!t.isEmpty() && !t.equalsIgnoreCase("None")) kwList.add(t);
                    }
                }
                charm.setKeywords(kwList);
                charm.setDuration(durationField.getText());
                charm.setFullText(descArea.getText());
                charm.setPotentiallyProblematicImport(problemCheck.isSelected());
                
                List<String> selectedIds = prereqList.getSelectionModel().getSelectedItems().stream()
                    .map(nameToId::get)
                    .collect(Collectors.toList());
                charm.setPrerequisites(selectedIds);
                
                try {
                    if ("evocation".equals(charm.getCategory())) {
                        dataService.saveEvocation(charm.getAbility(), contextName, charm);
                    } else {
                        dataService.saveCharm(charm);
                    }
                    data.setDirty(true);
                    if (onSave != null) onSave.run();
                    return bt;
                } catch (IOException ex) {
                    new Alert(Alert.AlertType.ERROR, "Failed to save " + term.toLowerCase() + ": " + ex.getMessage()).showAndWait();
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(r -> refreshCharms());
    }

}
