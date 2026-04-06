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
    private Button deleteBtn;

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
        
        deleteBtn = new Button("🗑 Delete");
        deleteBtn.getStyleClass().add("remove-btn");
        
        createNewBtn = new Button();
        createNewBtn.getStyleClass().add("secondary-btn");
        
        addSelectedBtn = new Button("Add Selected");
        addSelectedBtn.getStyleClass().add("action-btn");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        footer.getChildren().addAll(deleteBtn, spacer, createNewBtn, addSelectedBtn);
        
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
        
        // Disable "Add" and "Delete" buttons if nothing is selected
        addSelectedBtn.disableProperty().bind(itemListView.getSelectionModel().selectedItemProperty().isNull());
        deleteBtn.disableProperty().bind(itemListView.getSelectionModel().selectedItemProperty().isNull());

        deleteBtn.setOnAction(e -> viewModel.onDeleteRequest());

        com.vibethema.viewmodel.util.Messenger.subscribe("confirm_database_deletion", (name, payload) -> {
            String itemName = (String) payload[0];
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Deletion");
            alert.setHeaderText("Delete from Database?");
            alert.setContentText("Are you sure you want to permanently delete '" + itemName + "' from the global database?");
            alert.initOwner(getScene().getWindow());
            
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    viewModel.performDelete();
                }
            });
        });
        
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
