package com.vibethema.ui.martialarts;

import com.vibethema.ui.charms.CharmDetailsView;
import com.vibethema.ui.charms.CharmTreeView;
import com.vibethema.viewmodel.charms.CharmDetailsViewModel;
import com.vibethema.viewmodel.charms.CharmTreeViewModel;
import com.vibethema.viewmodel.martialarts.MartialArtsRowViewModel;
import com.vibethema.viewmodel.martialarts.MartialArtsViewModel;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import de.saxsys.mvvmfx.ViewTuple;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Tab for managing and viewing Martial Arts styles and charms. */
public class MartialArtsTab extends VBox implements JavaView<MartialArtsViewModel>, Initializable {

    @InjectViewModel private MartialArtsViewModel viewModel;

    private VBox stylesList;
    private CharmTreeView charmTree;
    private CharmTreeViewModel charmTreeViewModel;
    private CharmDetailsView charmDetails;
    private CharmDetailsViewModel charmDetailsViewModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setPadding(new Insets(20));
        setSpacing(20);
        getStyleClass().add("content-area");

        Label title = new Label("Martial Arts Styles");
        title.getStyleClass().add("section-title");

        stylesList = new VBox(12);
        stylesList.getStyleClass().add("merits-list"); // Reuse styling

        refreshStyles();
        viewModel
                .getStyleRows()
                .addListener((ListChangeListener<MartialArtsRowViewModel>) c -> refreshStyles());

        MenuButton addMenuBtn = new MenuButton("+ Add Style");
        addMenuBtn.getStyleClass().addAll("add-btn", "action-btn");

        // Prepopulated styles
        for (String styleName : viewModel.getAvailableStyles()) {
            MenuItem item = new MenuItem(styleName);
            item.setOnAction(e -> viewModel.addStyle(styleName));
            addMenuBtn.getItems().add(item);
        }

        // Custom option
        MenuItem customItem = new MenuItem("Custom...");
        customItem.setOnAction(e -> viewModel.addStyle());
        addMenuBtn.getItems().add(new SeparatorMenuItem());
        addMenuBtn.getItems().add(customItem);

        VBox managementSection = new VBox(10, title, stylesList, addMenuBtn);

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

        // SplitPane to host both components
        SplitPane splitPane = new SplitPane(charmTree, charmDetails);
        splitPane.getStyleClass().add("charms-split-pane");
        splitPane.setDividerPositions(0.75);
        SplitPane.setResizableWithParent(charmDetails, false);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // Main layout vs Warning
        Label warningLabel =
                new Label("Purchase Martial Artist merit to enable Martial Arts styles.");
        warningLabel.getStyleClass().add("problematic-warning");
        warningLabel.setMaxWidth(Double.MAX_VALUE);
        warningLabel.setAlignment(Pos.CENTER);
        warningLabel.visibleProperty().bind(viewModel.martialArtsEnabledProperty().not());
        warningLabel.managedProperty().bind(warningLabel.visibleProperty());

        VBox mainContent = new VBox(20);
        mainContent.visibleProperty().bind(viewModel.martialArtsEnabledProperty());
        mainContent.managedProperty().bind(mainContent.visibleProperty());
        VBox.setVgrow(mainContent, Priority.ALWAYS);
        mainContent.getChildren().addAll(managementSection, splitPane);

        getChildren().addAll(warningLabel, mainContent);

        // Trigger initial refresh
        charmTreeViewModel.refresh();
    }

    private void refreshStyles() {
        if (stylesList == null) return;
        stylesList.getChildren().clear();

        for (MartialArtsRowViewModel rowVm : viewModel.getStyleRows()) {
            MartialArtsRowView rowView = new MartialArtsRowView(rowVm);
            rowView.setOnRemove(e -> viewModel.removeStyle(rowVm.getModel()));

            // Clicking the row selects it in the charm tree
            rowView.setOnMouseClicked(
                    e -> {
                        viewModel
                                .selectedFilterValueProperty()
                                .set(rowVm.getModel().getStyleName());
                    });

            // Highlight if selected
            rowView.styleProperty()
                    .bind(
                            Bindings.createStringBinding(
                                    () -> {
                                        if (viewModel
                                                .selectedFilterValueProperty()
                                                .get()
                                                .equals(rowVm.getModel().getStyleName())) {
                                            return "-fx-background-color:"
                                                    + " rgba(var(--caste-color-rgb), 0.1);"
                                                    + " -fx-border-color: var(--caste-color);";
                                        }
                                        return "";
                                    },
                                    viewModel.selectedFilterValueProperty(),
                                    rowVm.nameProperty()));

            stylesList.getChildren().add(rowView);
        }
    }

    public void refresh() {
        if (charmTreeViewModel != null) {
            charmTreeViewModel.refresh();
        }
    }
}
