package com.vibethema.ui.sorcery;

import com.vibethema.model.SystemData;
import com.vibethema.model.CharacterData;
import com.vibethema.model.ShapingRitual;
import com.vibethema.model.Spell;
import com.vibethema.service.CharmDataService;
import com.vibethema.ui.util.UIUtils;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A standalone UI component for managing a character's Sorcery (Rituals and Spells).
 */
public class SorceryTab extends ScrollPane {

    private final CharacterData data;
    private final CharmDataService dataService;
    private final Runnable updateFooter;

    private Label sorceryWarningLabel;
    private VBox sorceryMainContent;
    private VBox shapingRitualsListContainer;
    private VBox spellsListContainer;

    public SorceryTab(CharacterData data, CharmDataService dataService, Runnable updateFooter) {
        this.data = data;
        this.dataService = dataService;
        this.updateFooter = updateFooter;

        setFitToWidth(true);
        getStyleClass().add("scroll-pane-custom");

        setContent(createContent());
        setupListeners();
        // Bindings set up in createSorceryContent or similar
    }

    private VBox createContent() {
        VBox content = new VBox(20);
        content.getStyleClass().add("content-area");
        content.setPadding(new Insets(20));

        sorceryWarningLabel = new Label("Purchase Occult charm Terrestrial Circle Sorcery to enable sorcery.");
        sorceryWarningLabel.getStyleClass().add("problematic-warning");
        sorceryWarningLabel.setMaxWidth(Double.MAX_VALUE);
        sorceryWarningLabel.setAlignment(Pos.CENTER);

        sorceryMainContent = new VBox(25);

        // Shaping Rituals
        VBox shapingSection = UIUtils.createSection("Shaping Rituals");
        shapingRitualsListContainer = new VBox(10);
        shapingRitualsListContainer.getStyleClass().add("merit-row-container");

        Button addRitualBtn = new Button("+ Add Shaping Ritual");
        addRitualBtn.getStyleClass().add("add-btn");
        addRitualBtn.getStyleClass().add("action-btn");
        addRitualBtn.setOnAction(e -> {
            ShapingRitual r = new ShapingRitual("New Ritual", "Description of the ritual's benefits and triggers...");
            data.getShapingRituals().add(r);
        });
        shapingSection.getChildren().addAll(shapingRitualsListContainer, addRitualBtn);

        // Spells
        VBox spellsSection = UIUtils.createSection("Spells");
        spellsListContainer = new VBox(10);
        spellsListContainer.getStyleClass().add("merit-row-container");

        Button addSpellBtn = new Button("+ Add Spell");
        addSpellBtn.getStyleClass().add("add-btn");
        addSpellBtn.getStyleClass().add("action-btn");
        addSpellBtn.setOnAction(e -> showSpellSelectionDialog());
        spellsSection.getChildren().addAll(spellsListContainer, addSpellBtn);

        sorceryMainContent.getChildren().addAll(shapingSection, spellsSection);
        content.getChildren().addAll(sorceryWarningLabel, sorceryMainContent);

        refreshShapingRituals();
        refreshSpellsList();

        return content;
    }

    private void setupListeners() {
        data.getShapingRituals().addListener((ListChangeListener<? super ShapingRitual>) c -> {
            boolean structuralChange = false;
            while (c.next()) {
                if (c.wasAdded() || c.wasRemoved() || c.wasPermutated()) structuralChange = true;
            }
            if (structuralChange) refreshShapingRituals();
            if (updateFooter != null) updateFooter.run();
        });

        data.getSpells().addListener((ListChangeListener<? super Spell>) c -> {
            boolean structuralChange = false;
            while (c.next()) {
                if (c.wasAdded() || c.wasRemoved() || c.wasPermutated()) structuralChange = true;
            }
            if (structuralChange) refreshSpellsList();
            if (updateFooter != null) updateFooter.run();
        });

    }

    // Removed manual refreshSorceryEligibility logic, now handled by bindings

    private void refreshShapingRituals() {
        if (shapingRitualsListContainer == null) return;
        shapingRitualsListContainer.getChildren().clear();

        for (ShapingRitual r : data.getShapingRituals()) {
            VBox rowContainer = new VBox(5);
            rowContainer.getStyleClass().add("merit-row");
            rowContainer.setPadding(new Insets(10));

            HBox header = new HBox(15);
            header.setAlignment(Pos.CENTER_LEFT);

            TextField nameField = new TextField(r.getName());
            nameField.setPromptText("Ritual Name (e.g. Soul-Perfecting Method)");
            HBox.setHgrow(nameField, Priority.ALWAYS);
            nameField.textProperty().addListener((obs, ov, nv) -> r.setName(nv));

            Button delBtn = new Button("🗑");
            delBtn.getStyleClass().add("remove-btn");
            delBtn.setOnAction(e -> data.getShapingRituals().remove(r));

            header.getChildren().addAll(nameField, delBtn);

            TextArea descArea = new TextArea(r.getDescription());
            descArea.setPromptText("Describe the shaping ritual rules...");
            descArea.setPrefRowCount(3);
            descArea.setWrapText(true);
            descArea.textProperty().addListener((obs, ov, nv) -> r.setDescription(nv));

            rowContainer.getChildren().addAll(header, descArea);
            shapingRitualsListContainer.getChildren().add(rowContainer);
        }
    }

    private void refreshSpellsList() {
        if (spellsListContainer == null) return;
        spellsListContainer.getChildren().clear();

        for (Spell s : data.getSpells()) {
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

            Button delBtn = new Button("🗑");
            delBtn.getStyleClass().add("remove-btn");
            delBtn.setOnAction(e -> {
                if (s.isCustom()) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Remove Spell");
                    alert.setHeaderText("Remove " + s.getName());
                    alert.setContentText("Do you want to just remove it from this character, or permanently delete it from the database?");
                    
                    ButtonType removeOnly = new ButtonType("Remove from Character");
                    ButtonType deleteDB = new ButtonType("Permanently Delete", ButtonBar.ButtonData.OTHER);
                    ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                    
                    alert.getButtonTypes().setAll(removeOnly, deleteDB, cancel);
                    alert.showAndWait().ifPresent(type -> {
                        if (type == removeOnly) {
                            data.getSpells().remove(s);
                        } else if (type == deleteDB) {
                            try {
                                dataService.deleteCustomSpell(s);
                                data.getSpells().remove(s);
                            } catch (IOException ex) {
                                new Alert(Alert.AlertType.ERROR, "Error deleting spell: " + ex.getMessage()).showAndWait();
                            }
                        }
                    });
                } else {
                    data.getSpells().remove(s);
                }
            });

            header.getChildren().addAll(title, circleLabel, delBtn);

            GridPane details = new GridPane();
            details.setHgap(20);
            details.setVgap(5);
            details.add(new Label("Cost: " + s.getCost()), 0, 0);
            details.add(new Label("Duration: " + s.getDuration()), 1, 0);
            
            if (!s.getKeywords().isEmpty()) {
                details.add(new Label("Keywords: " + String.join(", ", s.getKeywords())), 0, 1, 2, 1);
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
            if (circle.equals("CELESTIAL") && !data.hasCharmByName(SystemData.CELESTIAL_CIRCLE_SORCERY)) eligible = false;
            if (circle.equals("SOLAR") && !data.hasCharmByName(SystemData.SOLAR_CIRCLE_SORCERY)) eligible = false;
            
            if (!eligible) {
                tab.setDisable(true);
                tab.setTooltip(new Tooltip("Missing " + (circle.equals("CELESTIAL") ? "Celestial" : "Solar") + " Circle Sorcery charm"));
            }
            
            ListView<Spell> listView = new ListView<>();
            List<Spell> availableSpells = dataService.loadSpells(circle);
            listView.getItems().addAll(availableSpells);

            listView.setCellFactory(lv -> new ListCell<Spell>() {
                @Override
                protected void updateItem(Spell item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        VBox box = new VBox(2);
                        Label name = new Label(item.getName() + (item.isCustom() ? " (Custom)" : ""));
                        name.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
                        Label cost = new Label(item.getCost());
                        cost.setStyle("-fx-font-size: 0.9em; -fx-opacity: 0.8; -fx-text-fill: #cccccc;");
                        box.getChildren().addAll(name, cost);
                        setGraphic(box);
                    }
                }
            });

            listView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
                addBtn.setDisable(newV == null);
            });

            filterField.textProperty().addListener((obs, ov, nv) -> {
                String filter = nv.toLowerCase();
                listView.getItems().setAll(availableSpells.stream()
                    .filter(s -> s.getName().toLowerCase().contains(filter) || s.getDescription().toLowerCase().contains(filter))
                    .collect(Collectors.toList()));
            });

            tab.setContent(listView);
            circlesTabPane.getTabs().add(tab);
        }

        Button createCustomBtn = new Button("Create Custom Spell...");
        createCustomBtn.getStyleClass().add("secondary-btn");
        createCustomBtn.setMaxWidth(Double.MAX_VALUE);
        createCustomBtn.setOnAction(e -> {
            Tab activeTab = circlesTabPane.getSelectionModel().getSelectedItem();
            if (activeTab != null) {
                String initialCircle = activeTab.getText().toUpperCase();
                showCreateSpellDialog(initialCircle, () -> {
                    for (Tab t : circlesTabPane.getTabs()) {
                        String cName = t.getText().toUpperCase();
                        if (t.getContent() instanceof ListView<?> lv) {
                            @SuppressWarnings("unchecked")
                            ListView<Spell> slv = (ListView<Spell>) lv;
                            slv.getItems().setAll(dataService.loadSpells(cName));
                        }
                    }
                });
            }
        });

        addBtn.setOnAction(e -> {
            Tab activeTab = circlesTabPane.getSelectionModel().getSelectedItem();
            if (activeTab != null && activeTab.getContent() instanceof ListView<?> lv) {
                Spell selected = (Spell) lv.getSelectionModel().getSelectedItem();
                if (selected != null && data.getSpells().stream().noneMatch(existing -> existing.getName().equals(selected.getName()))) {
                    data.getSpells().add(selected);
                    stage.close();
                }
            }
        });

        layout.getChildren().addAll(filterField, circlesTabPane, createCustomBtn, addBtn);
        Scene selectionScene = new Scene(layout, 600, 600);
        selectionScene.getStylesheets().addAll(getScene().getStylesheets());
        stage.setScene(selectionScene);
        stage.show();
    }

    private void showCreateSpellDialog(String initialCircle, Runnable onSave) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(getScene().getWindow());
        stage.setTitle("Create Custom Spell");

        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-pane");
        root.setStyle("-fx-background-color: #1e1e1e;");

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(12);

        TextField nameField = new TextField();
        nameField.setPromptText("Spell Name");
        
        ComboBox<String> circleCombo = new ComboBox<>(FXCollections.observableArrayList("TERRESTRIAL", "CELESTIAL", "SOLAR"));
        circleCombo.setValue(initialCircle);

        TextField costField = new TextField();
        costField.setPromptText("e.g. 10sm, 1wp");

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
        selectKwBtn.setOnAction(e -> UIUtils.showKeywordSelectionDialog(stage, dataService, new ArrayList<>(selectedKeywords), result -> {
            selectedKeywords.setAll(result);
            updateKwUI.run();
        }));
        
        VBox kwBox = new VBox(5, kwDisplay, selectKwBtn);

        TextArea descArea = new TextArea();
        descArea.setPromptText("Spell Description...");
        descArea.setWrapText(true);
        descArea.setPrefRowCount(6);

        grid.add(new Label("Name:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Circle:"), 0, 1); grid.add(circleCombo, 1, 1);
        grid.add(new Label("Cost:"), 0, 2); grid.add(costField, 1, 2);
        grid.add(new Label("Duration:"), 0, 3); grid.add(durationField, 1, 3);
        grid.add(new Label("Keywords:"), 0, 4); grid.add(kwBox, 1, 4);
        
        for (javafx.scene.Node n : grid.getChildren()) {
            if (n instanceof Label l) l.setStyle("-fx-text-fill: #f9f6e6;");
        }

        Button saveBtn = new Button("Save Spell");
        saveBtn.getStyleClass().add("action-btn");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> {
            if (nameField.getText().isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "Spell name cannot be empty.").showAndWait();
                return;
            }
            Spell s = new Spell();
            s.setName(nameField.getText());
            s.setCircle(circleCombo.getValue());
            s.setCost(costField.getText());
            s.setDuration(durationField.getText());
            s.setDescription(descArea.getText());
            s.getKeywords().setAll(selectedKeywords);
            s.setCustom(true);

            try {
                dataService.saveCustomSpell(s);
                if (onSave != null) onSave.run();
                stage.close();
            } catch (IOException ex) {
                new Alert(Alert.AlertType.ERROR, "Error saving spell: " + ex.getMessage()).showAndWait();
            }
        });

        root.getChildren().addAll(grid, new Label("Description:"), descArea, saveBtn);
        ((Label)root.getChildren().get(1)).setStyle("-fx-text-fill: #f9f6e6;");

        Scene createScene = new Scene(root, 600, 750);
        createScene.getStylesheets().addAll(getScene().getStylesheets());
        stage.setScene(createScene);
        stage.show();
    }
}
