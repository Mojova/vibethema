package com.vibethema.ui;

import com.vibethema.service.CharmDataService;
import com.vibethema.service.PdfExtractor;
import com.vibethema.model.CharacterData;
import com.vibethema.model.Charm;
import com.vibethema.model.MartialArtsStyle;
import com.vibethema.model.PurchasedCharm;
import com.vibethema.model.Specialty;
import com.vibethema.service.CharmDataService;
import com.vibethema.service.PdfExtractor;
import com.google.gson.Gson;
import javafx.beans.binding.Bindings;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.shape.CubicCurve;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.input.KeyCombination;
import javafx.concurrent.Task;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    private List<CharmTreeComponent> charmTrees = new java.util.ArrayList<>();

    public BuilderUI(CharacterData data) {
        this(data, null);
    }

    public BuilderUI(CharacterData data, File currentFile) {
        this.data = data;
        this.currentFile = currentFile;
        this.dataService = new CharmDataService();
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

        Tab charmsTab = new Tab("Charms");
        charmsTab.setClosable(false);
        charmsTab.setContent(createCharmsContent());

        Tab martialArtsTab = new Tab("Martial Arts");
        martialArtsTab.setClosable(false);
        martialArtsTab.setContent(createMartialArtsContent());

        tabPane.getTabs().addAll(statsTab, meritsTab, equipmentTab, charmsTab, martialArtsTab);

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

        MenuItem importPdfItem = new MenuItem("Import Core PDF...");
        importPdfItem.setOnAction(e -> importPdf());

        fileMenu.getItems().addAll(saveItem, saveAsItem, loadItem, new SeparatorMenuItem(), importPdfItem);
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
        meritsLabel.setText("Merits: " + data.getMeritTotal() + "/10");
        specialtiesLabel.setText("Specialties: " + data.getSpecialties().size() + "/4");
        casteLabel.setText("Caste: " + casteCount + "/5");
        favoredLabel.setText("Favored: " + favoredCount + "/5");
        bpLabel.setText("Bonus Points: " + bp + "/15");

        updateAllWebNodeStyles();
    }

    private void updateAllWebNodeStyles() {
        for (CharmTreeComponent tree : charmTrees) {
            tree.updateWebNodeStyles();
        }
    }

    private VBox createEquipmentContent() {
        VBox content = new VBox(25);
        content.getStyleClass().add("content-area");
        content.setPadding(new Insets(20));

        VBox weaponsSection = createEquipmentSection("Weapons");
        VBox armorSection = createEquipmentSection("Armor");
        VBox hearthstonesSection = createEquipmentSection("Hearthstones");
        VBox otherSection = createEquipmentSection("Other");

        content.getChildren().addAll(weaponsSection, armorSection, hearthstonesSection, otherSection);
        return content;
    }

    private VBox createEquipmentSection(String titleText) {
        VBox section = new VBox(10);
        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");
        section.getChildren().add(title);
        return section;
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

        int row = 0;
        int col = 0;
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
                return notInCasteList || atLimit;
            }, data.casteProperty(), casteCount, data.getCasteAbility(ability)));

            CheckBox favoredBox = new CheckBox("F");
            favoredBox.getStyleClass().add("favored-checkbox");
            favoredBox.selectedProperty().bindBidirectional(data.getFavoredAbility(ability));
            favoredBox.disableProperty().bind(Bindings.createBooleanBinding(() -> {
                if ("Martial Arts".equals(ability))
                    return true; // Synced with Brawl
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
            rowBox.getChildren().addAll(casteBox, favoredBox, abLabel, selector);

            abilGrid.add(rowBox, col, row);
            row++;
            if (row >= 13) {
                row = 0;
                col++;
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

        content.getChildren().addAll(advantagesSection, attributesSection, abilAndSide);
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

        ComboBox<String> stylePicker = new ComboBox<>();
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
            for (MartialArtsStyle mas : data.getMartialArtsStyles()) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("merit-row");
                row.setPadding(new Insets(8));

                Label nameLabel = new Label(mas.getStyleName());
                nameLabel.getStyleClass().add("label");
                nameLabel.setPrefWidth(180);

                DotSelector selector = new DotSelector(mas.ratingProperty(), 0, 5);

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
                    refreshStyles.run();
                    // Listeners for individual style names
                    while (c.next()) {
                        if (c.wasAdded()) {
                            for (MartialArtsStyle mas : c.getAddedSubList()) {
                                mas.styleNameProperty().addListener((obs, ov, nv) -> refreshStylePicker.run());
                            }
                        }
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

        CharmTreeComponent maCharmTree = new CharmTreeComponent(stylePicker, "Martial Arts Style");
        charmTrees.add(maCharmTree);
        
        VBox fullLayout = new VBox(0);
        fullLayout.getChildren().addAll(content, maCharmTree);
        VBox.setVgrow(maCharmTree, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane(fullLayout);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane-custom");
        return scrollPane;
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
            return notInCasteList || atLimit;
        }, data.casteProperty(), casteCount, data.getCasteAbility("Craft")));

        CheckBox favoredBox = new CheckBox("F");
        favoredBox.getStyleClass().add("favored-checkbox");
        favoredBox.selectedProperty().bindBidirectional(data.getFavoredAbility("Craft"));
        favoredBox.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            boolean isCaste = data.getCasteAbility("Craft").get();
            boolean atLimit = favoredCount.get() >= 5 && !data.getFavoredAbility("Craft").get();
            return isCaste || atLimit;
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
            for (CraftAbility ca : data.getCrafts()) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("merit-row");
                row.setPadding(new Insets(8));

                TextField expertiseField = new TextField();
                expertiseField.setPromptText("Expertise (e.g. Metallurgy)");
                expertiseField.textProperty().bindBidirectional(ca.expertiseProperty());
                expertiseField.setPrefWidth(180);

                DotSelector selector = new DotSelector(ca.ratingProperty(), 0, 5);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button removeBtn = new Button("✕");
                removeBtn.getStyleClass().add("remove-btn");
                removeBtn.setOnAction(e -> data.getCrafts().remove(ca));

                row.getChildren().addAll(expertiseField, spacer, selector, removeBtn);
                craftList.getChildren().add(row);
            }
        };

        data.getCrafts()
                .addListener((javafx.collections.ListChangeListener<? super CraftAbility>) c -> refreshCrafts.run());
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

        data.getSpecialties()
                .addListener((javafx.collections.ListChangeListener<? super Specialty>) c -> refreshSpecs.run());
        refreshSpecs.run();

        Button addBtn = new Button("+ Add Specialty");
        addBtn.getStyleClass().add("action-btn");
        addBtn.setOnAction(e -> data.getSpecialties().add(new Specialty("", "")));

        section.getChildren().addAll(title, specList, addBtn);
        return section;
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
        Label healthLabel = new Label("Health Levels");
        healthLabel.getStyleClass().add("subsection-title");

        HBox trackBoxes = new HBox(15);
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

        advantagesSection.getChildren().addAll(advTitle, topRow, healthBox);
        return advantagesSection;
    }

    private HBox createHealthLevel(String label, int count) {
        HBox box = new HBox(5);
        box.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label);
        l.getStyleClass().add("label");
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

        data.getMerits()
                .addListener((javafx.collections.ListChangeListener.Change<? extends Merit> c) -> refreshMerits.run());
        refreshMerits.run();

        Button addBtn = new Button("+ Add New Merit");
        addBtn.getStyleClass().add("action-btn");
        addBtn.setPrefWidth(200);
        addBtn.setOnAction(e -> data.getMerits().add(new Merit("", 1)));

        content.getChildren().addAll(title, meritsList, addBtn);
        return content;
    }

    private javafx.scene.Node createCharmsContent() {
        ComboBox<String> abilityCombo = new ComboBox<>();
        abilityCombo.getItems().addAll(
            CharacterData.ABILITIES.stream()
                .filter(a -> !a.equals("Martial Arts"))
                .collect(Collectors.toList())
        );
        abilityCombo.setValue("Archery");
        
        CharmTreeComponent charmsView = new CharmTreeComponent(abilityCombo, "Ability");
        charmTrees.add(charmsView);
        return charmsView;
    }

    private void loadKeywords() {
        if (dataService == null) return;
        List<Keyword> keywords = dataService.loadKeywords();
        for (Keyword kw : keywords) {
            keywordDefs.put(kw.getName(), kw.getDescription());
        }
    }

    private void importPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Exalted Core 3e PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showOpenDialog(getScene().getWindow());

        if (file != null) {
            showImportProgress(file);
        }
    }

    private void showImportProgress(File pdfFile) {
        Stage progressStage = new Stage();
        progressStage.initModality(Modality.WINDOW_MODAL);
        progressStage.initOwner(getScene().getWindow());
        progressStage.setTitle("Importing PDF...");

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #1e1e1e;");

        Label label = new Label("Extracting data from PDF...\nThis may take a minute.");
        label.setStyle("-fx-text-fill: #f9f6e6; -fx-text-alignment: center;");
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);

        layout.getChildren().addAll(label, progressBar);
        progressStage.setScene(new javafx.scene.Scene(layout));
        progressStage.show();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                PdfExtractor extractor = new PdfExtractor();
                extractor.extractAll(pdfFile, progress -> {
                    Platform.runLater(() -> progressBar.setProgress(progress));
                });
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            progressStage.close();
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Import complete! Charms and keywords have been updated.", ButtonType.OK);
            alert.showAndWait();
            refreshCharms();
        });

        task.setOnFailed(e -> {
            progressStage.close();
            Throwable ex = task.getException();
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Import failed: " + ex.getMessage(), ButtonType.OK);
            alert.showAndWait();
        });

        new Thread(task).start();
    }

    private void refreshCharms() {
        loadKeywords();
        for (CharmTreeComponent tree : charmTrees) {
            tree.refresh();
        }
    }

    private class CharmTreeComponent extends SplitPane {
        private static final Map<String, String> globalCharmNameMap = new java.util.concurrent.ConcurrentHashMap<>();
        private final ComboBox<String> filterCombo;
        private final String filterType; // "Ability" or "Martial Arts Style"
        private Pane charmCanvas = new Pane();
        private Map<String, VBox> charmNodeMap = new HashMap<>();
        private List<Charm> currentCharms = new ArrayList<>();
        
        private Label titleLabel = new Label();
        private Label detailTitle = new Label("No Charm Selected");
        private Label detailReqs = new Label();
        
        private Label costLabel = new Label();
        private Label typeLabel = new Label();
        private Label durationLabel = new Label();
        private FlowPane kwFlow = new FlowPane(3, 3);
        private Label descriptionLabel = new Label();
        
        private Button toggleBtn = new Button("Purchase Charm");
        private Button refundBtn = new Button("Refund");
        private Button deleteCustomBtn = new Button("Delete Custom Charm");
        private Button editBtn = new Button("Edit Charm");
        
        private Charm selectedCharm;
        private javafx.beans.property.IntegerProperty currentRatingProperty;
        private javafx.beans.value.ChangeListener<Number> ratingListener;

        public CharmTreeComponent(ComboBox<String> filterCombo, String filterType) {
            this.filterCombo = filterCombo;
            this.filterType = filterType;
            getStyleClass().add("charms-split-pane");

            setupUI();
            filterCombo.valueProperty().addListener((obs, oldV, newV) -> refresh());
            refresh();
        }

        private void setupUI() {
            // Left Side: Tree Web Area
            VBox leftPane = new VBox(15);
            leftPane.setPadding(new Insets(20));

            HBox controls = new HBox(15);
            controls.setAlignment(Pos.CENTER_LEFT);
            Label comboLabel = new Label(filterType + ":");
            comboLabel.getStyleClass().add("label");
            
            Button createCharmBtn = new Button("Create New Charm");
            createCharmBtn.getStyleClass().add("action-btn");
            createCharmBtn.setOnAction(e -> showCreateCharmDialog(filterCombo.getValue(), filterType.equals("Martial Arts Style")));

            controls.getChildren().addAll(comboLabel, filterCombo, createCharmBtn);

            titleLabel.getStyleClass().add("section-title");

            ScrollPane charmScroll = new ScrollPane(charmCanvas);
            charmScroll.setPannable(true);
            charmScroll.getStyleClass().add("scroll-pane-custom");
            VBox.setVgrow(charmScroll, Priority.ALWAYS);

            leftPane.getChildren().addAll(controls, titleLabel, charmScroll);

            // Right Side: Sidebar Details
            VBox rightPane = new VBox(15);
            rightPane.setPadding(new Insets(20));
            rightPane.getStyleClass().add("charms-sidebar");

            detailTitle.getStyleClass().add("sidebar-title");
            detailTitle.setWrapText(true);
            detailReqs.getStyleClass().add("sidebar-reqs");
            detailReqs.setWrapText(true);

            GridPane statsGrid = new GridPane();
            statsGrid.setHgap(15);
            statsGrid.setVgap(10);
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

            for (javafx.scene.Node n : statsGrid.getChildren()) {
                if (GridPane.getColumnIndex(n) == 0 && n instanceof Label) {
                    n.getStyleClass().add("sidebar-stat-header");
                }
            }

            descriptionLabel.getStyleClass().add("sidebar-desc");
            descriptionLabel.setWrapText(true);
            ScrollPane descScroll = new ScrollPane(descriptionLabel);
            descScroll.setFitToWidth(true);
            descScroll.getStyleClass().add("scroll-pane-custom");
            VBox.setVgrow(descScroll, Priority.ALWAYS);

            toggleBtn.getStyleClass().add("charm-btn");
            toggleBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(toggleBtn, Priority.ALWAYS);

            refundBtn.getStyleClass().add("charm-btn");
            refundBtn.setStyle("-fx-base: #a03030;");
            refundBtn.setVisible(false);
            refundBtn.setManaged(false);

            editBtn.getStyleClass().add("charm-btn");
            editBtn.setStyle("-fx-base: #3c3c3c;");
            editBtn.setVisible(false);
            editBtn.setManaged(false);

            HBox charmButtons = new HBox(10, toggleBtn, refundBtn, deleteCustomBtn, editBtn);
            rightPane.getChildren().addAll(detailTitle, detailReqs, charmButtons, statsGrid, descScroll);

            getItems().addAll(leftPane, rightPane);
            setDividerPositions(0.7);
        }

        public void refresh() {
            charmCanvas.getChildren().clear();
            charmNodeMap.clear();
            currentCharms.clear();
            selectedCharm = null;
            
            if (ratingListener != null && currentRatingProperty != null) {
                currentRatingProperty.removeListener(ratingListener);
            }

            String selection = filterCombo.getValue();
            if (selection == null || selection.isEmpty()) {
                titleLabel.setText("Select a " + filterType + " to view charms");
                return;
            }

            List<Charm> loaded = dataService.loadCharmsForAbility(selection);
            if (loaded != null) {
                currentCharms.addAll(loaded);
                for (Charm c : loaded) globalCharmNameMap.put(c.getId(), c.getName());
            }

            currentRatingProperty = data.getAbilityProperty(selection);
            if (currentRatingProperty != null) {
                ratingListener = (obs, ov, nv) -> {
                    updateWebNodeStyles();
                    if (selectedCharm != null) updateSidebarButton(selectedCharm);
                };
                currentRatingProperty.addListener(ratingListener);
            }

            drawCharmWeb(currentCharms, selection);
            titleLabel.setText(selection + " Charms Web (" + currentCharms.size() + ")");

            detailTitle.setText("No Charm Selected");
            detailReqs.setText("");
            costLabel.setText("");
            typeLabel.setText("");
            durationLabel.setText("");
            kwFlow.getChildren().clear();
            descriptionLabel.setText("");
            toggleBtn.setVisible(false);
            refundBtn.setVisible(false);
            refundBtn.setManaged(false);
            deleteCustomBtn.setVisible(false);
            deleteCustomBtn.setManaged(false);
        }

        private void drawCharmWeb(List<Charm> charms, String ability) {
            // Mapping for display resolution (ID -> Name)
            Map<String, String> idToName = new HashMap<>();
            for (Charm c : charms) idToName.put(c.getId(), c.getName());

            // Topological Sort Logic - User IDs for depth calculation
            Map<String, Integer> charmDepth = new HashMap<>();
            boolean changed = true;
            while (changed) {
                changed = false;
                for (Charm c : charms) {
                    int currentDepth = charmDepth.getOrDefault(c.getId(), 0);
                    int reqDepth = 0;
                    if (c.getPrerequisites() != null) {
                        for (String reqId : c.getPrerequisites()) {
                            reqDepth = Math.max(reqDepth, charmDepth.getOrDefault(reqId, 0) + 1);
                        }
                    }
                    if (reqDepth > currentDepth) {
                        charmDepth.put(c.getId(), reqDepth);
                        changed = true;
                    }
                }
            }

            Map<Integer, List<Charm>> levels = new HashMap<>();
            int maxDepth = 0;
            for (Charm c : charms) {
                int depth = charmDepth.getOrDefault(c.getId(), 0);
                levels.computeIfAbsent(depth, k -> new ArrayList<>()).add(c);
                if (depth > maxDepth) maxDepth = depth;
            }

            double boxWidth = 220; double boxHeight = 70;
            double gapX = 40; double gapY = 80;

            double maxRowWidth = 0;
            for (int d = 0; d <= maxDepth; d++) {
                List<Charm> rowCharms = levels.getOrDefault(d, new ArrayList<>());
                double rowWidth = rowCharms.size() * boxWidth + Math.max(0, rowCharms.size() - 1) * gapX;
                if (rowWidth > maxRowWidth) maxRowWidth = rowWidth;
            }

            double virtualCanvasWidth = Math.max(800, maxRowWidth + 100);

            for (int d = 0; d <= maxDepth; d++) {
                List<Charm> rowCharms = levels.getOrDefault(d, new ArrayList<>());
                double rowWidth = rowCharms.size() * boxWidth + Math.max(0, rowCharms.size() - 1) * gapX;
                double startX = (virtualCanvasWidth - rowWidth) / 2;
                double y = 40 + d * (boxHeight + gapY);

                for (int i = 0; i < rowCharms.size(); i++) {
                    Charm c = rowCharms.get(i);
                    double x = startX + i * (boxWidth + gapX);

                    VBox box = new VBox(5);
                    box.setAlignment(Pos.CENTER);
                    box.setPrefSize(boxWidth, boxHeight);
                    box.getStyleClass().add("charm-node");

                    Label nameLbl = new Label((c.isPotentiallyProblematicImport() ? "⚠️ " : "") + c.getName());
                    nameLbl.getStyleClass().add("charm-node-title");
                    nameLbl.setWrapText(true);

                    Label reqLbl = new Label(c.getAbility() + " " + c.getMinAbility() + ", Ess " + c.getMinEssence());
                    reqLbl.getStyleClass().add("charm-node-reqs");

                    box.getChildren().addAll(nameLbl, reqLbl);
                    if (c.isPotentiallyProblematicImport()) {
                        Tooltip tt = new Tooltip("Warning: This charm may have incomplete data due to a problematic PDF import.");
                        tt.setWrapText(true);
                        tt.setMaxWidth(250);
                        Tooltip.install(box, tt);
                    }
                    box.setLayoutX(x);
                    box.setLayoutY(y);

                    charmNodeMap.put(c.getId(), box);
                    charmCanvas.getChildren().add(box);

                    box.setOnMouseClicked(e -> {
                        selectedCharm = c;
                        detailTitle.setText(c.getName());
                        
                        List<String> prereqNames = new ArrayList<>();
                        if (c.getPrerequisites() != null) {
                            for (String rid : c.getPrerequisites()) {
                                String resolvedName = globalCharmNameMap.get(rid);
                                if (resolvedName == null) resolvedName = idToName.getOrDefault(rid, rid);
                                prereqNames.add(resolvedName);
                            }
                        }
                        String prereqStr = prereqNames.isEmpty() ? "None" : String.join(", ", prereqNames);
                        
                        String baseReqs = "Mins: " + c.getAbility() + " " + c.getMinAbility() + ", Ess: "
                                + c.getMinEssence() + "\nPrereqs: " + prereqStr;
                        if (c.isPotentiallyProblematicImport()) {
                            baseReqs = "⚠️ WARNING: Problematic Import\n" + baseReqs;
                        }
                        detailReqs.setText(baseReqs);
                        costLabel.setText(c.getCost() != null ? c.getCost() : "");
                        typeLabel.setText(c.getType() != null ? c.getType() : "");
                        durationLabel.setText(c.getDuration() != null ? c.getDuration() : "");
                        updateKeywords(c.getKeywords(), kwFlow);
                        descriptionLabel.setText(c.getFullText() != null ? c.getFullText() : "");

                        toggleBtn.setVisible(true);
                        editBtn.setVisible(true);
                        editBtn.setManaged(true);
                        editBtn.setOnAction(ev -> showEditCharmDialog(c));
                        updateSidebarButton(c);

                        if (e.getClickCount() == 2) {
                            javafx.application.Platform.runLater(() -> toggleBtn.fire());
                        }
                    });
                }
            }

            toggleBtn.setOnAction(ev -> {
                if (selectedCharm != null) {
                    if (!data.hasCharm(selectedCharm.getId())) {
                        data.addCharm(new PurchasedCharm(selectedCharm.getId(), selectedCharm.getName(), selectedCharm.getAbility()));
                    } else {
                        data.removeCharm(selectedCharm.getId());
                    }
                    updateSidebarButton(selectedCharm);
                    updateWebNodeStyles();
                }
            });

            refundBtn.setOnAction(ev -> {
                if (selectedCharm != null) {
                    data.removeCharm(selectedCharm.getId());
                    updateSidebarButton(selectedCharm);
                    updateWebNodeStyles();
                }
            });

            deleteCustomBtn.setOnAction(ev -> {
                String selectedName = detailTitle.getText();
                Charm c = charms.stream().filter(ch -> ch.getName().equals(selectedName)).findFirst().orElse(null);
                if (c != null && c.isCustom()) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete the custom charm '" + c.getName() + "'?", ButtonType.YES, ButtonType.NO);
                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.YES) {
                            try {
                                dataService.deleteCustomCharm(c);
                                refreshCharms();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }
            });

            for (Charm c : charms) {
                VBox targetBox = charmNodeMap.get(c.getId());
                if (targetBox != null && c.getPrerequisites() != null) {
                    for (String reqId : c.getPrerequisites()) {
                        VBox sourceBox = charmNodeMap.get(reqId);
                        if (sourceBox != null) {
                            double startX = sourceBox.getLayoutX() + boxWidth / 2;
                            double startY = sourceBox.getLayoutY() + boxHeight;
                            double endX = targetBox.getLayoutX() + boxWidth / 2;
                            double endY = targetBox.getLayoutY();

                            CubicCurve curve = new CubicCurve(startX, startY, startX, startY + gapY / 1.5, endX, endY - gapY / 1.5, endX, endY);
                            curve.getStyleClass().add("charm-line");
                            charmCanvas.getChildren().add(curve);
                            curve.toBack();
                        }
                    }
                }
            }

            double minHeight = (maxDepth + 1) * (boxHeight + gapY) + 40;
            charmCanvas.setPrefSize(virtualCanvasWidth, minHeight);
            charmCanvas.setMinSize(virtualCanvasWidth, minHeight);
            updateWebNodeStyles();
        }

        private void updateSidebarButton(Charm c) {
            int count = data.getCharmCount(c.getId());
            boolean isOxBody = c.getName().equals("Ox-Body Technique");

            deleteCustomBtn.setVisible(c.isCustom());
            deleteCustomBtn.setManaged(c.isCustom());

            if (isOxBody) {
                int limit = data.getAbility("Resistance").get();
                toggleBtn.setText("Purchase (" + count + "/" + limit + ")");
                toggleBtn.setDisable(count >= limit || !c.isEligible(data));
                toggleBtn.setStyle("-fx-base: #d4af37;");
                refundBtn.setVisible(count > 0);
                refundBtn.setManaged(count > 0);
            } else if (data.hasCharm(c.getId())) {
                toggleBtn.setText("Refund Charm");
                toggleBtn.setDisable(false);
                toggleBtn.setStyle("-fx-base: #a03030;");
                refundBtn.setVisible(false);
                refundBtn.setManaged(false);
            } else if (c.isEligible(data)) {
                toggleBtn.setText("Purchase Charm");
                toggleBtn.setDisable(false);
                toggleBtn.setStyle("-fx-base: #d4af37;");
                refundBtn.setVisible(false);
                refundBtn.setManaged(false);
            } else {
                toggleBtn.setText("Requirements Not Met");
                toggleBtn.setDisable(true);
                toggleBtn.setStyle("-fx-base: #444444;");
                refundBtn.setVisible(false);
                refundBtn.setManaged(false);
            }
        }

        public void updateWebNodeStyles() {
            for (Charm c : currentCharms) {
                VBox box = charmNodeMap.get(c.getId());
                if (box != null) {
                    box.getStyleClass().removeAll("charm-node-unselected", "charm-node-selected", "charm-node-ineligible");
                    boolean bought = data.hasCharm(c.getId());
                    if (bought) {
                        box.getStyleClass().add("charm-node-selected");
                    } else if (c.isEligible(data)) {
                        box.getStyleClass().add("charm-node-unselected");
                    } else {
                        box.getStyleClass().add("charm-node-ineligible");
                    }
                    if (c.isCustom()) {
                        if (!box.getStyleClass().contains("charm-node-custom")) box.getStyleClass().add("charm-node-custom");
                    } else {
                        box.getStyleClass().remove("charm-node-custom");
                    }

                    if (c.isPotentiallyProblematicImport()) {
                        if (!box.getStyleClass().contains("charm-node-problematic")) box.getStyleClass().add("charm-node-problematic");
                    } else {
                        box.getStyleClass().remove("charm-node-problematic");
                    }
                }
            }
        }

        private void updateKeywords(List<String> keywords, FlowPane kwFlow) {
            kwFlow.getChildren().clear();
            if (keywords == null || keywords.isEmpty()) {
                return;
            }

            for (int i = 0; i < keywords.size(); i++) {
                String name = keywords.get(i).trim();
                if (name.isEmpty())
                    continue;

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

                if (i < keywords.size() - 1) {
                    Label comma = new Label(", ");
                    comma.getStyleClass().add("sidebar-stat");
                    kwFlow.getChildren().add(comma);
                }
            }
        }
    }



    private void showCreateCharmDialog(String defaultAbility, boolean martialArtsOnly) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(getScene().getWindow());
        dialog.setTitle("Create Custom Charm");

        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-pane");
        root.setStyle("-fx-background-color: #1e1e1e;");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);

        TextField nameField = new TextField();
        nameField.setPromptText("Charm Name");
        
        ComboBox<String> abCombo = new ComboBox<>();
        if (martialArtsOnly) {
            // Only add Martial Arts styles for the MA tab context
            for (MartialArtsStyle mas : data.getMartialArtsStyles()) {
                if (mas.getStyleName() != null && !mas.getStyleName().isEmpty() && !abCombo.getItems().contains(mas.getStyleName())) {
                    abCombo.getItems().add(mas.getStyleName());
                }
            }
        } else {
            // Regular abilities for the standard Charms tab
            abCombo.getItems().addAll(CharacterData.ABILITIES);
        }
        abCombo.setValue(defaultAbility != null ? defaultAbility : "Archery");

        Spinner<Integer> minAb = new Spinner<>(0, 5, 1);
        Spinner<Integer> minEss = new Spinner<>(1, 5, 1);
        
        TextField costField = new TextField();
        costField.setPromptText("e.g. 5m");
        
        TextField typeField = new TextField();
        typeField.setPromptText("e.g. Simple");
        
        TextField keywordsField = new TextField();
        keywordsField.setPromptText("e.g. Decisive-only, Mastery");
        
        TextField durationField = new TextField();
        durationField.setPromptText("e.g. Instant");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Full Charm Description...");
        descArea.setPrefRowCount(5);
        descArea.setWrapText(true);

        ListView<String> prereqList = new ListView<>();
        prereqList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        prereqList.setPrefHeight(100);

        Map<String, String> nameToId = new HashMap<>();
        Runnable updatePrereqs = () -> {
            String selectedAb = abCombo.getValue();
            List<Charm> charms = dataService.loadCharmsForAbility(selectedAb);
            nameToId.clear();
            for (Charm c : charms) nameToId.put(c.getName(), c.getId());
            prereqList.getItems().setAll(charms.stream().map(Charm::getName).collect(Collectors.toList()));
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
        grid.add(new Label("Prerequisites:"), 2, 0); grid.add(prereqList, 2, 1, 1, 7);

        VBox descBox = new VBox(5, new Label("Description:"), descArea);
        
        for (javafx.scene.Node n : grid.getChildren()) {
            if (n instanceof Label l) l.setStyle("-fx-text-fill: #f9f6e6;");
        }
        ((Label)descBox.getChildren().get(0)).setStyle("-fx-text-fill: #f9f6e6;");

        Button saveBtn = new Button("Save Charm");
        saveBtn.getStyleClass().add("action-btn");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> {
            if (nameField.getText().isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "Charm name cannot be empty.").showAndWait();
                return;
            }
            
            Charm nc = new Charm();
            nc.setName(nameField.getText());
            nc.setAbility(abCombo.getValue());
            nc.setMinAbility(minAb.getValue());
            nc.setMinEssence(minEss.getValue());
            nc.setCost(costField.getText());
            nc.setType(typeField.getText());
            
            List<String> kwList = new ArrayList<>();
            if (!keywordsField.getText().trim().isEmpty()) {
                for (String kw : keywordsField.getText().split(",")) {
                    String t = kw.trim();
                    if (!t.isEmpty() && !t.equalsIgnoreCase("None")) kwList.add(t);
                }
            }
            nc.setKeywords(kwList);
            nc.setDuration(durationField.getText());
            nc.setFullText(descArea.getText());
            List<String> selectedIds = prereqList.getSelectionModel().getSelectedItems().stream()
                .map(nameToId::get)
                .collect(Collectors.toList());
            nc.setPrerequisites(selectedIds);
            nc.setCustom(true);

            try {
                dataService.saveCharm(nc);
                data.setDirty(true);
                refreshCharms();
            } catch (IOException ex) {
                new Alert(Alert.AlertType.ERROR, "Failed to save custom charm: " + ex.getMessage()).showAndWait();
            }
        });

        root.getChildren().addAll(grid, descBox, saveBtn);
        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.show();
    }

    private void showEditCharmDialog(Charm charm) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Charm: " + charm.getName());
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
            List<Charm> charms = dataService.loadCharmsForAbility(selectedAb);
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
                    dataService.saveCharm(charm);
                    data.setDirty(true);
                    return bt;
                } catch (IOException ex) {
                    new Alert(Alert.AlertType.ERROR, "Failed to save charm: " + ex.getMessage()).showAndWait();
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(r -> refreshCharms());
    }
}
