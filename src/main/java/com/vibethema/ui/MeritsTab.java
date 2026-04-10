package com.vibethema.ui;

import com.vibethema.ui.traits.MeritRowView;
import com.vibethema.viewmodel.MeritsViewModel;
import com.vibethema.viewmodel.stats.MeritRowViewModel;
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

public class MeritsTab extends ScrollPane implements JavaView<MeritsViewModel>, Initializable {

    @InjectViewModel private MeritsViewModel viewModel;

    private VBox meritsList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setFitToWidth(true);
        getStyleClass().add("scroll-pane-custom");

        VBox content = new VBox(20);
        content.getStyleClass().add("content-area");
        content.setPadding(new Insets(20));

        Label title = new Label("Merits");
        title.getStyleClass().add("section-title");

        meritsList = new VBox(12);
        meritsList.getStyleClass().add("merits-list");

        refreshMerits();
        viewModel
                .getMeritRows()
                .addListener((ListChangeListener<MeritRowViewModel>) c -> refreshMerits());

        Button addBtn = new Button("+ Add Merit");
        addBtn.getStyleClass().add("action-btn");
        addBtn.setOnAction(e -> viewModel.addMerit());

        content.getChildren().addAll(title, meritsList, addBtn);
        setContent(content);
    }

    private void refreshMerits() {
        if (meritsList == null) return;
        meritsList.getChildren().clear();

        for (MeritRowViewModel rowVm : viewModel.getMeritRows()) {
            MeritRowView rowView = new MeritRowView(rowVm);
            rowView.setOnRemove(e -> viewModel.removeMerit(rowVm.getModel()));
            meritsList.getChildren().add(rowView);
        }
    }
}
