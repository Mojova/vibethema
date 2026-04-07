package com.vibethema.ui;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.social.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.logic.*;


import com.vibethema.viewmodel.equipment.AttackPoolRowViewModel;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * UI row for an attack pool, bound to an AttackPoolRowViewModel.
 */
public class AttackPoolRowView extends HBox {
    public AttackPoolRowView(AttackPoolRowViewModel viewModel) {
        setSpacing(15);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("attack-pool-row");
        
        Label status = new Label();
        status.textProperty().bind(viewModel.statusProperty());
        status.getStyleClass().add("status-marker");
        status.setPrefWidth(20);
        
        Label name = new Label();
        name.textProperty().bind(viewModel.nameProperty());
        name.getStyleClass().add("weapon-name");
        name.setPrefWidth(120);
        
        Label withering = new Label();
        withering.textProperty().bind(viewModel.witheringProperty());
        withering.setPrefWidth(100);
        
        Label decisive = new Label();
        decisive.textProperty().bind(viewModel.decisiveProperty());
        decisive.setPrefWidth(60);
        
        Label damage = new Label();
        damage.textProperty().bind(viewModel.damageProperty());
        damage.setPrefWidth(60);
        
        Label parry = new Label();
        parry.textProperty().bind(viewModel.parryProperty());
        parry.setPrefWidth(50);
        
        getChildren().addAll(status, name, withering, decisive, damage, parry);
    }
}