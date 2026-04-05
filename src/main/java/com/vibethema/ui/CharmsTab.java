package com.vibethema.ui;

import com.vibethema.model.Charm;
import com.vibethema.ui.charms.CharmTreeComponent;
import com.vibethema.viewmodel.CharmsViewModel;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import java.net.URL;
import java.util.ResourceBundle;

public class CharmsTab extends BorderPane implements JavaView<CharmsViewModel>, Initializable {

    @InjectViewModel
    private CharmsViewModel viewModel;

    private CharmTreeComponent charmTree;
    private final CharmTreeComponent.CharmTreeListener listener;

    public CharmsTab(CharmTreeComponent.CharmTreeListener listener) {
        this.listener = listener;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Create the ComboBox and bind it to ViewModel
        ComboBox<String> filterCombo = new ComboBox<>();
        filterCombo.setItems(viewModel.getFilterOptions());
        filterCombo.valueProperty().bindBidirectional(viewModel.selectedFilterValueProperty());
        filterCombo.setPrefWidth(200);

        // Instantiate the recursive CharmTreeComponent
        charmTree = new CharmTreeComponent(
                viewModel.getData(),
                viewModel.getCharmDataService(),
                viewModel.getKeywordDefs(),
                listener,
                filterCombo,
                viewModel.filterTypeProperty().get()
        );

        setCenter(charmTree);
    }

    public void refresh() {
        if (charmTree != null) {
            charmTree.refresh();
        }
    }
}
