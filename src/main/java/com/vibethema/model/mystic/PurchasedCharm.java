package com.vibethema.model.mystic;

public record PurchasedCharm(String id, String name, String ability, String sourceId) {
    public PurchasedCharm(String id, String name, String ability) {
        this(id, name, ability, null);
    }
}
