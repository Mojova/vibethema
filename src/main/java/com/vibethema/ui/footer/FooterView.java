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
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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

        HBox row1 = new HBox(20);
        row1.setAlignment(Pos.CENTER_LEFT);

        // Labels
        Label bpLabel = createLabel("BP");
        Label casteLabel = createLabel("Caste");
        Label favoredLabel = createLabel("Favored");
        Label attrLabel = createLabel("Attributes");
        Label abilitiesLabel = createLabel("Abilities");
        Label specialtiesLabel = createLabel("Specialties");
        Label charmsLabel = createLabel("Charms");
        Label regularXpLabel = createLabel("RegularXP");
        Label solarXpLabel = createLabel("SolarXP");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        finalizeBtn = new Button("Finalize Character");
        finalizeBtn.getStyleClass().add("action-btn");

        row1.getChildren()
                .addAll(
                        casteLabel,
                        favoredLabel,
                        attrLabel,
                        abilitiesLabel,
                        specialtiesLabel,
                        charmsLabel,
                        bpLabel,
                        regularXpLabel,
                        solarXpLabel,
                        spacer,
                        finalizeBtn);

        getChildren().add(row1);

        // Bindings
        FooterViewModel vm = viewModel;

        // Creation Mode bindings
        bpLabel.textProperty().bind(vm.bpSpentTextProperty());

        vm.bpStyleProperty()
                .addListener(
                        (obs, oldV, newV) -> {
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

        for (Label l :
                new Label[] {
                    bpLabel,
                    casteLabel,
                    favoredLabel,
                    attrLabel,
                    abilitiesLabel,
                    specialtiesLabel,
                    charmsLabel
                }) {
            l.visibleProperty().bind(vm.creationModeVisibleProperty());
            l.managedProperty().bind(l.visibleProperty());
        }

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
                .addListener(
                        (obs, oldV, newV) -> {
                            if (oldV != null) regularXpLabel.getStyleClass().remove(oldV);
                            if (newV != null) regularXpLabel.getStyleClass().add(newV);
                        });
        if (vm.regularXpStyleProperty().get() != null) {
            regularXpLabel.getStyleClass().add(vm.regularXpStyleProperty().get());
        }

        solarXpLabel.textProperty().bind(vm.solarXpTextProperty());
        vm.solarXpStyleProperty()
                .addListener(
                        (obs, oldV, newV) -> {
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
