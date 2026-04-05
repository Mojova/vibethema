package com.vibethema.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.util.UUID;

public class XpAward {
    private final String id;
    private final StringProperty description;
    private final IntegerProperty amount;
    private final BooleanProperty isSolar;

    public XpAward() {
        this("", 0, false);
    }

    public XpAward(String description, int amount, boolean isSolar) {
        this(UUID.randomUUID().toString(), description, amount, isSolar);
    }

    public XpAward(String id, String description, int amount, boolean isSolar) {
        this.id = id != null && !id.isEmpty() ? id : UUID.randomUUID().toString();
        this.description = new SimpleStringProperty(description != null ? description : "");
        this.amount = new SimpleIntegerProperty(amount);
        this.isSolar = new SimpleBooleanProperty(isSolar);
    }

    public String getId() { return id; }
    
    public String getDescription() { return description.get(); }
    public void setDescription(String val) { description.set(val); }
    public StringProperty descriptionProperty() { return description; }

    public int getAmount() { return amount.get(); }
    public void setAmount(int val) { amount.set(val); }
    public IntegerProperty amountProperty() { return amount; }

    public boolean isSolar() { return isSolar.get(); }
    public void setSolar(boolean val) { isSolar.set(val); }
    public BooleanProperty isSolarProperty() { return isSolar; }
}
