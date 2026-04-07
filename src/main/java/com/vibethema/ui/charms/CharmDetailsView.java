package com.vibethema.ui.charms;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.social.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.logic.*;


import com.vibethema.viewmodel.charms.CharmDetailsViewModel;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.net.URL;
import java.util.ResourceBundle;

public class CharmDetailsView extends VBox implements JavaView<CharmDetailsViewModel>, Initializable {

    @InjectViewModel
    private CharmDetailsViewModel viewModel;

    private final Label detailTitle = new Label();
    private final Label detailReqs = new Label();
    private final Label costLabel = new Label();
    private final Label typeLabel = new Label();
    private final Label durationLabel = new Label();
    private final FlowPane kwFlow = new FlowPane(3, 3);
    private final GridPane statsGrid = new GridPane();
    private final Label descriptionLabel = new Label();

    private final Button purchaseBtn = new Button();
    private final Button refundBtn = new Button();
    private final Button deleteCustomBtn = new Button();
    private final Button editBtn = new Button();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        getStyleClass().add("charms-sidebar");
        setFillWidth(true);

        setupUI();
        setupBindings();
    }

    private void setupUI() {
        detailTitle.getStyleClass().add("sidebar-title");
        detailTitle.setWrapText(true);
        detailTitle.setPadding(new Insets(20, 20, 10, 20));

        VBox sidebarContent = new VBox(20);
        sidebarContent.setPadding(new Insets(0, 0, 20, 0));
        sidebarContent.setFillWidth(true);

        detailReqs.getStyleClass().add("sidebar-reqs");
        detailReqs.setWrapText(true);
        detailReqs.setMaxWidth(Double.MAX_VALUE);

        statsGrid.setHgap(15); statsGrid.setVgap(10);
        costLabel.getStyleClass().add("sidebar-stat");
        typeLabel.getStyleClass().add("sidebar-stat");
        durationLabel.getStyleClass().add("sidebar-stat");
        kwFlow.setPrefWrapLength(200);

        statsGrid.add(new Label("Cost:"), 0, 0);
        statsGrid.add(costLabel, 1, 0);
        statsGrid.add(new Label("Type:"), 0, 1);
        statsGrid.add(typeLabel, 1, 1);
        statsGrid.add(new Label("Duration:"), 0, 2);
        statsGrid.add(durationLabel, 1, 2);
        statsGrid.add(new Label("Keywords:"), 0, 3);
        statsGrid.add(kwFlow, 1, 3);

        for (javafx.scene.Node n : statsGrid.getChildren()) {
            if (GridPane.getColumnIndex(n) == 0 && n instanceof Label) {
                n.getStyleClass().add("sidebar-stat-header");
            }
        }

        VBox paddedContent = new VBox(20);
        paddedContent.setPadding(new Insets(0, 20, 0, 20));

        descriptionLabel.getStyleClass().add("sidebar-desc");
        descriptionLabel.setWrapText(true);
        
        purchaseBtn.getStyleClass().add("charm-btn");
        purchaseBtn.setMaxWidth(Double.MAX_VALUE);
        purchaseBtn.setTooltip(new Tooltip("Buy or Add Stack (ENTER / SPACE)"));
        HBox.setHgrow(purchaseBtn, Priority.ALWAYS);
        purchaseBtn.setOnAction(e -> viewModel.togglePurchase());

        refundBtn.getStyleClass().add("charm-btn");
        refundBtn.setTooltip(new Tooltip("Refund One (BACKSPACE / DELETE)"));
        refundBtn.setStyle("-fx-base: #a03030;");
        refundBtn.setOnAction(e -> viewModel.refundOne());

        editBtn.getStyleClass().add("charm-btn");
        editBtn.setMnemonicParsing(true);
        editBtn.setTooltip(new Tooltip("Edit Charm (E)"));
        editBtn.setText("_Edit Charm");
        editBtn.setStyle("-fx-base: #3c3c3c;");
        editBtn.setOnAction(e -> viewModel.onEditRequest());

        deleteCustomBtn.getStyleClass().add("charm-btn");
        deleteCustomBtn.setStyle("-fx-base: #a03030;");
        deleteCustomBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, 
                "Are you sure you want to delete this custom charm?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(r -> { if (r == ButtonType.YES) viewModel.deleteCustom(); });
        });

        HBox charmButtons = new HBox(10, purchaseBtn, refundBtn, deleteCustomBtn, editBtn);
        
        paddedContent.getChildren().addAll(charmButtons, statsGrid, descriptionLabel);
        sidebarContent.getChildren().addAll(detailReqs, paddedContent);

        ScrollPane sideContentScroll = new ScrollPane(sidebarContent);
        sideContentScroll.setFitToWidth(true);
        sideContentScroll.getStyleClass().add("scroll-pane-custom");
        VBox.setVgrow(sideContentScroll, Priority.ALWAYS);

        getChildren().addAll(detailTitle, sideContentScroll);
    }

    private void setupBindings() {
        detailTitle.textProperty().bind(viewModel.detailTitleProperty());
        detailReqs.textProperty().bind(viewModel.detailReqsProperty());
        costLabel.textProperty().bind(viewModel.costTextProperty());
        typeLabel.textProperty().bind(viewModel.typeTextProperty());
        durationLabel.textProperty().bind(viewModel.durationTextProperty());
        descriptionLabel.textProperty().bind(viewModel.descriptionTextProperty());

        purchaseBtn.textProperty().bind(viewModel.purchaseBtnTextProperty());
        purchaseBtn.disableProperty().bind(viewModel.purchaseBtnDisabledProperty());
        purchaseBtn.visibleProperty().bind(viewModel.purchaseBtnVisibleProperty());
        purchaseBtn.managedProperty().bind(purchaseBtn.visibleProperty());
        purchaseBtn.styleProperty().bind(viewModel.purchaseBtnStyleProperty());

        refundBtn.textProperty().bind(viewModel.refundBtnTextProperty());
        refundBtn.visibleProperty().bind(viewModel.refundBtnVisibleProperty());
        refundBtn.managedProperty().bind(refundBtn.visibleProperty());

        deleteCustomBtn.textProperty().bind(viewModel.deleteCustomBtnTextProperty());
        deleteCustomBtn.visibleProperty().bind(viewModel.deleteCustomBtnVisibleProperty());
        deleteCustomBtn.managedProperty().bind(deleteCustomBtn.visibleProperty());

        editBtn.visibleProperty().bind(viewModel.editBtnVisibleProperty());
        editBtn.managedProperty().bind(editBtn.visibleProperty());

        // Visibility of headers when no charm selected
        detailReqs.visibleProperty().bind(viewModel.selectedCharmProperty().isNotNull());
        detailReqs.managedProperty().bind(detailReqs.visibleProperty());
        descriptionLabel.visibleProperty().bind(viewModel.selectedCharmProperty().isNotNull());
        descriptionLabel.managedProperty().bind(descriptionLabel.visibleProperty());
        statsGrid.visibleProperty().bind(viewModel.selectedCharmProperty().isNotNull());
        statsGrid.managedProperty().bind(statsGrid.visibleProperty());

        viewModel.getKeywords().addListener((javafx.collections.ListChangeListener<? super String>) c -> updateKeywordsUI());
        updateKeywordsUI();
    }

    private void updateKeywordsUI() {
        kwFlow.getChildren().clear();
        if (viewModel.selectedCharmProperty().get() == null) return;

        if (viewModel.getKeywords().isEmpty()) {
            Label l = new Label("None");
            l.getStyleClass().add("sidebar-stat");
            kwFlow.getChildren().add(l);
        } else {
            for (String kw : viewModel.getKeywords()) {
                Label l = new Label(kw);
                l.getStyleClass().add("sidebar-stat");
                l.getStyleClass().add("keyword-label");
                String def = viewModel.getKeywordDefs().get(kw);
                if (def != null) {
                    Tooltip tt = new Tooltip(def); tt.setWrapText(true); tt.setMaxWidth(300);
                    l.setTooltip(tt);
                    l.getStyleClass().add("keyword-with-def");
                }
                kwFlow.getChildren().add(l);
            }
        }
    }
}