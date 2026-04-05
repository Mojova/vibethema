package com.vibethema.ui;

import com.vibethema.model.Ability;
import com.vibethema.model.CharacterData;
import com.vibethema.model.CharacterMode;
import com.vibethema.model.Caste;
import com.vibethema.model.SystemData;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class AbilitySelectionComponent extends HBox {

    public AbilitySelectionComponent(CharacterData data, Ability ability, IntegerProperty rating, BooleanProperty caste, BooleanProperty favored) {
        this(data, ability.getDisplayName(), rating, caste, favored, ability);
    }

    public AbilitySelectionComponent(CharacterData data, String displayName, IntegerProperty rating, BooleanProperty caste, BooleanProperty favored, Ability ability) {
        setSpacing(6);
        setAlignment(Pos.CENTER_LEFT);

        // 1. Caste Indicator/Checkbox
        CheckBox casteBox = new CheckBox("C");
        casteBox.getStyleClass().add("caste-checkbox");
        casteBox.selectedProperty().bindBidirectional(caste);
        
        Label casteLabel = new Label("C");
        casteLabel.getStyleClass().add("caste-marker");
        casteLabel.setStyle("-fx-text-fill: #d4af37; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 0 4 0 4;");
        
        // Visibility bindings
        casteBox.visibleProperty().bind(data.modeProperty().isEqualTo(CharacterMode.CREATION));
        casteLabel.visibleProperty().bind(Bindings.and(data.modeProperty().isEqualTo(CharacterMode.EXPERIENCED), caste));
        casteLabel.managedProperty().bind(casteLabel.visibleProperty());

        // Disable logic for CheckBox (Move from BuilderUI)
        if (ability != null) {
            casteBox.disableProperty().bind(Bindings.createBooleanBinding(() -> {
                if (Ability.MARTIAL_ARTS == ability) return true; // Synced with Brawl
                Caste c = data.casteProperty().get();
                boolean notInCasteList = c == null || c == Caste.NONE || !SystemData.CASTE_OPTIONS.get(c).contains(ability);
                boolean atLimit = data.casteAbilityCountProperty().get() >= 5 && !caste.get();
                return (notInCasteList || atLimit) && !caste.get();
            }, data.casteProperty(), data.casteAbilityCountProperty(), caste));
        }

        StackPane casteContainer = new StackPane(casteBox);
        casteContainer.setMinWidth(25);
        casteContainer.managedProperty().bind(data.modeProperty().isEqualTo(CharacterMode.CREATION));
        casteContainer.visibleProperty().bind(casteContainer.managedProperty());
        
        // 2. Favored Indicator/Checkbox
        CheckBox favoredBox = new CheckBox("F");
        favoredBox.getStyleClass().add("favored-checkbox");
        favoredBox.selectedProperty().bindBidirectional(favored);
        
        Label favoredLabel = new Label("F");
        favoredLabel.getStyleClass().add("favored-marker");
        favoredLabel.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 0 4 0 4;");
        
        favoredBox.visibleProperty().bind(data.modeProperty().isEqualTo(CharacterMode.CREATION));
        favoredLabel.visibleProperty().bind(Bindings.and(data.modeProperty().isEqualTo(CharacterMode.EXPERIENCED), favored));
        favoredLabel.managedProperty().bind(favoredLabel.visibleProperty());

        if (ability != null) {
            favoredBox.disableProperty().bind(Bindings.createBooleanBinding(() -> {
                if (Ability.MARTIAL_ARTS == ability) return true; // Synced with Brawl
                boolean isCaste = caste.get();
                boolean atLimit = data.favoredAbilityCountProperty().get() >= 5 && !favored.get();
                return (isCaste || atLimit) && !favored.get();
            }, caste, data.favoredAbilityCountProperty(), favored));
        }

        StackPane favoredContainer = new StackPane(favoredBox);
        favoredContainer.setMinWidth(25);
        favoredContainer.managedProperty().bind(data.modeProperty().isEqualTo(CharacterMode.CREATION));
        favoredContainer.visibleProperty().bind(favoredContainer.managedProperty());

        StackPane expModeContainer = new StackPane(casteLabel, favoredLabel);
        expModeContainer.setMinWidth(25);
        expModeContainer.setAlignment(Pos.CENTER);
        expModeContainer.managedProperty().bind(data.modeProperty().isEqualTo(CharacterMode.EXPERIENCED));
        expModeContainer.visibleProperty().bind(expModeContainer.managedProperty());
        
        // 3. Ability Name Label
        Label nameLabel = new Label(displayName);
        nameLabel.setPrefWidth(95);
        if (ability != null) {
            data.casteProperty().addListener((obs, oldV, newV) -> {
                boolean isOption = newV != null && newV != Caste.NONE && SystemData.CASTE_OPTIONS.get(newV).contains(ability);
                if (isOption) {
                    if (!nameLabel.getStyleClass().contains("caste-option")) nameLabel.getStyleClass().add("caste-option");
                } else {
                    nameLabel.getStyleClass().remove("caste-option");
                }
            });
            // Initial check
            Caste currentCaste = data.casteProperty().get();
            if (currentCaste != null && currentCaste != Caste.NONE && SystemData.CASTE_OPTIONS.get(currentCaste).contains(ability)) {
                nameLabel.getStyleClass().add("caste-option");
            }

            // Excellency Indicator
            Runnable updateExcellency = () -> {
                if (data.hasExcellency(ability)) {
                    nameLabel.setText(displayName + " [E]");
                    if (!nameLabel.getStyleClass().contains("excellency-label")) {
                        nameLabel.getStyleClass().add("excellency-label");
                    }
                } else {
                    nameLabel.setText(displayName);
                    nameLabel.getStyleClass().remove("excellency-label");
                }
            };
            rating.addListener((obs, ov, nv) -> updateExcellency.run());
            caste.addListener((obs, ov, nv) -> updateExcellency.run());
            favored.addListener((obs, ov, nv) -> updateExcellency.run());
            data.getUnlockedCharms().addListener((javafx.collections.ListChangeListener<? super com.vibethema.model.PurchasedCharm>) c -> updateExcellency.run());
            updateExcellency.run();
        }
        
        // 4. Dot Selector
        DotSelector selector = new DotSelector(rating, 0);
        selector.minDotsProperty().bind(Bindings.when(favored).then(1).otherwise(0));

        getChildren().addAll(casteContainer, favoredContainer, expModeContainer, nameLabel, selector);
    }

    public void setOnNameClick(javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> value) {
        javafx.scene.Node nameNode = getChildren().get(3);
        if (nameNode instanceof Label) {
            nameNode.setCursor(javafx.scene.Cursor.HAND);
            nameNode.setOnMouseClicked(value);
        }
    }

    public void setNameWidth(double width) {
        javafx.scene.Node nameNode = getChildren().get(3);
        if (nameNode instanceof Label l) {
            l.setPrefWidth(width);
            l.setMinWidth(width);
        }
    }

    public DotSelector getSelector() {
        return (DotSelector) getChildren().get(4);
    }
}
