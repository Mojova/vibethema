package com.vibethema.ui;

import com.vibethema.model.*;
import com.vibethema.model.mystic.*;
import com.vibethema.ui.charms.CharmsTab;
import com.vibethema.ui.charms.EditCharmView;
import com.vibethema.ui.equipment.DefaultEquipmentDialogService;
import com.vibethema.ui.equipment.EquipmentTab;
import com.vibethema.ui.experience.ExperienceTab;
import com.vibethema.ui.footer.FooterView;
import com.vibethema.ui.sorcery.SorceryTab;
import com.vibethema.viewmodel.*;
import com.vibethema.viewmodel.charms.CharmsViewModel;
import com.vibethema.viewmodel.charms.EditCharmViewModel;
import com.vibethema.viewmodel.equipment.EquipmentViewModel;
import com.vibethema.viewmodel.experience.ExperienceViewModel;
import com.vibethema.viewmodel.footer.FooterViewModel;
import com.vibethema.viewmodel.util.Messenger;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import de.saxsys.mvvmfx.ViewTuple;
import de.saxsys.mvvmfx.utils.notifications.NotificationObserver;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainView extends BorderPane implements JavaView<MainViewModel>, Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MainView.class);

    @InjectViewModel private MainViewModel viewModel;

    private TabPane mainTabPane;
    private Tab experienceTab;
    private Map<String, CharmsTab> charmTabs = new HashMap<>();
    private final List<NotificationObserver> observers = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        getStyleClass().add("main-pane");

        setTop(createTopSection());

        mainTabPane = new TabPane();
        mainTabPane.getStyleClass().add("tab-pane");

        // Tabs
        mainTabPane
                .getTabs()
                .addAll(
                        createTab(
                                "Stats",
                                StatsTab.class,
                                viewModel.getStatsViewModel()), // Eagerly load the first tab
                        createLazyTab(
                                "Merits",
                                () ->
                                        createTab(
                                                "Merits",
                                                MeritsTab.class,
                                                viewModel.getMeritsViewModel())),
                        createLazyTab(
                                "Intimacies",
                                () ->
                                        createTab(
                                                "Intimacies",
                                                IntimaciesTab.class,
                                                viewModel.getIntimaciesViewModel())),
                        createCharmsLazyTab("Solar Charms", "Ability"),
                        createCharmsLazyTab("Martial Arts", "Martial Arts Style"),
                        createLazyTab(
                                "Sorcery",
                                () ->
                                        createTab(
                                                "Sorcery",
                                                SorceryTab.class,
                                                viewModel.getSorceryViewModel())),
                        createLazyEquipmentTab());

        experienceTab = createLazyExperienceTab();
        viewModel
                .getData()
                .modeProperty()
                .addListener((obs, oldV, newV) -> updateExperienceTabVisibility(newV));
        updateExperienceTabVisibility(viewModel.getData().getMode());

        mainTabPane
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, oldTab, newTab) -> {
                            if (newTab != null) {
                                viewModel.currentTabIdProperty().set(newTab.getText());
                            }
                        });

        setCenter(mainTabPane);

        // Footer
        ViewTuple<FooterView, FooterViewModel> footerVt =
                de.saxsys.mvvmfx.FluentViewLoader.javaView(FooterView.class)
                        .viewModel(viewModel.getFooterViewModel())
                        .load();
        setBottom(footerVt.getView());

        // Window Title binding
        viewModel.windowTitleProperty().addListener((obs, oldV, newV) -> updateWindowTitle(newV));

        // UI Messenger subscriptions
        addObserver(
                "jump_to_charms",
                (name, payload) -> {
                    if (payload != null && payload.length > 0 && payload[0] instanceof String) {
                        handleJumpToCharms((String) payload[0]);
                    }
                });

        addObserver(
                "jump_to_evocations",
                (name, payload) -> {
                    if (payload != null
                            && payload.length > 1
                            && payload[0] instanceof String
                            && payload[1] instanceof String) {
                        handleJumpToEvocations((String) payload[0], (String) payload[1]);
                    }
                });

        addObserver("show_finalization_dialog", (name, payload) -> showFinalizationDialog());

        addObserver(
                "refresh_all_ui",
                (name, payload) -> {
                    charmTabs.values().forEach(CharmsTab::refresh);
                });

        addObserver(
                "highlight_element",
                (name, payload) -> {
                    if (payload != null && payload.length > 0 && payload[0] instanceof String) {
                        triggerPulseHighlight((String) payload[0]);
                    }
                });

        addObserver(
                "char_load_warning",
                (name, payload) -> {
                    if (payload != null
                            && payload.length > 0
                            && payload[0] instanceof java.util.List) {
                        @SuppressWarnings("unchecked")
                        java.util.List<String> missing = (java.util.List<String>) payload[0];
                        String details = String.join("\n• ", missing);
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Missing Equipment");
                        alert.setHeaderText("Some items were not found in the database:");
                        alert.setContentText(
                                "The following items are missing from your global equipment"
                                        + " library. Stats for these items will not be loaded:\n\n"
                                        + "• "
                                        + details);
                        alert.getDialogPane().setPrefWidth(500);
                        applyDialogStyle(alert);
                        alert.showAndWait();
                    }
                });

        addObserver(
                "confirm_discard_changes",
                (name, payload) -> {
                    String nextAction =
                            (payload != null && payload.length > 0) ? (String) payload[0] : "";
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Unsaved Changes");
                    alert.setHeaderText("Your character has unsaved changes.");
                    alert.setContentText("Would you like to save before continuing?");

                    ButtonType saveBtn = new ButtonType("Save");
                    ButtonType discardBtn = new ButtonType("Discard");
                    ButtonType cancelBtn =
                            new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                    alert.getButtonTypes().setAll(saveBtn, discardBtn, cancelBtn);
                    applyDialogStyle(alert);

                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent()) {
                        if (result.get() == saveBtn) {
                            viewModel.onSaveRequest();
                        } else if (result.get() == discardBtn) {
                            viewModel.resetToNew();
                            if ("LOAD".equals(nextAction)) {
                                viewModel.onLoadRequest();
                            }
                        }
                    }
                });

        addObserver(
                "request_save_as",
                (name, payload) -> {
                    String suggestName =
                            (payload != null && payload.length > 0)
                                    ? (String) payload[0]
                                    : "Character.vbtm";
                    System.out.println("DEBUG: request_save_as received filename: " + suggestName);
                    handleSaveAs(suggestName);
                });
        addObserver(
                "request_load_file",
                (name, payload) -> {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Load Character");
                    fileChooser
                            .getExtensionFilters()
                            .add(new FileChooser.ExtensionFilter("Vibethema Save File", "*.vbtm"));
                    File file = fileChooser.showOpenDialog(getScene().getWindow());
                    if (file != null) {
                        viewModel.loadCharacter(file);
                    }
                });
        addObserver(
                "request_pdf_save_location",
                (name, payload) -> {
                    String suggestName =
                            (payload != null && payload.length > 0)
                                    ? (String) payload[0]
                                    : "Character.pdf";
                    handleExportPdf(suggestName);
                });
        addObserver(
                "pdf_export_success",
                (name, payload) -> {
                    String msg = (payload != null && payload.length > 0) ? (String) payload[0] : "";
                    handleExportSuccess(msg);
                });
        addObserver(
                "pdf_export_error",
                (name, payload) -> {
                    String msg = (payload != null && payload.length > 0) ? (String) payload[0] : "";
                    handleExportError(msg);
                });

        addObserver(
                "open_edit_charm_dialog",
                (name, payload) -> {
                    if (payload != null && payload.length >= 4) {
                        showEditCharmDialog(
                                (Charm) payload[0],
                                (String) payload[1],
                                (String) payload[2],
                                (Runnable) payload[3]);
                    }
                });

        addObserver(
                "switch_to_tab",
                (name, payload) -> {
                    if (payload != null
                            && payload.length > 0
                            && payload[0] instanceof String tabTitle) {
                        for (Tab tab : mainTabPane.getTabs()) {
                            if (tabTitle.equals(tab.getText())) {
                                mainTabPane.getSelectionModel().select(tab);
                                break;
                            }
                        }
                    }
                });

        addObserver(
                "show_about_dialog",
                (name, payload) -> {
                    if (payload != null && payload.length >= 2) {
                        String title = (String) payload[0];
                        String msg = (String) payload[1];
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle(title);
                        alert.setHeaderText(null);

                        // Try to load and set the Dark Pack logo
                        try {
                            javafx.scene.image.Image logo =
                                    new javafx.scene.image.Image(
                                            getClass().getResourceAsStream("/darkpack_logo2.png"));
                            javafx.scene.image.ImageView imageView =
                                    new javafx.scene.image.ImageView(logo);
                            imageView.setFitWidth(150);
                            imageView.setPreserveRatio(true);
                            alert.setGraphic(imageView);
                        } catch (Exception e) {
                            // Suppress logo loading errors
                        }

                        alert.setContentText(msg);
                        applyDialogStyle(alert);
                        alert.showAndWait();
                    }
                });

        addObserver(
                "show_info_alert",
                (name, payload) -> {
                    if (payload != null && payload.length >= 2) {
                        String title = (String) payload[0];
                        String msg = (String) payload[1];
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
                        alert.setTitle(title);
                        alert.setHeaderText(title);
                        applyDialogStyle(alert);
                        alert.showAndWait();
                    }
                });

        addObserver(
                "show_error_alert",
                (name, payload) -> {
                    if (payload != null && payload.length > 0) {
                        String msg = (String) payload[0];
                        Alert alert = new Alert(Alert.AlertType.ERROR, msg);
                        alert.setTitle("Error");
                        applyDialogStyle(alert);
                        alert.showAndWait();
                    }
                });
    }

    private void addObserver(String name, NotificationObserver observer) {
        Messenger.subscribe(name, observer);
        observers.add(observer);
    }

    public void cleanup() {
        observers.forEach(obs -> Messenger.unsubscribe(null, obs));
        observers.clear();
    }

    private Tab createCharmsLazyTab(String title, String filterType) {
        return createLazyTab(
                title,
                () -> {
                    ViewTuple<CharmsTab, CharmsViewModel> vt =
                            de.saxsys.mvvmfx.FluentViewLoader.javaView(CharmsTab.class)
                                    .viewModel(viewModel.getCharmsViewModel(filterType))
                                    .load();
                    CharmsTab view = (CharmsTab) vt.getView();
                    charmTabs.put(title, view);
                    return new Tab(title, view);
                });
    }

    private Tab createLazyEquipmentTab() {
        return createLazyTab("Equipment", this::createEquipmentTab);
    }

    private Tab createLazyExperienceTab() {
        return createLazyTab("Experience", this::createExperienceTab);
    }

    private Tab createLazyTab(String title, java.util.function.Supplier<Tab> tabSupplier) {
        Tab lazyTab = new Tab(title);
        lazyTab.setClosable(false);
        lazyTab.selectedProperty()
                .addListener(
                        (obs, oldVal, newVal) -> {
                            if (newVal && lazyTab.getContent() == null) {
                                Tab loadedTab = tabSupplier.get();
                                lazyTab.setContent(loadedTab.getContent());
                            }
                        });
        return lazyTab;
    }

    private Tab createEquipmentTab() {
        ViewTuple<EquipmentTab, EquipmentViewModel> vt =
                de.saxsys.mvvmfx.FluentViewLoader.javaView(EquipmentTab.class)
                        .viewModel(viewModel.getEquipmentViewModel())
                        .codeBehind(
                                new EquipmentTab(
                                        new DefaultEquipmentDialogService(
                                                viewModel.getEquipmentService())))
                        .load();

        EquipmentTab view = (EquipmentTab) vt.getView();
        view.initialize();

        Tab tab = new Tab("Equipment", view);
        tab.setClosable(false);
        return tab;
    }

    private Tab createExperienceTab() {
        ViewTuple<ExperienceTab, ExperienceViewModel> vt =
                de.saxsys.mvvmfx.FluentViewLoader.javaView(ExperienceTab.class)
                        .viewModel(viewModel.getExperienceViewModel(() -> {}))
                        .load();

        ExperienceTab view = (ExperienceTab) vt.getView();
        view.initialize();

        Tab tab = new Tab("Experience", view);
        tab.setClosable(false);
        return tab;
    }

    private <V extends JavaView<VM>, VM extends de.saxsys.mvvmfx.ViewModel> Tab createTab(
            String title, Class<V> viewClass, VM vm) {
        ViewTuple<V, VM> vt =
                de.saxsys.mvvmfx.FluentViewLoader.javaView(viewClass).viewModel(vm).load();
        Tab tab = new Tab(title, vt.getView());
        tab.setClosable(false);
        return tab;
    }

    private void updateExperienceTabVisibility(CharacterMode mode) {
        if (mode == CharacterMode.EXPERIENCED) {
            if (!mainTabPane.getTabs().contains(experienceTab)) {
                mainTabPane.getTabs().add(experienceTab);
            }
        } else {
            mainTabPane.getTabs().remove(experienceTab);
        }
    }

    private VBox createTopSection() {
        VBox topContainer = new VBox();
        topContainer.getChildren().addAll(createMenuBar(), createHeader());
        return topContainer;
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.setUseSystemMenuBar(false);

        Menu fileMenu = new Menu("File");
        MenuItem newItem = new MenuItem("New Character");
        newItem.setAccelerator(KeyCombination.keyCombination("Shortcut+N"));
        newItem.setOnAction(e -> viewModel.onNewCharacterRequest());
        newItem.disableProperty().bind(viewModel.coreDataImportedProperty().not());

        MenuItem saveItem = new MenuItem("Save");
        saveItem.setAccelerator(KeyCombination.keyCombination("Shortcut+S"));
        saveItem.setOnAction(e -> viewModel.onSaveRequest());

        MenuItem loadItem = new MenuItem("Load Character");
        loadItem.setAccelerator(KeyCombination.keyCombination("Shortcut+O"));
        loadItem.setOnAction(e -> viewModel.onLoadRequest());
        loadItem.disableProperty().bind(viewModel.coreDataImportedProperty().not());

        MenuItem exportPdf = new MenuItem("Export to PDF...");
        exportPdf.setAccelerator(KeyCombination.keyCombination("Shortcut+E"));
        exportPdf.setOnAction(e -> viewModel.onExportPdfRequest());

        MenuItem quitItem = new MenuItem("Quit");
        quitItem.setOnAction(e -> Platform.exit());

        fileMenu.getItems()
                .addAll(
                        newItem,
                        new SeparatorMenuItem(),
                        saveItem,
                        loadItem,
                        new SeparatorMenuItem(),
                        exportPdf,
                        new SeparatorMenuItem(),
                        quitItem);
        menuBar.getMenus().add(fileMenu);

        Menu importMenu = new Menu("Import");
        MenuItem importCoreItem = new MenuItem("Import Core Book PDF...");
        importCoreItem.setOnAction(
                e -> PdfImportHelper.importCorePdf(getScene().getWindow(), viewModel::refreshImportStatus));

        MenuItem importMoseItem = new MenuItem("Import Miracles of the Solar Exalted...");
        importMoseItem.setOnAction(
                e ->
                        PdfImportHelper.importMosePdf(
                                getScene().getWindow(), viewModel::refreshImportStatus));

        importMenu.getItems().addAll(importCoreItem, importMoseItem);
        menuBar.getMenus().add(importMenu);

        Menu toolsMenu = new Menu("Tools");
        MenuItem statsItem = new MenuItem("Database Statistics");
        statsItem.setOnAction(e -> viewModel.showDatabaseStats());
        MenuItem openDirItem = new MenuItem("Open Application Data Directory");
        openDirItem.setOnAction(e -> viewModel.openDataDirectory());
        toolsMenu.getItems().addAll(statsItem, openDirItem);
        menuBar.getMenus().add(toolsMenu);

        Menu editMenu = new Menu("Edit");
        MenuItem undoItem = new MenuItem("Undo");
        undoItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Z"));
        undoItem.setOnAction(e -> viewModel.undo());
        undoItem.disableProperty().bind(viewModel.canUndoProperty().not());

        MenuItem redoItem = new MenuItem("Redo");
        redoItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Shift+Z"));
        redoItem.setOnAction(e -> viewModel.redo());
        redoItem.disableProperty().bind(viewModel.canRedoProperty().not());

        // Also permit Shortcut+Y for redo (traditional Windows/Linux)
        // We add it to the scene once it's available
        sceneProperty()
                .addListener(
                        (obs, oldS, newS) -> {
                            if (newS != null) {
                                newS.getAccelerators()
                                        .put(
                                                KeyCombination.keyCombination("Shortcut+Y"),
                                                () -> {
                                                    if (viewModel.canRedoProperty().get()) {
                                                        viewModel.redo();
                                                    }
                                                });
                            }
                        });

        editMenu.getItems().addAll(undoItem, redoItem);
        menuBar.getMenus().add(editMenu);

        Menu helpMenu = new Menu("Help");
        MenuItem checkDataItem = new MenuItem("Check for Missing Data");
        checkDataItem.setOnAction(e -> viewModel.checkMissingData());
        MenuItem aboutItem = new MenuItem("About Vibethema");
        aboutItem.setOnAction(e -> viewModel.showAboutDialog());
        helpMenu.getItems().addAll(checkDataItem, new SeparatorMenuItem(), aboutItem);
        menuBar.getMenus().add(helpMenu);

        return menuBar;
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.getStyleClass().add("header");

        Text titleText = new Text("Exalted 3rd Edition Solar Builder");
        titleText.getStyleClass().add("title-text");

        HBox basicInfo = new HBox(15);
        basicInfo.getStyleClass().add("info-bar");

        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField();
        nameField.setId("info_name");
        nameField.setPromptText("Character Name");
        nameField.textProperty().bindBidirectional(viewModel.getData().nameProperty());
        nameField.setAccessibleText("Character Name");
        nameLabel.setLabelFor(nameField);

        Label casteLabel = new Label("Caste:");
        ComboBox<Caste> casteBox = new ComboBox<>();
        casteBox.setId("info_caste");
        casteBox.getItems().addAll(Caste.values());
        casteBox.setValue(viewModel.getData().casteProperty().get());
        casteBox.setAccessibleText("Character Caste");
        casteLabel.setLabelFor(casteBox);
        casteBox.focusedProperty()
                .addListener(
                        (obs, oldV, newV) -> {
                            if (!newV) { // Focus lost (blur)
                                handleCasteChange(casteBox);
                            }
                        });

        // Keep UI in sync with Model (for loads/imports)
        viewModel
                .getData()
                .casteProperty()
                .addListener(
                        (obs, oldV, newV) -> {
                            if (newV != casteBox.getValue()) {
                                casteBox.setValue(newV);
                            }
                        });

        ComboBox<String> supernalDropdown = new ComboBox<>();
        supernalDropdown.setId("info_supernal");
        supernalDropdown.setPrefWidth(120);
        supernalDropdown.setItems(viewModel.getData().getValidSupernalAbilities());
        supernalDropdown
                .valueProperty()
                .bindBidirectional(viewModel.getData().supernalAbilityProperty());
        supernalDropdown
                .valueProperty()
                .addListener((obs, oldV, newV) -> Messenger.publish("refresh_all_ui"));
        supernalDropdown.setAccessibleText("Supernal Ability");

        Label supernalLabel = new Label("Supernal:");
        supernalLabel.setLabelFor(supernalDropdown);
        Button helpBtn = new Button("?");
        helpBtn.getStyleClass().add("help-btn");
        Tooltip helpTooltip =
                new Tooltip(
                        "Only abilities selected as 'Caste' in the Stats tab can be chosen as your"
                                + " Supernal ability.");
        helpBtn.setTooltip(helpTooltip);

        // Show help on focus (for keyboard navigation)
        helpBtn.focusedProperty()
                .addListener(
                        (obs, oldV, newV) -> {
                            if (newV) {
                                Bounds bounds = helpBtn.localToScreen(helpBtn.getBoundsInLocal());
                                helpTooltip.show(helpBtn, bounds.getMinX(), bounds.getMaxY() + 5);
                            } else {
                                helpTooltip.hide();
                            }
                        });

        HBox supernalContainer = new HBox(5);
        supernalContainer.setAlignment(Pos.CENTER_LEFT);
        supernalContainer.getChildren().addAll(supernalDropdown, helpBtn);

        basicInfo
                .getChildren()
                .addAll(
                        nameLabel,
                        nameField,
                        casteLabel,
                        casteBox,
                        supernalLabel,
                        supernalContainer);

        header.getChildren().addAll(titleText, basicInfo);
        return header;
    }

    private void handleCasteChange(ComboBox<Caste> combo) {
        Caste newCaste = combo.getValue();
        Caste oldCaste = viewModel.getData().casteProperty().get();

        if (newCaste == oldCaste) return;

        MainViewModel.CasteChangeReport report = viewModel.validateCasteChange(newCaste);

        if (report.isEmpty()) {
            // Safe to apply
            viewModel.applyCasteChange(newCaste, report);
            return;
        }

        // Show Warning
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Caste Change Warning");
        alert.setHeaderText("Caste Change");
        alert.setContentText(viewModel.generateCasteChangeWarning(newCaste, report));
        alert.getDialogPane().setPrefWidth(500);

        // Style the dialog
        applyDialogStyle(alert);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            viewModel.applyCasteChange(newCaste, report);
        } else {
            // Revert ComboBox
            combo.setValue(oldCaste);
        }
    }

    private void handleSaveAs(String suggestName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Character");
        fileChooser
                .getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Vibethema Save File", "*.vbtm"));
        fileChooser.setInitialFileName(suggestName);
        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            viewModel.saveCharacter(file);
        }
    }

    private void handleExportPdf(String suggestName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export to PDF");
        fileChooser
                .getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName(suggestName);

        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            viewModel.exportToPdf(file);
        }
    }

    private void handleExportSuccess(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
        alert.setTitle(MainViewModel.EXPORT_SUCCESS_TITLE);
        applyDialogStyle(alert);
        alert.showAndWait();
    }

    private void handleExportError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg);
        alert.setTitle(MainViewModel.EXPORT_FAILURE_TITLE);
        applyDialogStyle(alert);
        alert.showAndWait();
    }

    public boolean confirmDiscardChanges() {
        if (!viewModel.dirtyProperty().get()) return true;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("Your character has unsaved changes.");
        alert.setContentText("Would you like to save before continuing?");

        ButtonType saveBtn = new ButtonType("Save");
        ButtonType discardBtn = new ButtonType("Discard");
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(saveBtn, discardBtn, cancelBtn);
        applyDialogStyle(alert);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == saveBtn) {
                viewModel.onSaveRequest();
                return false; // Action is handled asynchronously or as part of save flow
            } else if (result.get() == discardBtn) {
                return true;
            }
        }
        return false;
    }

    public void showFinalizationDialog() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(MainViewModel.FINALIZATION_DIALOG_TITLE);
        confirm.setHeaderText(MainViewModel.FINALIZATION_DIALOG_HEADER);
        confirm.setContentText(MainViewModel.FINALIZATION_DIALOG_CONTENT);

        applyDialogStyle(confirm);
        confirm.showAndWait()
                .ifPresent(
                        response -> {
                            if (response == ButtonType.OK) {
                                viewModel.proceedWithFinalization();
                            }
                        });
    }

    private void showEditCharmDialog(
            Charm charm, String contextName, String filterType, Runnable onSave) {
        EditCharmViewModel editViewModel =
                new EditCharmViewModel(
                        charm, viewModel.getCharmDataService(), contextName, filterType, onSave);
        ViewTuple<EditCharmView, EditCharmViewModel> viewTuple =
                de.saxsys.mvvmfx.FluentViewLoader.javaView(EditCharmView.class)
                        .viewModel(editViewModel)
                        .load();

        Dialog<ButtonType> dialog = new Dialog<>();
        String term =
                "Evocation".equals(filterType) || "evocation".equals(charm.getCategory())
                        ? "Evocation"
                        : "Charm";
        dialog.setTitle("Edit " + term + ": " + editViewModel.getCharmName());

        applyDialogStyle(dialog);
        dialog.getDialogPane().setContent(viewTuple.getView());

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        dialog.setResultConverter(
                bt -> {
                    if (bt == saveType) {
                        try {
                            editViewModel.save();
                        } catch (IOException ex) {
                            new Alert(Alert.AlertType.ERROR, "Failed to save: " + ex.getMessage())
                                    .showAndWait();
                        }
                    }
                    return bt;
                });

        dialog.showAndWait();
    }

    private void handleJumpToCharms(String abilityName) {
        MainViewModel.NavigationTarget target = viewModel.resolveNavigationTarget(abilityName);
        performNavigation(target);
    }

    private void handleJumpToEvocations(String artifactId, String artifactName) {
        MainViewModel.NavigationTarget target = viewModel.resolveEvocationTarget(artifactName);
        performNavigation(target);
    }

    private void performNavigation(MainViewModel.NavigationTarget target) {
        CharmsViewModel cvm = viewModel.getCharmsViewModel(target.filterValue());
        if (cvm != null) {
            cvm.selectedFilterValueProperty().set(target.filterValue());
            for (Tab tab : mainTabPane.getTabs()) {
                if (target.tabName().equals(tab.getText())) {
                    mainTabPane.getSelectionModel().select(tab);
                    break;
                }
            }
        }
    }

    private void updateWindowTitle(String title) {
        if (getScene() != null && getScene().getWindow() instanceof Stage stage) {
            stage.setTitle(title);
        }
    }

    private void applyDialogStyle(Dialog<?> dialog) {
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog-pane-custom");

        if (dialog instanceof Alert alert) {
            if (alert.getAlertType() == Alert.AlertType.WARNING
                    || alert.getAlertType() == Alert.AlertType.ERROR) {
                dialogPane.getStyleClass().add("warning-dialog");
            }
        }
    }

    private void triggerPulseHighlight(String targetId) {
        if (targetId == null || targetId.isEmpty()) return;

        javafx.application.Platform.runLater(
                () -> {
                    Node node = lookup("#" + targetId);
                    if (node != null) {
                        ensureVisible(node);
                        node.requestFocus();
                        node.getStyleClass().add("pulse-highlight");

                        javafx.animation.PauseTransition pause =
                                new javafx.animation.PauseTransition(
                                        javafx.util.Duration.millis(800));
                        pause.setOnFinished(e -> node.getStyleClass().remove("pulse-highlight"));
                        pause.play();
                    } else {
                        logger.warn("Could not find element to highlight: #{}", targetId);
                    }
                });
    }

    private void ensureVisible(Node node) {
        Node current = node;
        while (current != null) {
            if (current instanceof ScrollPane scrollPane) {
                Node content = scrollPane.getContent();
                if (content == null) return;

                Bounds nodeInScene = node.localToScene(node.getBoundsInLocal());
                Bounds contentInScene = content.localToScene(content.getBoundsInLocal());

                double height = content.getBoundsInLocal().getHeight();
                double viewportHeight = scrollPane.getViewportBounds().getHeight();

                if (height > viewportHeight) {
                    double yInContent = nodeInScene.getMinY() - contentInScene.getMinY();
                    // Scroll so the node is near the top (with a 20px padding)
                    double targetV = (yInContent - 20) / (height - viewportHeight);
                    scrollPane.setVvalue(Math.max(0, Math.min(1, targetV)));
                }
                break;
            }
            current = current.getParent();
        }
    }
}
