package com.example.spendwise.repo;

import com.example.spendwise.model.Budget;
import com.google.firebase.firestore.FirebaseFirestore;

public class BudgetRepo {

    private final FirebaseFirestore db;

    public BudgetRepo() {
        db = FirebaseFirestore.getInstance();
    }

    public void addBudget(Budget budget, OnBudgetAddedListener listener) {
        db.collection("budgets")
                .add(budget)
                .addOnSuccessListener(documentReference ->
                        listener.onSuccess("Budget added with ID: " + documentReference.getId()))
                .addOnFailureListener(e ->
                        listener.onFailure("Error adding budget: " + e.getMessage()));
    }

    public interface OnBudgetAddedListener {
        void onSuccess(String message);
        void onFailure(String error);
    }
}