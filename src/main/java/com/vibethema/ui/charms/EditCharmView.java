package com.vibethema.ui.charms;

import com.vibethema.model.mystic.*;
import com.vibethema.viewmodel.charms.EditCharmViewModel;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/**
 * Programmatic View for editing or creating a charm. Binds JavaFX Controls to the
 * EditCharmViewModel.
 */
public class EditCharmView extends GridPane implements JavaView<EditCharmViewModel>, Initializable {

    @InjectViewModel private EditCharmViewModel viewModel;

    private final TextField nameField = new TextField();
    private final ComboBox<String> abCombo = new ComboBox<>();
    private final Spinner<Integer> minAb = new Spinner<>(0, 5, 0);
    private final Spinner<Integer> minEss = new Spinner<>(1, 5, 1);
    private final TextField costField = new TextField();
    private final TextField typeField = new TextField();
    private final TextField durationField = new TextField();
    private final TextArea descArea = new TextArea();
    private final ListView<Charm> prereqList = new ListView<>();
    private final ListView<String> keywordsList = new ListView<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setHgap(15);
        setVgap(10);
        setPadding(new Insets(20));

        // Setup Bindings
        nameField.textProperty().bindBidirectional(viewModel.nameProperty());
        abCombo.itemsProperty().set(viewModel.getAvailableAbilities());
        abCombo.valueProperty().bindBidirectional(viewModel.abilityProperty());

        // Spinner bindings require some care
        minAb.setEditable(true);
        minEss.setEditable(true);

        // Ensure values are committed when typing and losing focus
        minAb.focusedProperty()
                .addListener(
                        (obs, oldV, newV) -> {
                            if (!newV) minAb.increment(0);
                        });
        minEss.focusedProperty()
                .addListener(
                        (obs, oldV, newV) -> {
                            if (!newV) minEss.increment(0);
                        });

        minAb.getValueFactory()
                .valueProperty()
                .bindBidirectional(viewModel.minAbilityProperty().asObject());
        minEss.getValueFactory()
                .valueProperty()
                .bindBidirectional(viewModel.minEssenceProperty().asObject());

        costField.textProperty().bindBidirectional(viewModel.costProperty());
        typeField.textProperty().bindBidirectional(viewModel.typeProperty());
        durationField.textProperty().bindBidirectional(viewModel.durationProperty());

        descArea.textProperty().bindBidirectional(viewModel.fullTextProperty());
        descArea.setWrapText(true);
        descArea.setPrefRowCount(10);
        descArea.getStyleClass().add("text-area-custom");

        // Keywords List Setup
        keywordsList.setItems(viewModel.getAvailableKeywords());
        keywordsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        keywordsList.setPrefHeight(100);
        keywordsList.getStyleClass().add("charm-tree");

        // Sync Keywords selection
        for (String k : viewModel.getSelectedKeywords()) {
            keywordsList.getSelectionModel().select(k);
        }
        keywordsList
                .getSelectionModel()
                .getSelectedItems()
                .addListener(
                        (javafx.collections.ListChangeListener<String>)
                                c -> {
                                    viewModel
                                            .getSelectedKeywords()
                                            .setAll(
                                                    keywordsList
                                                            .getSelectionModel()
                                                            .getSelectedItems());
                                });

        // Prerequisite List Setup
        prereqList.setItems(viewModel.getAvailablePrerequisites());
        prereqList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        prereqList.setPrefHeight(120);
        prereqList.getStyleClass().add("charm-tree");
        prereqList.setCellFactory(
                lv ->
                        new ListCell<>() {
                            @Override
                            protected void updateItem(Charm item, boolean empty) {
                                super.updateItem(item, empty);
                                setText(empty || item == null ? null : item.getName());
                            }
                        });

        // Sync selection from ViewModel to View (Initial)
        for (Charm c : viewModel.getSelectedPrerequisites()) {
            prereqList.getSelectionModel().select(c);
        }

        // Sync selection from View to ViewModel
        prereqList
                .getSelectionModel()
                .getSelectedItems()
                .addListener(
                        (javafx.collections.ListChangeListener<Charm>)
                                change -> {
                                    viewModel
                                            .getSelectedPrerequisites()
                                            .setAll(
                                                    prereqList
                                                            .getSelectionModel()
                                                            .getSelectedItems());
                                });

        // Re-sync when available items change (e.g. ability change)
        viewModel
                .getAvailablePrerequisites()
                .addListener(
                        (javafx.collections.ListChangeListener<Charm>)
                                change -> {
                                    prereqList.getSelectionModel().clearSelection();
                                    for (Charm sel : viewModel.getSelectedPrerequisites()) {
                                        prereqList.getSelectionModel().select(sel);
                                    }
                                });

        // Enable tabbing out of the description area
        descArea.addEventFilter(
                javafx.scene.input.KeyEvent.KEY_PRESSED,
                event -> {
                    if (event.getCode() == javafx.scene.input.KeyCode.TAB
                            && !event.isControlDown()) {
                        javafx.scene.Node parent = descArea.getParent();
                        if (parent != null) {
                            parent.fireEvent(event.copyFor(parent, parent));
                            event.consume();
                        }
                    }
                });

        // Layout
        add(new Label("Name:"), 0, 0);
        add(nameField, 1, 0);
        add(new Label("Ability (Group):"), 0, 1);
        add(abCombo, 1, 1);
        add(new Label("Cost:"), 0, 2);
        add(costField, 1, 2);
        add(new Label("Min Essence:"), 0, 3);
        add(minEss, 1, 3);
        add(new Label("Min Ability:"), 0, 4);
        add(minAb, 1, 4);
        add(new Label("Type:"), 0, 5);
        add(typeField, 1, 5);
        add(new Label("Keywords:"), 0, 6);
        add(keywordsList, 1, 6);
        add(new Label("Duration:"), 0, 7);
        add(durationField, 1, 7);
        add(new Label("Prerequisites:"), 0, 8);
        add(prereqList, 1, 8);
        add(new Label("Description:"), 0, 9);
        add(descArea, 1, 9);

        GridPane.setHgrow(nameField, Priority.ALWAYS);
        GridPane.setHgrow(abCombo, Priority.ALWAYS);
        GridPane.setHgrow(costField, Priority.ALWAYS);
        GridPane.setHgrow(typeField, Priority.ALWAYS);
        GridPane.setHgrow(keywordsList, Priority.ALWAYS);
        GridPane.setHgrow(durationField, Priority.ALWAYS);
        GridPane.setHgrow(prereqList, Priority.ALWAYS);
        GridPane.setHgrow(descArea, Priority.ALWAYS);

        abCombo.setMaxWidth(Double.MAX_VALUE);
        minAb.setMaxWidth(Double.MAX_VALUE);
        minEss.setMaxWidth(Double.MAX_VALUE);
    }
}
