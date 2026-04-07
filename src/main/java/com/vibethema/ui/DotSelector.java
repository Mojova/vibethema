package com.vibethema.ui;

import com.vibethema.viewmodel.util.Messenger;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

public class DotSelector extends HBox {
    private final int maxDots;
    private final IntegerProperty valueProperty;
    private final Circle[] dots;
    private final IntegerProperty minDotsProperty;

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
        getStyleClass().add("dot-selector");
        
        for (int i = 0; i < maxDots; i++) {
            Circle dot = new Circle(6); // Radius 6
            dot.getStyleClass().add("dot");
            final int dotIndex = i + 1;
            
            dot.setOnMouseClicked(e -> {
                int currentMin = minDotsProperty.get();
                if (valueProperty.get() == dotIndex && dotIndex > currentMin) {
                    // Clicked the current max dot, decrease by 1
                    valueProperty.set(dotIndex - 1);
                    Messenger.publish("refresh_all_ui");
                } else if (dotIndex >= currentMin) {
                    valueProperty.set(dotIndex);
                    Messenger.publish("refresh_all_ui");
                }
            });
            
            dots[i] = dot;
            getChildren().add(dot);
        }
        
        valueProperty.addListener((obs, oldVal, newVal) -> updateDots());
        minDotsProperty.addListener((obs, oldVal, newVal) -> {
            if (valueProperty.get() < newVal.intValue()) {
                valueProperty.set(newVal.intValue());
            }
            updateDots();
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
}
