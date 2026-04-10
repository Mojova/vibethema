package com.vibethema.ui;

import com.vibethema.ui.social.IntimacyRowView;
import com.vibethema.viewmodel.IntimaciesViewModel;
import com.vibethema.viewmodel.social.IntimacyRowViewModel;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.ListChangeListener;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

public class IntimaciesTab extends ScrollPane
        implements JavaView<IntimaciesViewModel>, Initializable {

    @InjectViewModel private IntimaciesViewModel viewModel;

    private VBox intimaciesList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setFitToWidth(true);
        getStyleClass().add("scroll-pane-custom");

        VBox content = new VBox(20);
        content.getStyleClass().add("content-area");
        content.setPadding(new Insets(20));

        Label title = new Label("Intimacies");
        title.getStyleClass().add("section-title");

        intimaciesList = new VBox(12);
        intimaciesList.getStyleClass().add("merit-row-container");

        refreshIntimacies();
        viewModel
                .getIntimacyRows()
                .addListener((ListChangeListener<IntimacyRowViewModel>) c -> refreshIntimacies());

        Button addBtn = new Button("+ Add Intimacy");
        addBtn.getStyleClass().add("action-btn");
        addBtn.setOnAction(e -> viewModel.addIntimacy());

        content.getChildren().addAll(title, intimaciesList, addBtn);
        setContent(content);
    }

    private void refreshIntimacies() {
        if (intimaciesList == null) return;
        intimaciesList.getChildren().clear();

        for (IntimacyRowViewModel rowVm : viewModel.getIntimacyRows()) {
            IntimacyRowView rowView = new IntimacyRowView(rowVm);
            rowView.setOnRemove(e -> viewModel.removeIntimacy(rowVm.getModel()));
            intimaciesList.getChildren().add(rowView);
        }
    }
}
