package com.example.spendwise.model;

public enum Category {
    FOOD("Food"),
    TRANSPORT("Transport"),
    ENTERTAINMENT("Entertainment"),
    BILLS("Bills"),
    SHOPPING("Shopping"),
    HEALTH("Health"),
    OTHER("Other");

    private String displayName;

    // Constructor for enum values
    Category(String displayName) {
        this.displayName = displayName;
    }

    // Get the display name for UI
    public String getDisplayName() {
        return displayName;
    }
}
