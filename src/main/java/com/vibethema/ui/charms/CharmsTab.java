package com.vibethema.ui.charms;

import com.vibethema.model.*;
import com.vibethema.model.mystic.*;
import com.vibethema.viewmodel.charms.CharmDetailsViewModel;
import com.vibethema.viewmodel.charms.CharmTreeViewModel;
import com.vibethema.viewmodel.charms.CharmsViewModel;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import de.saxsys.mvvmfx.ViewTuple;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class CharmsTab extends BorderPane implements JavaView<CharmsViewModel>, Initializable {

    @InjectViewModel private CharmsViewModel viewModel;

    private CharmTreeView charmTree;
    private CharmTreeViewModel charmTreeViewModel;
    private CharmDetailsView charmDetails;
    private CharmDetailsViewModel charmDetailsViewModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Create the ComboBox and bind it to ViewModel
        ComboBox<String> filterCombo = new ComboBox<>();
        filterCombo.setItems(viewModel.getFilterOptions());
        filterCombo.valueProperty().bindBidirectional(viewModel.selectedFilterValueProperty());
        filterCombo.setPrefWidth(200);

        // Load the CharmTree MVVM component
        charmTreeViewModel =
                new CharmTreeViewModel(
                        viewModel.getData(),
                        viewModel.getCharmDataService(),
                        viewModel.getKeywordDefs());

        ViewTuple<CharmTreeView, CharmTreeViewModel> vtTree =
                de.saxsys.mvvmfx.FluentViewLoader.javaView(CharmTreeView.class)
                        .viewModel(charmTreeViewModel)
                        .load();

        charmTree = vtTree.getCodeBehind();

        // Load the CharmDetails MVVM component
        charmDetailsViewModel =
                new CharmDetailsViewModel(
                        viewModel.getData(),
                        viewModel.getCharmDataService(),
                        viewModel.getKeywordDefs());

        ViewTuple<CharmDetailsView, CharmDetailsViewModel> vtDetails =
                de.saxsys.mvvmfx.FluentViewLoader.javaView(CharmDetailsView.class)
                        .viewModel(charmDetailsViewModel)
                        .load();

        charmDetails = vtDetails.getCodeBehind();

        // Synchronize the child VM with the parent VM state
        charmTreeViewModel.filterTypeProperty().bind(viewModel.filterTypeProperty());
        charmTreeViewModel.selectionIdProperty().bind(viewModel.selectedFilterValueProperty());

        // Redraw when selection changed
        viewModel
                .selectedFilterValueProperty()
                .addListener((obs, oldV, newV) -> charmTreeViewModel.refresh());

        // Header for the filter
        HBox topBar = new HBox(15);
        topBar.setPadding(new javafx.geometry.Insets(10));
        topBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label filterLabel = new Label(viewModel.filterTypeProperty().get() + ":");
        filterLabel.setLabelFor(filterCombo);
        filterCombo.setAccessibleText(viewModel.filterTypeProperty().get() + " filter");

        topBar.getChildren().addAll(filterLabel, filterCombo);

        // SplitPane to host both components
        SplitPane splitPane = new SplitPane(charmTree, charmDetails);
        splitPane.setDividerPositions(0.7);
        splitPane.getStyleClass().add("charms-split-pane");

        setTop(topBar);
        setCenter(splitPane);

        // Trigger initial refresh
        charmTreeViewModel.refresh();
    }

    public void refresh() {
        if (charmTreeViewModel != null) {
            charmTreeViewModel.refresh();
        }
    }
}
