package com.example.spendwise.model;

public enum Category {
    FOOD("Food"),
    TRANSPORT("Transport"),
    ENTERTAINMENT("Entertainment"),
    BILLS("Bills"),https://github.com/aarushyadav47/CS2340_F25_Team11/pull/58/conflict?name=app%252Fsrc%252Fmain%252Fjava%252Fcom%252Fexample%252Fspendwise%252Fview%252FExpenseLog.java&ancestor_oid=d2f1e9bf7c3411cd56a37b70aa14d285ad336118&base_oid=4229dc7c270571789700ae1fd1b8df8301f8d46a&head_oid=e71837c1a0a6d5d3448ed30c27d1fb7863d83f4e
    SHOPPING("Shopping"),
    HEALTH("Health"),
    OTHER("Other");

    private String displayName;

    // Constructor
    Category(String displayName) {
        this.displayName = displayName;
    }

    // Get the display name
    public String getDisplayName() {
        return displayName;
    }
}
