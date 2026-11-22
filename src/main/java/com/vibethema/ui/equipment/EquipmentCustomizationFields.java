package com.vibethema.ui.equipment;

import java.util.Locale;
import java.util.Map;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;

final class EquipmentCustomizationFields {
    private EquipmentCustomizationFields() {}

    static <S extends Enum<S>> void addTo(
            GridPane form,
            int row,
            StringProperty description,
            S[] stats,
            Map<S, Integer> overrides) {
        TextArea notes = new TextArea();
        notes.setWrapText(true);
        notes.setPrefRowCount(3);
        notes.textProperty().bindBidirectional(description);
        Label descriptionLabel = new Label("Description");
        descriptionLabel.setLabelFor(notes);
        form.addRow(row, descriptionLabel, notes);

        GridPane fields = new GridPane(8, 4);
        for (int i = 0; i < stats.length; i++) {
            S stat = stats[i];
            String name = stat.name().toLowerCase(Locale.ROOT).replace('_', ' ');
            Label label = new Label(Character.toUpperCase(name.charAt(0)) + name.substring(1));
            TextField value = new TextField();
            value.setPrefColumnCount(4);
            value.setPromptText("Default");
            if (overrides.containsKey(stat)) value.setText(overrides.get(stat).toString());
            value.setTextFormatter(
                    new TextFormatter<>(change ->
                            change.getControlNewText().matches("-?[0-9]{0,4}") ? change : null));
            value.textProperty()
                    .addListener((obs, old, text) -> {
                        if (text.isEmpty() || text.equals("-")) overrides.remove(stat);
                        else overrides.put(stat, Integer.parseInt(text));
                    });
            label.setLabelFor(value);
            int column = i % 2 * 2;
            fields.add(label, column, i / 2);
            fields.add(value, column + 1, i / 2);
        }
        form.addRow(row + 1, new Label("Stat overrides"), fields);
    }
}
