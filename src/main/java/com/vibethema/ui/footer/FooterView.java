package com.vibethema.ui.footer;

import com.vibethema.viewmodel.footer.FooterViewModel;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public class FooterView extends VBox implements JavaView<FooterViewModel>, Initializable {

    @InjectViewModel private FooterViewModel viewModel;

    private final Map<String, Label> labels = new java.util.HashMap<>();
    private Button finalizeBtn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        getStyleClass().add("footer");
        setPadding(new Insets(10));
        setSpacing(5);

        FlowPane row1 = new FlowPane(20, 5);
        row1.setAlignment(Pos.CENTER_LEFT);

        // Labels
        Label bpLabel = createLabel("BP");
        Label casteLabel = createLabel("Caste");
        Label favoredLabel = createLabel("Favored");
        Label attrLabel = createLabel("Attributes");
        Label abilitiesLabel = createLabel("Abilities");
        Label specialtiesLabel = createLabel("Specialties");
        Label charmsLabel = createLabel("Charms");
        Label meritsLabel = createLabel("Merits");
        Label regularXpLabel = createLabel("RegularXP");
        Label solarXpLabel = createLabel("SolarXP");

        finalizeBtn = new Button("Finalize Character");
        finalizeBtn.getStyleClass().add("action-btn");

        row1.getChildren()
                .addAll(
                        casteLabel,
                        favoredLabel,
                        attrLabel,
                        abilitiesLabel,
                        specialtiesLabel,
                        meritsLabel,
                        charmsLabel,
                        bpLabel,
                        regularXpLabel,
                        solarXpLabel,
                        finalizeBtn);

        getChildren().add(row1);

        // Bindings
        FooterViewModel vm = viewModel;

        // Creation Mode bindings
        bpLabel.textProperty().bind(vm.bpSpentTextProperty());

        vm.bpStyleProperty()
                .addListener((obs, oldV, newV) -> {
                    if (oldV != null) bpLabel.getStyleClass().remove(oldV);
                    if (newV != null) bpLabel.getStyleClass().add(newV);
                });
        if (vm.bpStyleProperty().get() != null) {
            bpLabel.getStyleClass().add(vm.bpStyleProperty().get());
        }
        casteLabel.textProperty().bind(vm.casteTextProperty());
        favoredLabel.textProperty().bind(vm.favoredTextProperty());
        attrLabel.textProperty().bind(vm.attrTextProperty());
        abilitiesLabel.textProperty().bind(vm.abilitiesTextProperty());
        specialtiesLabel.textProperty().bind(vm.specialtiesTextProperty());
        charmsLabel.textProperty().bind(vm.charmsTextProperty());
        meritsLabel.textProperty().bind(vm.meritsTextProperty());

        for (Label l :
                new Label[] {
                    bpLabel,
                    casteLabel,
                    favoredLabel,
                    attrLabel,
                    abilitiesLabel,
                    specialtiesLabel,
                    meritsLabel,
                    charmsLabel
                }) {
            l.visibleProperty().bind(vm.creationModeVisibleProperty());
            l.managedProperty().bind(l.visibleProperty());
        }

        VBox checklist = new VBox(4);
        checklist.setPadding(new Insets(8));
        Runnable refreshChecklist =
                () -> {
                    checklist.getChildren().clear();
                    if (vm.getValidationIssues().isEmpty()) {
                        checklist
                                .getChildren()
                                .add(new Label("All automatic creation checks passed."));
                    } else {
                        for (var issue : vm.getValidationIssues()) {
                            Hyperlink link = new Hyperlink(issue.message());
                            link.setWrapText(true);
                            link.setMaxWidth(Double.MAX_VALUE);
                            link.setAccessibleText(issue.message() + " Open the relevant field.");
                            link.setOnAction(event -> vm.navigateToIssue(issue));
                            checklist.getChildren().add(link);
                        }
                    }
                    Label reminder =
                            new Label(
                                    "Review your Intimacies with your Storyteller: include at least"
                                            + " one positive and one negative Intimacy.");
                    reminder.setWrapText(true);
                    checklist.getChildren().add(reminder);
                };
        vm.getValidationIssues()
                .addListener(
                        (javafx.collections.ListChangeListener<
                                        com.vibethema.model.logic.CreationValidator.Issue>)
                                change -> refreshChecklist.run());
        refreshChecklist.run();
        ScrollPane checklistScroll = new ScrollPane(checklist);
        checklistScroll.setFitToWidth(true);
        checklistScroll.setPrefViewportHeight(130);
        TitledPane checklistPane = new TitledPane();
        checklistPane.textProperty().bind(vm.validationTitleProperty());
        checklistPane.setContent(checklistScroll);
        checklistPane.setExpanded(false);
        checklistPane.visibleProperty().bind(vm.creationModeVisibleProperty());
        checklistPane.managedProperty().bind(checklistPane.visibleProperty());
        getChildren().add(checklistPane);

        // Finalize Button bindings
        finalizeBtn.visibleProperty().bind(vm.creationModeVisibleProperty());
        finalizeBtn.managedProperty().bind(finalizeBtn.visibleProperty());
        finalizeBtn.disableProperty().bind(vm.finalizeDisabledProperty());
        finalizeBtn
                .tooltipProperty()
                .bind(
                        Bindings.createObjectBinding(
                                () -> new Tooltip(vm.finalizeTooltipProperty().get()),
                                vm.finalizeTooltipProperty()));
        finalizeBtn.setOnAction(e -> vm.finalizeCharacter());

        // Experience Mode bindings
        regularXpLabel.textProperty().bind(vm.regularXpTextProperty());
        vm.regularXpStyleProperty()
                .addListener((obs, oldV, newV) -> {
                    if (oldV != null) regularXpLabel.getStyleClass().remove(oldV);
                    if (newV != null) regularXpLabel.getStyleClass().add(newV);
                });
        if (vm.regularXpStyleProperty().get() != null) {
            regularXpLabel.getStyleClass().add(vm.regularXpStyleProperty().get());
        }

        solarXpLabel.textProperty().bind(vm.solarXpTextProperty());
        vm.solarXpStyleProperty()
                .addListener((obs, oldV, newV) -> {
                    if (oldV != null) solarXpLabel.getStyleClass().remove(oldV);
                    if (newV != null) solarXpLabel.getStyleClass().add(newV);
                });
        if (vm.solarXpStyleProperty().get() != null) {
            solarXpLabel.getStyleClass().add(vm.solarXpStyleProperty().get());
        }

        for (Label l : new Label[] {regularXpLabel, solarXpLabel}) {
            l.visibleProperty().bind(vm.experiencedModeVisibleProperty());
            l.managedProperty().bind(l.visibleProperty());
        }
    }

    private static final Map<String, Double> LABEL_WIDTHS =
            Map.of(
                    "BP", 130.0,
                    "Caste", 85.0,
                    "Favored", 105.0,
                    "Attributes", 145.0,
                    "Abilities", 115.0,
                    "Specialties", 115.0,
                    "Charms", 105.0,
                    "Merits", 100.0,
                    "RegularXP", 160.0,
                    "SolarXP", 160.0);

    private Label createLabel(String id) {
        Label l = new Label();
        l.setMinWidth(LABEL_WIDTHS.getOrDefault(id, 100.0));
        l.setPrefWidth(LABEL_WIDTHS.getOrDefault(id, 100.0));
        l.setAlignment(Pos.CENTER_LEFT);
        labels.put(id, l);
        return l;
    }
}
