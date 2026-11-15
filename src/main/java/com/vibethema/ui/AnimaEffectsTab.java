package com.vibethema.ui;

import com.vibethema.model.mystic.AnimaAbility;
import com.vibethema.viewmodel.AnimaEffectsViewModel;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.ListChangeListener;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

public class AnimaEffectsTab extends ScrollPane
        implements JavaView<AnimaEffectsViewModel>, Initializable {
    @InjectViewModel private AnimaEffectsViewModel viewModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setFitToWidth(true);
        getStyleClass().add("scroll-pane-custom");
        setAccessibleText("Anima Effects");
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.getStyleClass().add("content-area");
        Label message = text("");
        message.textProperty().bind(viewModel.messageProperty());
        message.visibleProperty().bind(viewModel.messageProperty().isNotEmpty());
        message.managedProperty().bind(message.visibleProperty());
        VBox groups = new VBox(24);
        groups.setMaxWidth(640);
        content.getChildren().addAll(message, groups);
        setContent(content);
        viewModel
                .getAbilities()
                .addListener((ListChangeListener<AnimaAbility>) change -> render(groups));
        render(groups);
    }

    private void render(VBox groups) {
        groups.getChildren().clear();
        for (var ability : viewModel.getAbilities()) {
            VBox section = new VBox(12);
            Label title = text(ability.name());
            title.getStyleClass().add("section-title");
            section.getChildren().add(title);
            if (!ability.description().isBlank())
                section.getChildren().add(text(ability.description()));
            for (var level : ability.levels()) {
                Label levelTitle = text(level.name());
                levelTitle.getStyleClass().add("subsection-title");
                section.getChildren().addAll(levelTitle, text(level.description()));
            }
            for (String effect : ability.effects()) section.getChildren().add(text("• " + effect));
            groups.getChildren().add(section);
        }
    }

    private Label text(String value) {
        Label label = new Label(value);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMinWidth(0);
        return label;
    }
}
