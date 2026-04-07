package com.vibethema.ui.charms;

import com.vibethema.viewmodel.charms.EditCharmViewModel;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Programmatic View for editing or creating a charm.
 * Binds JavaFX Controls to the EditCharmViewModel.
 */
public class EditCharmView extends GridPane implements JavaView<EditCharmViewModel>, Initializable {

    @InjectViewModel
    private EditCharmViewModel viewModel;

    private final TextField nameField = new TextField();
    private final ComboBox<String> abCombo = new ComboBox<>();
    private final Spinner<Integer> minAb = new Spinner<>(0, 5, 0);
    private final Spinner<Integer> minEss = new Spinner<>(1, 5, 1);
    private final TextField costField = new TextField();
    private final TextField typeField = new TextField();
    private final TextField durationField = new TextField();
    private final TextArea descArea = new TextArea();

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
        minAb.getValueFactory().valueProperty().bindBidirectional(viewModel.minAbilityProperty().asObject());
        minEss.getValueFactory().valueProperty().bindBidirectional(viewModel.minEssenceProperty().asObject());

        costField.textProperty().bindBidirectional(viewModel.costProperty());
        typeField.textProperty().bindBidirectional(viewModel.typeProperty());
        durationField.textProperty().bindBidirectional(viewModel.durationProperty());
        
        descArea.textProperty().bindBidirectional(viewModel.fullTextProperty());
        descArea.setWrapText(true);
        descArea.setPrefRowCount(4);

        // Layout
        add(new Label("Name:"), 0, 0); add(nameField, 1, 0);
        add(new Label("Ability:"), 0, 1); add(abCombo, 1, 1);
        add(new Label("Min Ability:"), 0, 2); add(minAb, 1, 2);
        add(new Label("Min Essence:"), 0, 3); add(minEss, 1, 3);
        add(new Label("Cost:"), 0, 4); add(costField, 1, 4);
        add(new Label("Type:"), 0, 5); add(typeField, 1, 5);
        add(new Label("Duration:"), 0, 6); add(durationField, 1, 6);
        add(new Label("Description:"), 0, 7); add(descArea, 1, 7);

        GridPane.setHgrow(nameField, Priority.ALWAYS);
        GridPane.setHgrow(abCombo, Priority.ALWAYS);
        GridPane.setHgrow(descArea, Priority.ALWAYS);
        
        abCombo.setMaxWidth(Double.MAX_VALUE);
        minAb.setMaxWidth(Double.MAX_VALUE);
        minEss.setMaxWidth(Double.MAX_VALUE);
    }
}
