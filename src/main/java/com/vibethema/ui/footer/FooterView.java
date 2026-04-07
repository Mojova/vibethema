package com.vibethema.ui.footer;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.social.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.logic.*;


import com.vibethema.viewmodel.footer.FooterViewModel;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
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
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

public class FooterView extends VBox implements JavaView<FooterViewModel>, Initializable {

    @InjectViewModel
    private FooterViewModel viewModel;

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
        finalizeBtn.setStyle("-fx-background-color: #d4af37; -fx-text-fill: black; -fx-font-weight: bold;");

        row1.getChildren().addAll(
                casteLabel, favoredLabel, attrLabel, abilitiesLabel, 
                specialtiesLabel, charmsLabel, bpLabel, 
                regularXpLabel, solarXpLabel, 
                spacer, finalizeBtn
        );

        getChildren().add(row1);

        // Bindings
        FooterViewModel vm = viewModel;

        // Creation Mode bindings
        bpLabel.textProperty().bind(vm.bpSpentTextProperty());
        bpLabel.styleProperty().bind(vm.bpStyleProperty());
        casteLabel.textProperty().bind(vm.casteTextProperty());
        favoredLabel.textProperty().bind(vm.favoredTextProperty());
        attrLabel.textProperty().bind(vm.attrTextProperty());
        abilitiesLabel.textProperty().bind(vm.abilitiesTextProperty());
        specialtiesLabel.textProperty().bind(vm.specialtiesTextProperty());
        charmsLabel.textProperty().bind(vm.charmsTextProperty());

        for (Label l : new Label[]{bpLabel, casteLabel, favoredLabel, attrLabel, abilitiesLabel, specialtiesLabel, charmsLabel}) {
            l.visibleProperty().bind(vm.creationModeVisibleProperty());
            l.managedProperty().bind(l.visibleProperty());
        }

        // Finalize Button bindings
        finalizeBtn.visibleProperty().bind(vm.creationModeVisibleProperty());
        finalizeBtn.managedProperty().bind(finalizeBtn.visibleProperty());
        finalizeBtn.disableProperty().bind(vm.finalizeDisabledProperty());
        finalizeBtn.tooltipProperty().bind(Bindings.createObjectBinding(() -> new Tooltip(vm.finalizeTooltipProperty().get()), vm.finalizeTooltipProperty()));
        finalizeBtn.setOnAction(e -> vm.finalizeCharacter());

        // Experience Mode bindings
        regularXpLabel.textProperty().bind(vm.regularXpTextProperty());
        regularXpLabel.styleProperty().bind(vm.regularXpStyleProperty());
        solarXpLabel.textProperty().bind(vm.solarXpTextProperty());
        solarXpLabel.styleProperty().bind(vm.solarXpStyleProperty());

        for (Label l : new Label[]{regularXpLabel, solarXpLabel}) {
            l.visibleProperty().bind(vm.experiencedModeVisibleProperty());
            l.managedProperty().bind(l.visibleProperty());
        }
    }

    private Label createLabel(String id) {
        Label l = new Label();
        labels.put(id, l);
        return l;
    }
}