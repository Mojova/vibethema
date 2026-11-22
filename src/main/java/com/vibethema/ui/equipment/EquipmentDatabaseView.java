package com.vibethema.ui.equipment;

import com.vibethema.viewmodel.equipment.EquipmentDatabaseViewModel;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;

/**
 * A centralized equipment selection and creation view. Displays a list of items from the database
 * and provides an option to create new ones.
 */
public class EquipmentDatabaseView extends VBox implements JavaView<EquipmentDatabaseViewModel> {

    @InjectViewModel private EquipmentDatabaseViewModel viewModel;

    private TextField searchField;
    private ListView<Object> itemListView;
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
        searchField.setPromptText("Search templates…");
        searchField.setAccessibleText("Search equipment templates");
        searchField.getStyleClass().add("text-field-custom");

        itemListView = new ListView<>();
        titleLabel.setLabelFor(itemListView);
        itemListView.getStyleClass().add("list-view-custom");
        VBox.setVgrow(itemListView, Priority.ALWAYS);

        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_RIGHT);

        deleteBtn = new Button("🗑 Delete");
        deleteBtn.getStyleClass().add("remove-btn");

        createNewBtn = new Button();
        createNewBtn.getStyleClass().add("secondary-btn");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        footer.getChildren().addAll(deleteBtn, spacer, createNewBtn);

        getChildren().addAll(titleLabel, searchField, itemListView, footer);

        // Bind UI to ViewModel properties
        // (Bindings are handled in initialize() for mvvmFX)
    }

    public void initialize() {
        // Text property bindings
        searchField.textProperty().bindBidirectional(viewModel.searchQueryProperty());
        itemListView.setItems(viewModel.getFilteredItems());
        itemListView.setCellFactory(list -> new TemplateCell());
        viewModel
                .getFilteredItems()
                .addListener((ListChangeListener<Object>) change -> itemListView.refresh());

        // Selection binding
        viewModel
                .selectedItemProperty()
                .bind(itemListView.getSelectionModel().selectedItemProperty());

        // Button actions
        createNewBtn.textProperty().bind(viewModel.createNewTextProperty());
        createNewBtn.setOnAction(e -> viewModel.onCreateNew());

        deleteBtn.visibleProperty().bind(viewModel.canDeleteSelectedProperty());
        deleteBtn.managedProperty().bind(deleteBtn.visibleProperty());

        deleteBtn.setOnAction(e -> viewModel.onDeleteRequest());

        com.vibethema.viewmodel.util.Messenger.subscribe(
                "confirm_database_deletion",
                (name, payload) -> {
                    String itemName = (String) payload[0];
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Confirm Deletion");
                    alert.setHeaderText("Delete from Database?");
                    alert.setContentText(
                            "Are you sure you want to permanently delete '"
                                    + itemName
                                    + "' from the global database?");
                    alert.initOwner(getScene().getWindow());

                    alert.showAndWait()
                            .ifPresent(response -> {
                                if (response == ButtonType.OK) {
                                    viewModel.performDelete();
                                }
                            });
                });

        // Title binding
        // We'll manage the title at the dialog level usually, but we can bind a label
        // if needed
        if (getChildren().get(0) instanceof Label titleLabel) {
            titleLabel.textProperty().bind(viewModel.titleProperty());
        }
    }

    private final class TemplateCell extends ListCell<Object> {
        private final Label heading = new Label();
        private final Label item = new Label();
        private final VBox rows = new VBox(heading, item);

        private TemplateCell() {
            getStyleClass().add("equipment-template-cell");
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            rows.prefWidthProperty().bind(widthProperty());

            heading.getStyleClass().addAll("subsection-title", "equipment-template-heading");
            heading.setMaxWidth(Double.MAX_VALUE);
            heading.setPickOnBounds(true);
            heading.managedProperty().bind(heading.visibleProperty());
            heading.addEventFilter(MouseEvent.ANY, Event::consume);

            item.getStyleClass().add("equipment-template-item");
            item.setMaxWidth(Double.MAX_VALUE);
            item.textProperty().bind(textProperty());
        }

        @Override
        protected void updateItem(Object template, boolean empty) {
            super.updateItem(template, empty);
            String section = empty ? null : viewModel.getSectionHeading(getIndex());
            heading.setText(section);
            heading.setVisible(section != null);
            setGraphic(empty ? null : rows);
            setText(empty ? null : template.toString());
        }
    }
}
