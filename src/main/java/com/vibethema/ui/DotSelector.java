package com.vibethema.ui;

import com.vibethema.viewmodel.MainViewModel.CheckpointRequest;
import com.vibethema.viewmodel.util.Messenger;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

public class DotSelector extends HBox {
    private final int maxDots;
    private final IntegerProperty valueProperty;
    private final Circle[] dots;
    private final IntegerProperty minDotsProperty;
    private final StringProperty contextId = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty targetId = new SimpleStringProperty();

    public DotSelector(IntegerProperty valueProperty, int minDots) {
        this(valueProperty, minDots, 5);
    }

    public DotSelector(IntegerProperty valueProperty, int minDots, int maxDots) {
        this.valueProperty = valueProperty;
        this.minDotsProperty = new SimpleIntegerProperty(minDots);
        this.maxDots = maxDots;
        this.dots = new Circle[maxDots];

        setSpacing(4);
        setAlignment(Pos.CENTER_LEFT);
        setFocusTraversable(true);
        setAccessibleRole(AccessibleRole.SPINNER);
        setAccessibleRoleDescription("dots");
        getStyleClass().add("dot-selector");

        for (int i = 0; i < maxDots; i++) {
            Circle dot = new Circle(6); // Radius 6
            dot.getStyleClass().add("dot");
            final int dotIndex = i + 1;

            dot.setOnMouseClicked(
                    e -> {
                        int currentMin = minDotsProperty.get();
                        int oldVal = valueProperty.get();
                        int newVal = -1;
                        if (oldVal == dotIndex && dotIndex > currentMin) {
                            newVal = dotIndex - 1;
                        } else if (dotIndex >= currentMin) {
                            newVal = dotIndex;
                        }

                        if (newVal != -1 && newVal != oldVal) {
                            triggerCheckpoint();
                            valueProperty.set(newVal);
                        }
                    });

            dots[i] = dot;
            getChildren().add(dot);
        }

        // Keyboard navigation
        setOnKeyPressed(
                e -> {
                    if (e.getCode() == KeyCode.RIGHT || e.getCode() == KeyCode.UP) {
                        if (valueProperty.get() < maxDots) {
                            triggerCheckpoint();
                            valueProperty.set(valueProperty.get() + 1);
                        }
                        e.consume();
                    } else if (e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.DOWN) {
                        if (valueProperty.get() > minDotsProperty.get()) {
                            triggerCheckpoint();
                            valueProperty.set(valueProperty.get() - 1);
                        }
                        e.consume();
                    }
                });

        minDotsProperty.addListener(
                (obs, oldVal, newVal) -> {
                    if (valueProperty.get() < newVal.intValue()) {
                        valueProperty.set(newVal.intValue());
                    }
                    updateDots();
                });

        accessibleTextProperty()
                .bind(
                        Bindings.createStringBinding(
                                () -> (description.get() != null ? description.get() : "Dots"),
                                description));

        valueProperty.addListener(
                (obs, oldVal, newVal) -> {
                    updateDots();
                    notifyAccessibleAttributeChanged(AccessibleAttribute.VALUE);
                });

        updateDots();
    }

    public IntegerProperty minDotsProperty() {
        return minDotsProperty;
    }

    private void updateDots() {
        int currentVal = valueProperty.get();
        for (int i = 0; i < maxDots; i++) {
            if (i < currentVal) {
                if (!dots[i].getStyleClass().contains("filled")) {
                    dots[i].getStyleClass().add("filled");
                }
            } else {
                dots[i].getStyleClass().remove("filled");
            }
        }
    }

    private void triggerCheckpoint() {
        if (contextId.get() != null) {
            Messenger.publish(
                    "RECORD_UNDO_CHECKPOINT",
                    new CheckpointRequest(contextId.get(), description.get(), targetId.get()));
        }
    }

    public StringProperty contextIdProperty() {
        return contextId;
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public StringProperty targetIdProperty() {
        return targetId;
    }

    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case VALUE:
                return (double) valueProperty.get();
            case VALUE_STRING:
                return String.valueOf(valueProperty.get());
            case MIN_VALUE:
                return (double) minDotsProperty.get();
            case MAX_VALUE:
                return (double) maxDots;
            case TEXT:
                return String.valueOf(valueProperty.get());
            case HELP:
                return description.get();
            default:
                return super.queryAccessibleAttribute(attribute, parameters);
        }
    }

    @Override
    public void executeAccessibleAction(AccessibleAction action, Object... parameters) {
        switch (action) {
            case INCREMENT:
                if (valueProperty.get() < maxDots) {
                    triggerCheckpoint();
                    valueProperty.set(valueProperty.get() + 1);
                }
                break;
            case DECREMENT:
                if (valueProperty.get() > minDotsProperty.get()) {
                    triggerCheckpoint();
                    valueProperty.set(valueProperty.get() - 1);
                }
                break;
            default:
                super.executeAccessibleAction(action, parameters);
        }
    }
}
