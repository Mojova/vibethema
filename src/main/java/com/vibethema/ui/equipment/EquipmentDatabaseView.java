package com.vibethema.ui.equipment;

import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import com.vibethema.viewmodel.equipment.EquipmentDatabaseViewModel;

/**
 * A centralized equipment selection and creation view.
 * Displays a list of items from the database and provides an option to create new ones.
 */
public class EquipmentDatabaseView extends VBox implements JavaView<EquipmentDatabaseViewModel> {

    @InjectViewModel
    private EquipmentDatabaseViewModel viewModel;

    private TextField searchField;
    private ListView<Object> itemListView;
    private Button addSelectedBtn;
    private Button createNewBtn;

    public EquipmentDatabaseView() {
        setSpacing(15);
        setPadding(new Insets(20));
        setPrefSize(500, 600);
        setupLayout();
    }

    private void setupLayout() {
        Label titleLabel = new Label();
        titleLabel.getStyleClass().add("section-title");
        
        searchField = new TextField();
        searchField.setPromptText("Search items...");
        searchField.getStyleClass().add("text-field-custom");
        
        itemListView = new ListView<>();
        itemListView.getStyleClass().add("list-view-custom");
        VBox.setVgrow(itemListView, Priority.ALWAYS);
        
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_RIGHT);
        
        createNewBtn = new Button();
        createNewBtn.getStyleClass().add("secondary-btn");
        
        addSelectedBtn = new Button("Add Selected");
        addSelectedBtn.getStyleClass().add("action-btn");
        
        footer.getChildren().addAll(createNewBtn, addSelectedBtn);
        
        getChildren().addAll(titleLabel, searchField, itemListView, footer);
        
        // Bind UI to ViewModel properties
        // (Bindings are handled in initialize() for mvvmFX)
    }

    public void initialize() {
        // Text property bindings
        searchField.textProperty().bindBidirectional(viewModel.searchQueryProperty());
        itemListView.setItems(viewModel.getFilteredItems());
        
        // Selection binding
        viewModel.selectedItemProperty().bind(itemListView.getSelectionModel().selectedItemProperty());
        
        // Button actions
        createNewBtn.textProperty().bind(viewModel.createNewTextProperty());
        createNewBtn.setOnAction(e -> viewModel.onCreateNew());
        
        // Disable "Add" button if nothing is selected
        addSelectedBtn.disableProperty().bind(itemListView.getSelectionModel().selectedItemProperty().isNull());
        
        // Title binding
        // We'll manage the title at the dialog level usually, but we can bind a label if needed
        if (getChildren().get(0) instanceof Label titleLabel) {
            titleLabel.textProperty().bind(viewModel.titleProperty());
        }
    }

    public Button getAddSelectedBtn() {
        return addSelectedBtn;
    }
}
