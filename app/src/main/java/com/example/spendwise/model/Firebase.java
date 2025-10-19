package com.example.spendwise.model;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Firebase {

    private static FirebaseDatabase databaseInstance;
    private static DatabaseReference expensesRef;
    private static DatabaseReference budgetsRef;

    // Private constructor prevents instantiation
    private Firebase() { }

    // Get the FirebaseDatabase instance
    public static FirebaseDatabase getDatabase() {
        if (databaseInstance == null) {
            databaseInstance = FirebaseDatabase.getInstance();
        }
        return databaseInstance;
    }

    // Get the expenses reference
    public static DatabaseReference getExpensesRef() {
        if (expensesRef == null) {
            expensesRef = getDatabase().getReference("expenses");
        }
        return expensesRef;
    }

    // Get the expenses reference
    public static DatabaseReference getBudgetsRef() {
        if (budgetsRef == null) {
            budgetsRef = getDatabase().getReference("budgets");
        }
        return budgetsRef;
    }
}
