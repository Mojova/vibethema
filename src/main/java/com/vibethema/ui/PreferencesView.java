package com.vibethema.ui;

import com.vibethema.service.UserPreferencesService;
import com.vibethema.viewmodel.PreferencesViewModel;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class PreferencesView extends VBox implements JavaView<PreferencesViewModel>, Initializable {

    @InjectViewModel
    private PreferencesViewModel viewModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setSpacing(15);
        setPadding(new Insets(10));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        Label paperSizeLabel = new Label("PDF Paper Size:");
        ComboBox<String> paperSizeCombo = new ComboBox<>();
        paperSizeCombo.getItems().addAll(UserPreferencesService.PAPER_SIZE_A4, UserPreferencesService.PAPER_SIZE_LETTER);
        paperSizeCombo.valueProperty().bindBidirectional(viewModel.paperSizeProperty());

        grid.add(paperSizeLabel, 0, 0);
        grid.add(paperSizeCombo, 1, 0);

        getChildren().add(grid);
    }
}
