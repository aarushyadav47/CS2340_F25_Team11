package com.example.spendwise.model;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Firebase {

    // Volatile ensures visibility across threads
    private static volatile FirebaseDatabase databaseInstance;
    private static volatile DatabaseReference expensesRef;
    private static volatile DatabaseReference budgetsRef;

    // Private constructor prevents instantiation
    private Firebase() { }

    // Double-checked locking for thread-safe singleton
    public static FirebaseDatabase getDatabase() {
        if (databaseInstance == null) {
            synchronized (Firebase.class) {
                if (databaseInstance == null) {
                    databaseInstance = FirebaseDatabase.getInstance();
                }
            }
        }
        return databaseInstance;
    }

    // Thread-safe expenses reference getter
    public static DatabaseReference getExpensesRef() {
        if (expensesRef == null) {
            synchronized (Firebase.class) {
                if (expensesRef == null) {
                    expensesRef = getDatabase().getReference("expenses");
                }
            }
        }
        return expensesRef;
    }

    // Thread-safe budgets reference getter
    public static DatabaseReference getBudgetsRef() {
        if (budgetsRef == null) {
            synchronized (Firebase.class) {
                if (budgetsRef == null) {
                    budgetsRef = getDatabase().getReference("budgets");
                }
            }
        }
        return budgetsRef;
    }
}
