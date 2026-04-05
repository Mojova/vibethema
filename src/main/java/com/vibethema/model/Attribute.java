package com.vibethema.model;

public enum Attribute {
    STRENGTH(Category.PHYSICAL, "Strength"),
    DEXTERITY(Category.PHYSICAL, "Dexterity"),
    STAMINA(Category.PHYSICAL, "Stamina"),
    
    CHARISMA(Category.SOCIAL, "Charisma"),
    MANIPULATION(Category.SOCIAL, "Manipulation"),
    APPEARANCE(Category.SOCIAL, "Appearance"),
    
    PERCEPTION(Category.MENTAL, "Perception"),
    INTELLIGENCE(Category.MENTAL, "Intelligence"),
    WITS(Category.MENTAL, "Wits");

    public enum Category {
        PHYSICAL, SOCIAL, MENTAL
    }

    private final Category category;
    private final String displayName;

    Attribute(Category category, String displayName) {
        this.category = category;
        this.displayName = displayName;
    }

    public Category getCategory() { return category; }
    public String getDisplayName() { return displayName; }

    public static Attribute fromString(String name) {
        for (Attribute a : values()) {
            if (a.displayName.equalsIgnoreCase(name.trim())) return a;
        }
        return null;
    }
}
