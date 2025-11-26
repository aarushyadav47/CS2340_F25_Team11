package com.example.spendwise.util;

/**
 * Constants for notification system
 * Fixes code smell: Magic Numbers/Strings
 */
public class NotificationConstants {
    public static final int NO_EXPENSE_DAYS_THRESHOLD = 3;
    public static final double BUDGET_WARNING_PERCENTAGE = 0.90;
    public static final double BUDGET_WARNING_PERCENTAGE_100 = 90.0;
    
    // Error messages
    public static final String LLAMA_SLEEPING_MESSAGE = "😴 Llama is sleeping, knock later!";
    public static final String LLAMA_NAPPING_MESSAGE = "😴 Llama is napping. Please try again later!";
    
    private NotificationConstants() {
        throw new IllegalStateException("Utility class");
    }
}

