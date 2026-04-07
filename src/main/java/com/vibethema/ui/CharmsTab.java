package com.vibethema.ui;

import com.vibethema.ui.charms.CharmTreeComponent;
import com.vibethema.viewmodel.CharmsViewModel;
import com.vibethema.viewmodel.charms.CharmTreeViewModel;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import de.saxsys.mvvmfx.ViewTuple;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import java.net.URL;
import java.util.ResourceBundle;

public class CharmsTab extends BorderPane implements JavaView<CharmsViewModel>, Initializable {

    @InjectViewModel
    private CharmsViewModel viewModel;

    private CharmTreeComponent charmTree;
    private CharmTreeViewModel charmTreeViewModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Create the ComboBox and bind it to ViewModel
        ComboBox<String> filterCombo = new ComboBox<>();
        filterCombo.setItems(viewModel.getFilterOptions());
        filterCombo.valueProperty().bindBidirectional(viewModel.selectedFilterValueProperty());
        filterCombo.setPrefWidth(200);

        // Load the CharmTree MVVM component
        charmTreeViewModel = new CharmTreeViewModel(
            viewModel.getData(), 
            viewModel.getCharmDataService(), 
            viewModel.getKeywordDefs()
        );
        
        ViewTuple<CharmTreeComponent, CharmTreeViewModel> vt = de.saxsys.mvvmfx.FluentViewLoader
                .javaView(CharmTreeComponent.class)
                .viewModel(charmTreeViewModel)
                .load();
        
        charmTree = vt.getCodeBehind();
        
        // Synchronize the child VM with the parent VM state
        charmTreeViewModel.filterTypeProperty().bind(viewModel.filterTypeProperty());
        charmTreeViewModel.selectionIdProperty().bind(viewModel.selectedFilterValueProperty());
        
        // If we want to support Evocation mode jump later, we can bind artifactId as well.
        
        // Redraw when selection changed
        viewModel.selectedFilterValueProperty().addListener((obs, oldV, newV) -> charmTreeViewModel.refresh());

        // Header for the filter
        HBox topBar = new HBox(15);
        topBar.setPadding(new javafx.geometry.Insets(10));
        topBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        topBar.getChildren().addAll(new javafx.scene.control.Label(viewModel.filterTypeProperty().get() + ":"), filterCombo);
        
        setTop(topBar);
        setCenter(charmTree);
        
        // Trigger initial refresh
        charmTreeViewModel.refresh();
    }

    public void refresh() {
        if (charmTreeViewModel != null) {
            charmTreeViewModel.refresh();
        }
    }
}
