package com.vibethema.ui;

import com.vibethema.model.traits.MeritReference;
import com.vibethema.ui.traits.MeritRowView;
import com.vibethema.viewmodel.MeritsViewModel;
import com.vibethema.viewmodel.stats.MeritRowViewModel;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.collections.ListChangeListener;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
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

        MenuButton addMenuBtn = new MenuButton("+ Add Merit");
        addMenuBtn.getStyleClass().add("action-btn");

        // Group available merits by category
        Map<String, Menu> categoryMenus = new HashMap<>();
        for (MeritReference ref : viewModel.getAvailableMerits()) {
            String category = ref.getCategory();
            Menu catMenu =
                    categoryMenus.computeIfAbsent(
                            category,
                            c -> {
                                Menu m = new Menu(c);
                                addMenuBtn.getItems().add(m);
                                return m;
                            });

            MenuItem item = new MenuItem(ref.getName());
            item.setOnAction(e -> viewModel.addMerit(ref));
            catMenu.getItems().add(item);
        }

        // Add "Custom" option
        MenuItem customItem = new MenuItem("Custom...");
        customItem.setOnAction(e -> viewModel.addMerit());
        addMenuBtn.getItems().add(new SeparatorMenuItem());
        addMenuBtn.getItems().add(customItem);

        content.getChildren().addAll(title, meritsList, addMenuBtn);
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
