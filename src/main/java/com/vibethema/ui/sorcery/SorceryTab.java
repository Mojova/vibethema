package com.vibethema.ui.sorcery;

import com.vibethema.model.*;
import com.vibethema.model.mystic.*;
import com.vibethema.ui.util.UIUtils;
import com.vibethema.viewmodel.SorceryViewModel;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.collections.ListChangeListener;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SorceryTab extends ScrollPane implements JavaView<SorceryViewModel>, Initializable {

    @InjectViewModel private SorceryViewModel viewModel;

    private Label sorceryWarningLabel;
    private VBox sorceryMainContent;
    private VBox shapingRitualsListContainer;
    private VBox spellsListContainer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setFitToWidth(true);
        getStyleClass().add("scroll-pane-custom");

        VBox content = new VBox(20);
        content.getStyleClass().add("content-area");
        content.setPadding(new Insets(20));

        sorceryWarningLabel =
                new Label("Purchase Occult charm Terrestrial Circle Sorcery to enable sorcery.");
        sorceryWarningLabel.getStyleClass().add("problematic-warning");
        sorceryWarningLabel.setMaxWidth(Double.MAX_VALUE);
        sorceryWarningLabel.setAlignment(Pos.CENTER);
        sorceryWarningLabel.visibleProperty().bind(viewModel.sorceryEnabledProperty().not());
        sorceryWarningLabel.managedProperty().bind(sorceryWarningLabel.visibleProperty());

        sorceryMainContent = new VBox(25);
        sorceryMainContent.visibleProperty().bind(viewModel.sorceryEnabledProperty());
        sorceryMainContent.managedProperty().bind(sorceryMainContent.visibleProperty());

        // Shaping Rituals
        VBox shapingSection = UIUtils.createSection("Shaping Rituals");
        shapingRitualsListContainer = new VBox(10);
        shapingRitualsListContainer.getStyleClass().add("merit-row-container");

        Button addRitualBtn = new Button("+ Add Shaping Ritual");
        addRitualBtn.getStyleClass().addAll("add-btn", "action-btn");
        addRitualBtn.setOnAction(e -> viewModel.addShapingRitual());
        shapingSection.getChildren().addAll(shapingRitualsListContainer, addRitualBtn);

        // Spells
        VBox spellsSection = UIUtils.createSection("Spells");
        spellsListContainer = new VBox(10);
        spellsListContainer.getStyleClass().add("merit-row-container");

        Button addSpellBtn = new Button("+ Add Spell");
        addSpellBtn.getStyleClass().addAll("add-btn", "action-btn");
        addSpellBtn.setOnAction(e -> showSpellSelectionDialog());
        spellsSection.getChildren().addAll(spellsListContainer, addSpellBtn);

        sorceryMainContent.getChildren().addAll(shapingSection, spellsSection);
        content.getChildren().addAll(sorceryWarningLabel, sorceryMainContent);

        refreshShapingRituals();
        viewModel
                .getShapingRituals()
                .addListener((ListChangeListener<ShapingRitual>) c -> refreshShapingRituals());

        refreshSpellsList();
        viewModel.getSpells().addListener((ListChangeListener<Spell>) c -> refreshSpellsList());

        setContent(content);
    }

    private void refreshShapingRituals() {
        if (shapingRitualsListContainer == null) return;
        shapingRitualsListContainer.getChildren().clear();

        for (ShapingRitual r : viewModel.getShapingRituals()) {
            VBox rowContainer = new VBox(5);
            rowContainer.getStyleClass().add("merit-row");
            rowContainer.setPadding(new Insets(10));

            HBox header = new HBox(15);
            header.setAlignment(Pos.CENTER_LEFT);

            TextField nameField = new TextField();
            nameField.textProperty().bindBidirectional(r.nameProperty());
            nameField.setPromptText("Ritual Name (e.g. Soul-Perfecting Method)");
            HBox.setHgrow(nameField, Priority.ALWAYS);

            Button delBtn = new Button("✕");
            delBtn.getStyleClass().add("remove-btn");
            delBtn.setOnAction(e -> viewModel.removeShapingRitual(r));

            header.getChildren().addAll(nameField, delBtn);

            TextArea descArea = new TextArea();
            descArea.textProperty().bindBidirectional(r.descriptionProperty());
            descArea.setPromptText("Describe the shaping ritual rules...");
            descArea.setPrefRowCount(3);
            descArea.setWrapText(true);

            rowContainer.getChildren().addAll(header, descArea);
            shapingRitualsListContainer.getChildren().add(rowContainer);
        }
    }

    private void refreshSpellsList() {
        if (spellsListContainer == null) return;
        spellsListContainer.getChildren().clear();

        for (Spell s : viewModel.getSpells()) {
            VBox row = new VBox(5);
            row.getStyleClass().add("merit-row");
            row.setPadding(new Insets(10));

            HBox header = new HBox(15);
            header.setAlignment(Pos.CENTER_LEFT);

            Label title = new Label(s.getName());
            title.getStyleClass().add("merit-name");
            HBox.setHgrow(title, Priority.ALWAYS);

            Label circleLabel = new Label(s.getCircle());
            circleLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #d4af37;");

            Button delBtn = new Button("✕");
            delBtn.getStyleClass().add("remove-btn");
            delBtn.setOnAction(e -> viewModel.removeSpell(s));

            header.getChildren().addAll(title, circleLabel, delBtn);

            GridPane details = new GridPane();
            details.setHgap(20);
            details.setVgap(5);
            details.add(new Label("Cost: " + s.getCost()), 0, 0);
            details.add(new Label("Duration: " + s.getDuration()), 1, 0);

            if (!s.getKeywords().isEmpty()) {
                details.add(
                        new Label("Keywords: " + String.join(", ", s.getKeywords())), 0, 1, 2, 1);
            }

            Label desc = new Label(s.getDescription());
            desc.setWrapText(true);
            desc.setMaxWidth(700);
            desc.getStyleClass().add("spell-desc");

            row.getChildren().addAll(header, details, desc);
            spellsListContainer.getChildren().add(row);
        }
    }

    private void showSpellSelectionDialog() {
        // Implementation logic imported from previous version
        // Using Stage, initModality, etc.
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(getScene().getWindow());
        stage.setTitle("Select Spells");
        stage.setMinWidth(600);
        stage.setMinHeight(500);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.getStyleClass().add("main-pane");
        layout.setStyle("-fx-background-color: #1a1a1a;");

        TextField filterField = new TextField();
        filterField.setPromptText("Filter spells...");

        TabPane circlesTabPane = new TabPane();
        circlesTabPane.getStyleClass().add("tab-pane");

        Button addBtn = new Button("Add Selected Spell");
        addBtn.getStyleClass().add("action-btn");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setDisable(true);

        String[] circlesArr = {"TERRESTRIAL", "CELESTIAL", "SOLAR"};
        for (String circle : circlesArr) {
            Tab tab = new Tab(circle.substring(0, 1) + circle.substring(1).toLowerCase());
            tab.setClosable(false);

            boolean eligible = true;
            if (circle.equals("CELESTIAL")
                    && !viewModel.getData().hasCharmByName(SystemData.CELESTIAL_CIRCLE_SORCERY))
                eligible = false;
            if (circle.equals("SOLAR")
                    && !viewModel.getData().hasCharmByName(SystemData.SOLAR_CIRCLE_SORCERY))
                eligible = false;

            if (!eligible) {
                tab.setDisable(true);
                tab.setTooltip(
                        new Tooltip(
                                "Missing "
                                        + (circle.equals("CELESTIAL") ? "Celestial" : "Solar")
                                        + " Circle Sorcery charm"));
            }

            ListView<Spell> listView = new ListView<>();
            List<Spell> availableSpells = viewModel.getCharmDataService().loadSpells(circle);
            listView.getItems().addAll(availableSpells);

            listView.setCellFactory(
                    lv ->
                            new ListCell<Spell>() {
                                @Override
                                protected void updateItem(Spell item, boolean empty) {
                                    super.updateItem(item, empty);
                                    if (empty || item == null) {
                                        setText(null);
                                        setGraphic(null);
                                    } else {
                                        VBox box = new VBox(2);
                                        Label name =
                                                new Label(
                                                        item.getName()
                                                                + (item.isCustom()
                                                                        ? " (Custom)"
                                                                        : ""));
                                        name.setStyle(
                                                "-fx-font-weight: bold; -fx-text-fill: white;");
                                        Label cost = new Label(item.getCost());
                                        cost.setStyle(
                                                "-fx-font-size: 0.9em; -fx-opacity: 0.8;"
                                                        + " -fx-text-fill: #cccccc;");
                                        box.getChildren().addAll(name, cost);
                                        setGraphic(box);
                                    }
                                }
                            });

            listView.getSelectionModel()
                    .selectedItemProperty()
                    .addListener(
                            (obs, oldV, newV) -> {
                                addBtn.setDisable(newV == null);
                            });

            filterField
                    .textProperty()
                    .addListener(
                            (obs, ov, nv) -> {
                                String filter = nv.toLowerCase();
                                listView.getItems()
                                        .setAll(
                                                availableSpells.stream()
                                                        .filter(
                                                                s ->
                                                                        s.getName()
                                                                                        .toLowerCase()
                                                                                        .contains(
                                                                                                filter)
                                                                                || s.getDescription()
                                                                                        .toLowerCase()
                                                                                        .contains(
                                                                                                filter))
                                                        .collect(Collectors.toList()));
                            });

            tab.setContent(listView);
            circlesTabPane.getTabs().add(tab);
        }

        addBtn.setOnAction(
                e -> {
                    Tab activeTab = circlesTabPane.getSelectionModel().getSelectedItem();
                    if (activeTab != null && activeTab.getContent() instanceof ListView<?> lv) {
                        Spell selected = (Spell) lv.getSelectionModel().getSelectedItem();
                        if (selected != null
                                && viewModel.getSpells().stream()
                                        .noneMatch(
                                                existing ->
                                                        existing.getName()
                                                                .equals(selected.getName()))) {
                            viewModel.getSpells().add(selected);
                            stage.close();
                        }
                    }
                });

        layout.getChildren().addAll(filterField, circlesTabPane, addBtn);
        Scene selectionScene = new Scene(layout, 600, 600);
        selectionScene.getStylesheets().addAll(getScene().getStylesheets());

        selectionScene.setOnKeyPressed(
                event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        stage.close();
                    }
                });

        stage.setScene(selectionScene);
        stage.show();
    }
}
