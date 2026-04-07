package com.vibethema.ui.experience;

import com.vibethema.model.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;
import com.vibethema.model.traits.*;
import com.vibethema.viewmodel.experience.ExperienceViewModel;
import com.vibethema.viewmodel.experience.XpAwardRowViewModel;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Experience tab View — shows the XP award log and the dynamically computed spend breakdown. */
public class ExperienceTab extends ScrollPane implements JavaView<ExperienceViewModel> {

    @InjectViewModel private ExperienceViewModel viewModel;

    private VBox awardsListContainer;
    private VBox spendLogContainer;
    private Label regularSummaryLabel;
    private Label solarSummaryLabel;

    public ExperienceTab() {
        setFitToWidth(true);
        getStyleClass().add("scroll-pane-custom");
    }

    public void initialize() {
        setContent(createContent());
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private HBox createContent() {
        HBox root = new HBox(20);
        root.getStyleClass().add("content-area");
        root.setPadding(new Insets(20));

        root.getChildren().addAll(createAwardsPanel(), createSpendLogPanel());
        return root;
    }

    // Left panel: manual award entries
    private VBox createAwardsPanel() {
        VBox panel = new VBox(12);
        panel.setPrefWidth(380);
        panel.setMinWidth(300);

        Label title = new Label("XP Awards");
        title.getStyleClass().add("section-title");

        awardsListContainer = new VBox(8);
        awardsListContainer.getStyleClass().add("merit-row-container");

        viewModel
                .getAwards()
                .addListener((ListChangeListener<XpAwardRowViewModel>) c -> refreshAwardsList());
        refreshAwardsList();

        // Add-award form
        TextField descField = new TextField();
        descField.setPromptText("Description (e.g. \"Session 5\")");
        HBox.setHgrow(descField, Priority.ALWAYS);

        Spinner<Integer> amountSpinner = new Spinner<>(1, 999, 5);
        amountSpinner.setEditable(true);
        amountSpinner.setPrefWidth(75);

        CheckBox solarCheck = new CheckBox("Solar");
        solarCheck.setStyle("-fx-text-fill: #d4af37;");

        Button addBtn = new Button("+ Add");
        addBtn.getStyleClass().addAll("action-btn", "add-btn");
        addBtn.setOnAction(
                e -> {
                    viewModel.addAward(
                            descField.getText(), amountSpinner.getValue(), solarCheck.isSelected());
                    descField.clear();
                    amountSpinner.getValueFactory().setValue(5);
                    solarCheck.setSelected(false);
                });

        HBox addRow = new HBox(8, descField, amountSpinner, solarCheck, addBtn);
        addRow.setAlignment(Pos.CENTER_LEFT);

        // Totals summary
        regularSummaryLabel = new Label();
        regularSummaryLabel.setStyle("-fx-text-fill: white; -fx-font-size: 0.9em;");
        solarSummaryLabel = new Label();
        solarSummaryLabel.setStyle("-fx-text-fill: #d4af37; -fx-font-size: 0.9em;");

        updateSummaryLabels();
        viewModel
                .getAwards()
                .addListener((ListChangeListener<XpAwardRowViewModel>) c -> updateSummaryLabels());

        panel.getChildren()
                .addAll(
                        title,
                        awardsListContainer,
                        new Separator(),
                        addRow,
                        new HBox(20, regularSummaryLabel, solarSummaryLabel));
        return panel;
    }

    // Right panel: computed spend breakdown
    private VBox createSpendLogPanel() {
        VBox panel = new VBox(12);
        HBox.setHgrow(panel, Priority.ALWAYS);

        Label title = new Label("XP Spending Breakdown");
        title.getStyleClass().add("section-title");

        spendLogContainer = new VBox(5);
        spendLogContainer.getStyleClass().add("merit-row-container");

        ScrollPane logScroll = new ScrollPane(spendLogContainer);
        logScroll.setFitToWidth(true);
        logScroll.getStyleClass().add("scroll-pane-custom");
        VBox.setVgrow(logScroll, Priority.ALWAYS);

        viewModel
                .getSpendLog()
                .addListener((ListChangeListener<ExperiencePurchase>) c -> refreshSpendLog());
        refreshSpendLog();

        panel.getChildren().addAll(title, logScroll);
        return panel;
    }

    // ── Refresh helpers ───────────────────────────────────────────────────────

    private void refreshAwardsList() {
        if (awardsListContainer == null) return;
        awardsListContainer.getChildren().clear();

        for (XpAwardRowViewModel vm : viewModel.getAwards()) {
            HBox row = new HBox(10);
            row.getStyleClass().add("merit-row");
            row.setAlignment(Pos.CENTER_LEFT);

            String typeTag = vm.isSolar() ? " ☀ Solar" : " Regular";
            String textStyle = vm.isSolar() ? "-fx-text-fill: #d4af37;" : "-fx-text-fill: white;";

            Label descLabel = new Label(vm.getDescription());
            descLabel.getStyleClass().add("merit-name");
            HBox.setHgrow(descLabel, Priority.ALWAYS);

            Label amountLabel = new Label("+" + vm.getAmount() + " XP" + typeTag);
            amountLabel.setStyle(textStyle + " -fx-font-size: 0.9em;");

            Button delBtn = new Button("🗑");
            delBtn.getStyleClass().add("remove-btn");
            delBtn.setOnAction(e -> viewModel.removeAward(vm.getAward()));

            row.getChildren().addAll(descLabel, amountLabel, delBtn);
            awardsListContainer.getChildren().add(row);
        }

        if (viewModel.getAwards().isEmpty()) {
            Label empty = new Label("No XP awards recorded yet.");
            empty.setStyle("-fx-text-fill: #888; -fx-font-style: italic;");
            awardsListContainer.getChildren().add(empty);
        }
    }

    private void refreshSpendLog() {
        if (spendLogContainer == null) return;
        spendLogContainer.getChildren().clear();

        for (ExperiencePurchase purchase : viewModel.getSpendLog()) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(3, 8, 3, 8));

            String poolTag = purchase.canUseSolarXp() ? " (Solar eligible)" : " (Regular only)";

            Label descLabel = new Label(purchase.getDescription());
            descLabel.setStyle("-fx-text-fill: #ccc;");
            HBox.setHgrow(descLabel, Priority.ALWAYS);

            Label costLabel = new Label(purchase.getCost() + " XP" + poolTag);
            String costStyle =
                    purchase.canUseSolarXp()
                            ? "-fx-text-fill: #d4af37; -fx-font-size: 0.85em;"
                            : "-fx-text-fill: #aaa; -fx-font-size: 0.85em;";
            costLabel.setStyle(costStyle);

            row.getChildren().addAll(descLabel, costLabel);
            spendLogContainer.getChildren().add(row);
        }

        if (viewModel.getSpendLog().isEmpty()) {
            Label empty = new Label("No expenditures above creation baseline.");
            empty.setStyle("-fx-text-fill: #888; -fx-font-style: italic; -fx-padding: 8;");
            spendLogContainer.getChildren().add(empty);
        }

        // Totals footer row
        var status = viewModel.getExperienceStatus();
        Label totalReg =
                new Label(
                        String.format(
                                "Regular XP: %d spent / %d awarded",
                                status.regularXpSpent, status.totalRegularXpAwarded));
        totalReg.setStyle(
                "-fx-text-fill: "
                        + (status.getRegularXpRemaining() < 0 ? "red" : "white")
                        + "; -fx-padding: 6 0 0 8;");

        Label totalSol =
                new Label(
                        String.format(
                                "Solar XP: %d spent / %d awarded",
                                status.solarXpSpent, status.totalSolarXpAwarded));
        totalSol.setStyle(
                "-fx-text-fill: "
                        + (status.getSolarXpRemaining() < 0 ? "red" : "#d4af37")
                        + "; -fx-padding: 6 0 0 8;");

        spendLogContainer.getChildren().addAll(new Separator(), totalReg, totalSol);
    }

    private void updateSummaryLabels() {
        if (regularSummaryLabel == null) return;
        regularSummaryLabel.setText("Regular: " + viewModel.getTotalRegularXp() + " XP awarded");
        solarSummaryLabel.setText("Solar: " + viewModel.getTotalSolarXp() + " XP awarded");
    }
}
