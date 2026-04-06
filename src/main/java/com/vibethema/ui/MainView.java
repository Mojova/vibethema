package com.vibethema.ui;

import com.vibethema.model.*;
import com.vibethema.ui.charms.CharmTreeComponent;
import com.vibethema.ui.equipment.DefaultEquipmentDialogService;
import com.vibethema.ui.equipment.EquipmentTab;
import com.vibethema.ui.experience.ExperienceTab;
import com.vibethema.ui.footer.FooterView;
import com.vibethema.ui.sorcery.SorceryTab;
import com.vibethema.viewmodel.*;
import com.vibethema.viewmodel.equipment.EquipmentViewModel;
import com.vibethema.viewmodel.experience.ExperienceViewModel;
import com.vibethema.viewmodel.footer.FooterViewModel;
import com.vibethema.viewmodel.util.Messenger;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import de.saxsys.mvvmfx.ViewTuple;
import de.saxsys.mvvmfx.utils.notifications.NotificationObserver;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class MainView extends BorderPane implements JavaView<MainViewModel>, Initializable {

    @InjectViewModel
    private MainViewModel viewModel;

    private TabPane mainTabPane;
    private Tab experienceTab;
    private Map<String, CharmsTab> charmTabs = new HashMap<>();
    private Map<String, CharmsViewModel> charmViewModels = new HashMap<>();
    private final List<NotificationObserver> observers = new ArrayList<>();

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
            charmTabs.values().forEach(CharmsTab::refresh);
        }
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        getStyleClass().add("main-pane");

        setTop(createTopSection());
        
        mainTabPane = new TabPane();
        mainTabPane.getStyleClass().add("tab-pane");
        
        // Tabs
        mainTabPane.getTabs().addAll(
                createTab("Stats", StatsTab.class, new StatsViewModel(viewModel.getData())),
                createTab("Merits", MeritsTab.class, new MeritsViewModel(viewModel.getData())),
                createTab("Intimacies", IntimaciesTab.class, new IntimaciesViewModel(viewModel.getData())),
                createCharmsTab("Solar Charms", "Ability"),
                createCharmsTab("Martial Arts", "Martial Arts Style"),
                createTab("Sorcery", SorceryTab.class, new SorceryViewModel(viewModel.getData(), viewModel.getCharmDataService())),
                createEquipmentTab()
        );

        experienceTab = createExperienceTab();
        viewModel.getData().modeProperty().addListener((obs, oldV, newV) -> updateExperienceTabVisibility(newV));
        updateExperienceTabVisibility(viewModel.getData().getMode());

        setCenter(mainTabPane);

        // Footer
        ViewTuple<FooterView, FooterViewModel> footerVt = de.saxsys.mvvmfx.FluentViewLoader
                .javaView(FooterView.class)
                .viewModel(viewModel.getFooterViewModel())
                .load();
        setBottom(footerVt.getView());
        
        // Window Title binding
        viewModel.windowTitleProperty().addListener((obs, oldV, newV) -> updateWindowTitle(newV));

        // UI Messenger subscriptions
        addObserver("jump_to_charms", (name, payload) -> {
            if (payload != null && payload.length > 0 && payload[0] instanceof String) {
                handleJumpToCharms((String) payload[0]);
            }
        });

        addObserver("jump_to_evocations", (name, payload) -> {
            if (payload != null && payload.length > 1 && payload[0] instanceof String && payload[1] instanceof String) {
                handleJumpToEvocations((String) payload[0], (String) payload[1]);
            }
        });

        addObserver("show_finalization_dialog", (name, payload) -> showFinalizationDialog());

        addObserver("refresh_all_ui", (name, payload) -> {
            charmTabs.values().forEach(CharmsTab::refresh);
        });

        addObserver("char_load_warning", (name, payload) -> {
            if (payload != null && payload.length > 0 && payload[0] instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<String> missing = (java.util.List<String>) payload[0];
                String details = String.join("\n• ", missing);
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Missing Equipment");
                alert.setHeaderText("Some items were not found in the database:");
                alert.setContentText("The following items are missing from your global equipment library. Stats for these items will not be loaded:\n\n• " + details);
                alert.getDialogPane().setPrefWidth(500);
                alert.showAndWait();
            }
        });

        addObserver("confirm_discard_changes", (name, payload) -> {
            String nextAction = (payload != null && payload.length > 0) ? (String) payload[0] : "";
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Unsaved Changes");
            alert.setHeaderText("Your character has unsaved changes.");
            alert.setContentText("Would you like to save before continuing?");

            ButtonType saveBtn = new ButtonType("Save");
            ButtonType discardBtn = new ButtonType("Discard");
            ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(saveBtn, discardBtn, cancelBtn);

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

        addObserver("request_save_as", (name, payload) -> {
            String suggestName = (payload != null && payload.length > 0) ? (String) payload[0] : "Character.vbtm";
            System.out.println("DEBUG: request_save_as received filename: " + suggestName);
            handleSaveAs(suggestName);
        });
        addObserver("request_load_file", (name, payload) -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Load Character");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Vibethema Save File", "*.vbtm"));
            File file = fileChooser.showOpenDialog(getScene().getWindow());
            if (file != null) {
                viewModel.loadCharacter(file);
            }
        });
        addObserver("request_pdf_export", (name, payload) -> {
            String suggestName = (payload != null && payload.length > 0) ? (String) payload[0] : "Character.pdf";
            handleExportPdf(suggestName);
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

    private Tab createCharmsTab(String title, String filterType) {
        CharmsViewModel cvm = new CharmsViewModel(viewModel.getData(), viewModel.getCharmDataService(), viewModel.getKeywordDefs(), filterType);
        ViewTuple<CharmsTab, CharmsViewModel> vt = de.saxsys.mvvmfx.FluentViewLoader
                .javaView(CharmsTab.class)
                .viewModel(cvm)
                .codeBehind(new CharmsTab(this.charmTreeListener))
                .load();
        
        CharmsTab view = (CharmsTab) vt.getView();
        charmTabs.put(title, view);
        charmViewModels.put(title, cvm);
        
        Tab tab = new Tab(title, view);
        tab.setClosable(false);
        return tab;
    }

    private Tab createEquipmentTab() {
        EquipmentViewModel eqVm = new EquipmentViewModel(
                viewModel.getData(), 
                viewModel.getEquipmentService(), 
                viewModel.getCharmDataService(), 
                viewModel.getTagDescriptions(),
                null);
        
        ViewTuple<EquipmentTab, EquipmentViewModel> vt = de.saxsys.mvvmfx.FluentViewLoader
                .javaView(EquipmentTab.class)
                .viewModel(eqVm)
                .codeBehind(new EquipmentTab(new DefaultEquipmentDialogService(viewModel.getEquipmentService())))
                .load();
        
        EquipmentTab view = (EquipmentTab) vt.getView();
        view.initialize();
        
        Tab tab = new Tab("Equipment", view);
        tab.setClosable(false);
        return tab;
    }

    private Tab createExperienceTab() {
        ExperienceViewModel expVm = new ExperienceViewModel(viewModel.getData(), () -> {});
        
        ViewTuple<ExperienceTab, ExperienceViewModel> vt = de.saxsys.mvvmfx.FluentViewLoader
                .javaView(ExperienceTab.class)
                .viewModel(expVm)
                .load();
        
        ExperienceTab view = (ExperienceTab) vt.getView();
        view.initialize();
        
        Tab tab = new Tab("Experience", view);
        tab.setClosable(false);
        return tab;
    }

    private <V extends JavaView<VM>, VM extends de.saxsys.mvvmfx.ViewModel> Tab createTab(String title, Class<V> viewClass, VM vm) {
        ViewTuple<V, VM> vt = de.saxsys.mvvmfx.FluentViewLoader.javaView(viewClass).viewModel(vm).load();
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
        
        MenuItem saveItem = new MenuItem("Save");
        saveItem.setAccelerator(KeyCombination.keyCombination("Shortcut+S"));
        saveItem.setOnAction(e -> viewModel.onSaveRequest());
        
        MenuItem loadItem = new MenuItem("Load Character");
        loadItem.setAccelerator(KeyCombination.keyCombination("Shortcut+O"));
        loadItem.setOnAction(e -> viewModel.onLoadRequest());

        MenuItem exportPdf = new MenuItem("Export to PDF...");
        exportPdf.setAccelerator(KeyCombination.keyCombination("Shortcut+E"));
        exportPdf.setOnAction(e -> viewModel.onExportPdfRequest());

        MenuItem quitItem = new MenuItem("Quit");
        quitItem.setOnAction(e -> Platform.exit());

        fileMenu.getItems().addAll(newItem, new SeparatorMenuItem(), saveItem, loadItem, new SeparatorMenuItem(), exportPdf, new SeparatorMenuItem(), quitItem);
        menuBar.getMenus().add(fileMenu);
        
        return menuBar;
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.getStyleClass().add("header");

        Text titleText = new Text("Exalted 3rd Edition Solar Builder");
        titleText.getStyleClass().add("title-text");

        HBox basicInfo = new HBox(15);
        basicInfo.getStyleClass().add("info-bar");

        TextField nameField = new TextField();
        nameField.setPromptText("Character Name");
        nameField.textProperty().bindBidirectional(viewModel.getData().nameProperty());

        ComboBox<Caste> casteBox = new ComboBox<>();
        casteBox.getItems().addAll(Caste.values());
        casteBox.valueProperty().bindBidirectional(viewModel.getData().casteProperty());
        casteBox.valueProperty().addListener((obs, oldV, newV) -> Messenger.publish("refresh_all_ui"));

        ComboBox<String> supernalDropdown = new ComboBox<>();
        supernalDropdown.setPrefWidth(120);
        supernalDropdown.setItems(viewModel.getData().getValidSupernalAbilities());
        supernalDropdown.valueProperty().bindBidirectional(viewModel.getData().supernalAbilityProperty());
        supernalDropdown.valueProperty().addListener((obs, oldV, newV) -> Messenger.publish("refresh_all_ui"));

        basicInfo.getChildren().addAll(new Label("Name:"), nameField, new Label("Caste:"), casteBox,
                new Label("Supernal:"), supernalDropdown);

        header.getChildren().addAll(titleText, basicInfo);
        return header;
    }

    private void handleSaveAs(String suggestName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Character");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Vibethema Save File", "*.vbtm"));
        fileChooser.setInitialFileName(suggestName);
        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            viewModel.saveCharacter(file);
        }
    }

    private void handleExportPdf(String suggestName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export to PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName(suggestName);
        
        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try {
                new com.vibethema.service.PdfExportService().exportToPdf(viewModel.getData(), file);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Character sheet exported successfully!");
                alert.showAndWait();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage()).showAndWait();
            }
        }
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
        confirm.setTitle("Finalize Character");
        confirm.setHeaderText("Finalizing Character Creation");
        confirm.setContentText(
                "Once finalized, your Caste, Supernal, and Caste/Favored abilities cannot be changed. This is a one-way process.\n\nProceed to Experienced mode?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                viewModel.proceedWithFinalization();
            }
        });
    }

    private void showCreateCharmDialog(String contextId, String contextName, String filterType, Runnable onSave) {
        Charm charm = "Martial Arts Style".equals(filterType) ? new MartialArtsCharm() : new SolarCharm();
        charm.setId(UUID.randomUUID().toString());
        charm.setName("New " + ("Evocation".equals(filterType) ? "Evocation" : "Charm"));
        charm.setAbility(contextId);
        charm.setCategory("Evocation".equals(filterType) ? "evocation" : "charm");
        charm.setCustom(true);
        showEditCharmDialog(charm, contextName, filterType, onSave);
    }

    private void showEditCharmDialog(Charm charm, String contextName, String filterType, Runnable onSave) {
        // Implementation ported from BuilderUI
        Dialog<ButtonType> dialog = new Dialog<>();
        String term = "Evocation".equals(filterType) || "evocation".equals(charm.getCategory()) ? "Evocation" : "Charm";
        dialog.setTitle("Edit " + term + ": " + charm.getName());
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane-custom");

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(charm.getName());
        ComboBox<String> abCombo = new ComboBox<>();
        abCombo.getItems().addAll(SystemData.ABILITIES.stream().map(Ability::getDisplayName).collect(Collectors.toList()));
        abCombo.getItems().addAll(viewModel.getCharmDataService().getAvailableMartialArtsStyles());
        abCombo.setValue(charm.getAbility());

        Spinner<Integer> minAb = new Spinner<>(0, 5, charm.getMinAbility());
        Spinner<Integer> minEss = new Spinner<>(1, 5, charm.getMinEssence());
        TextField costField = new TextField(charm.getCost());
        TextField typeField = new TextField(charm.getType());
        TextField durationField = new TextField(charm.getDuration());
        TextArea descArea = new TextArea(charm.getFullText());
        descArea.setWrapText(true);

        grid.add(new Label("Name:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Ability:"), 0, 1); grid.add(abCombo, 1, 1);
        grid.add(new Label("Min Ability:"), 0, 2); grid.add(minAb, 1, 2);
        grid.add(new Label("Min Essence:"), 0, 3); grid.add(minEss, 1, 3);
        grid.add(new Label("Cost:"), 0, 4); grid.add(costField, 1, 4);
        grid.add(new Label("Type:"), 0, 5); grid.add(typeField, 1, 5);
        grid.add(new Label("Duration:"), 0, 6); grid.add(durationField, 1, 6);

        dialogPane.setContent(grid);
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == saveType) {
                charm.setName(nameField.getText());
                charm.setAbility(abCombo.getValue());
                charm.setMinAbility(minAb.getValue());
                charm.setMinEssence(minEss.getValue());
                charm.setCost(costField.getText());
                charm.setType(typeField.getText());
                charm.setDuration(durationField.getText());
                charm.setFullText(descArea.getText());

                try {
                    if ("evocation".equals(charm.getCategory())) {
                        viewModel.getCharmDataService().saveEvocation(charm.getAbility(), contextName, charm);
                    } else {
                        viewModel.getCharmDataService().saveCharm(charm);
                    }
                    if (onSave != null) onSave.run();
                } catch (IOException ex) {
                    new Alert(Alert.AlertType.ERROR, "Failed to save: " + ex.getMessage()).showAndWait();
                }
            }
            return bt;
        });

        dialog.showAndWait();
    }

    private void handleJumpToCharms(String abilityName) {
        String tabTitle = "Solar Charms";
        if (viewModel.getData().isMartialArtsStyle(abilityName)) {
            tabTitle = "Martial Arts";
        }

        CharmsViewModel cvm = charmViewModels.get(tabTitle);
        if (cvm != null) {
            cvm.selectedFilterValueProperty().set(abilityName);
            for (Tab tab : mainTabPane.getTabs()) {
                if (tabTitle.equals(tab.getText())) {
                    mainTabPane.getSelectionModel().select(tab);
                    break;
                }
            }
        }
    }

    private void handleJumpToEvocations(String artifactId, String artifactName) {
        // Logic to switch to Charms tab in Evocation mode
    }

    private void updateWindowTitle(String title) {
        if (getScene() != null && getScene().getWindow() instanceof Stage stage) {
            stage.setTitle(title);
        }
    }
}
