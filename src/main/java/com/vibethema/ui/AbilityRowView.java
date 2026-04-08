package com.vibethema.ui;

import com.vibethema.model.*;
import com.vibethema.viewmodel.stats.AbilityRowViewModel;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/** UI row for an ability, bound to an AbilityRowViewModel. */
public class AbilityRowView extends HBox {
    private final Label nameLabel;
    private final DotSelector selector;

    public AbilityRowView(AbilityRowViewModel viewModel) {
        setSpacing(6);
        setAlignment(Pos.CENTER_LEFT);

        // 1. Caste Indicator/Checkbox
        CheckBox casteBox = new CheckBox("C");
        casteBox.getStyleClass().add("caste-checkbox");
        casteBox.selectedProperty().bindBidirectional(viewModel.isCaste());
        casteBox.disableProperty().bind(viewModel.casteBoxDisabledProperty());

        Label casteLabel = new Label("C");
        casteLabel.getStyleClass().add("caste-marker");
        casteLabel.setStyle(
                "-fx-text-fill: #d4af37; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 0"
                        + " 4 0 4;");

        // Visibility bindings for Caste
        casteBox.visibleProperty().bind(viewModel.modeProperty().isEqualTo(CharacterMode.CREATION));
        casteLabel
                .visibleProperty()
                .bind(
                        Bindings.and(
                                viewModel.modeProperty().isEqualTo(CharacterMode.EXPERIENCED),
                                viewModel.isCaste()));
        casteLabel.managedProperty().bind(casteLabel.visibleProperty());

        StackPane casteContainer = new StackPane(casteBox);
        casteContainer.setMinWidth(25);
        casteContainer
                .managedProperty()
                .bind(viewModel.modeProperty().isEqualTo(CharacterMode.CREATION));
        casteContainer.visibleProperty().bind(casteContainer.managedProperty());

        // 2. Favored Indicator/Checkbox
        CheckBox favoredBox = new CheckBox("F");
        favoredBox.getStyleClass().add("favored-checkbox");
        favoredBox.selectedProperty().bindBidirectional(viewModel.isFavored());
        favoredBox.disableProperty().bind(viewModel.favoredBoxDisabledProperty());

        Label favoredLabel = new Label("F");
        favoredLabel.getStyleClass().add("favored-marker");
        favoredLabel.setStyle(
                "-fx-text-fill: #3498db; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 0"
                        + " 4 0 4;");

        // Visibility bindings for Favored
        favoredBox
                .visibleProperty()
                .bind(viewModel.modeProperty().isEqualTo(CharacterMode.CREATION));
        favoredLabel
                .visibleProperty()
                .bind(
                        Bindings.and(
                                viewModel.modeProperty().isEqualTo(CharacterMode.EXPERIENCED),
                                viewModel.isFavored()));
        favoredLabel.managedProperty().bind(favoredLabel.visibleProperty());

        StackPane favoredContainer = new StackPane(favoredBox);
        favoredContainer.setMinWidth(25);
        favoredContainer
                .managedProperty()
                .bind(viewModel.modeProperty().isEqualTo(CharacterMode.CREATION));
        favoredContainer.visibleProperty().bind(favoredContainer.managedProperty());

        // Experienced Mode Container (C/F labels)
        StackPane expModeContainer = new StackPane(casteLabel, favoredLabel);
        expModeContainer.setMinWidth(25);
        expModeContainer.setAlignment(Pos.CENTER);
        expModeContainer
                .managedProperty()
                .bind(viewModel.modeProperty().isEqualTo(CharacterMode.EXPERIENCED));
        expModeContainer.visibleProperty().bind(expModeContainer.managedProperty());

        // 3. Ability Name Label
        nameLabel = new Label();
        nameLabel.setPrefWidth(95);
        nameLabel
                .textProperty()
                .bind(
                        Bindings.when(viewModel.isExcellencyAvailableProperty())
                                .then(viewModel.baseDisplayNameProperty().concat(" [E]"))
                                .otherwise(viewModel.baseDisplayNameProperty()));

        // Caste highlight style
        viewModel.isOptionProperty().addListener((obs, oldV, newV) -> updateCasteOptionStyle(newV));
        updateCasteOptionStyle(viewModel.isOptionProperty().get());

        // Excellency style
        viewModel
                .isExcellencyAvailableProperty()
                .addListener((obs, oldV, newV) -> updateExcellencyStyle(newV));
        updateExcellencyStyle(viewModel.isExcellencyAvailableProperty().get());

        // 4. Dot Selector
        selector = new DotSelector(viewModel.ratingProperty(), 0);
        selector.minDotsProperty().bind(viewModel.minDotsProperty());
        selector.contextIdProperty().set("Stats");
        selector.descriptionProperty().set("Change " + viewModel.getAbility().getDisplayName());
        String id = "ability_" + viewModel.getAbility().name().toLowerCase();
        selector.targetIdProperty().set(id);
        selector.setId(id);

        getChildren()
                .addAll(casteContainer, favoredContainer, expModeContainer, nameLabel, selector);
    }

    private void updateCasteOptionStyle(boolean isOption) {
        if (isOption) {
            if (!nameLabel.getStyleClass().contains("caste-option"))
                nameLabel.getStyleClass().add("caste-option");
        } else {
            nameLabel.getStyleClass().remove("caste-option");
        }
    }

    private void updateExcellencyStyle(boolean isExcellency) {
        if (isExcellency) {
            if (!nameLabel.getStyleClass().contains("excellency-label"))
                nameLabel.getStyleClass().add("excellency-label");
        } else {
            nameLabel.getStyleClass().remove("excellency-label");
        }
    }

    public void setOnNameClick(
            javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> value) {
        nameLabel.setCursor(javafx.scene.Cursor.HAND);
        nameLabel.setOnMouseClicked(value);
    }

    public void setNameWidth(double width) {
        nameLabel.setPrefWidth(width);
        nameLabel.setMinWidth(width);
    }

    public DotSelector getSelector() {
        return selector;
    }
}
