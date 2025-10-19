package com.example.spendwise.model;

public enum Category {
    FOOD("Food"),
    TRANSPORT("Transport"),
    ENTERTAINMENT("Entertainment"),
    BILLS("Bills"),
    SHOPPING("Shopping"),
    HEALTH("Health"),
    OTHER("Other");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
