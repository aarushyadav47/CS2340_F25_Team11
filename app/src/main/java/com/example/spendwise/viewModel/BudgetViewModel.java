package com.example.spendwise.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.spendwise.model.Budget;
import com.example.spendwise.model.Category;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetViewModel extends ViewModel {
    private final DatabaseReference database = FirebaseDatabase.getInstance().getReference();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final MutableLiveData<List<Budget>> budgets = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();

    public LiveData<List<Budget>> getBudgets() {
        loadBudgets();
        return budgets;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public void loadBudgets() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            statusMessage.setValue("User not authenticated");
            return;
        }

        database.child("users")
                .child(user.getUid())
                .child("budgets")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<Budget> budgetList = new ArrayList<>();
                        for (DataSnapshot budgetSnapshot : snapshot.getChildren()) {
                            String id = budgetSnapshot.getKey();
                            String name = budgetSnapshot.child("name").getValue(String.class);
                            Double amount = budgetSnapshot.child("amount").getValue(Double.class);
                            String categoryStr = budgetSnapshot.child("category")
                                    .getValue(String.class);
                            String date = budgetSnapshot.child("date").getValue(String.class);
                            String freq = budgetSnapshot.child("freq").getValue(String.class);

                            if (name != null && amount != null && categoryStr != null
                                    && date != null && freq != null) {
                                try {
                                    Category category = Category.valueOf(categoryStr);
                                    Budget budget = new Budget(name, amount, category, date, freq);
                                    budget.setId(id);
                                    budgetList.add(budget);
                                } catch (IllegalArgumentException e) {
                                    // Skip invalid category
                                }
                            }
                        }
                        budgets.setValue(budgetList);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        statusMessage.setValue("Error loading budgets: " + error.getMessage());
                    }
                });
    }

    public void addBudget(String name, double amount, Category category, String date, String freq) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            statusMessage.setValue("User not authenticated");
            return;
        }

        Budget budget = new Budget(name, amount, category, date, freq);

        Map<String, Object> budgetData = new HashMap<>();
        budgetData.put("name", budget.getName());
        budgetData.put("amount", budget.getAmount());
        budgetData.put("category", budget.getCategory().name());
        budgetData.put("date", budget.getDate());
        budgetData.put("freq", budget.getfreq());

        database.child("users")
                .child(user.getUid())
                .child("budgets")
                .child(budget.getId())
                .setValue(budgetData)
                .addOnSuccessListener(aVoid -> {
                    statusMessage.setValue("Budget created successfully");
                })
                .addOnFailureListener(e -> {
                    statusMessage.setValue("Error creating budget: " + e.getMessage());
                });
    }

    public void deleteBudget(String budgetId) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            statusMessage.setValue("User not authenticated");
            return;
        }

        database.child("users")
                .child(user.getUid())
                .child("budgets")
                .child(budgetId)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    statusMessage.setValue("Budget deleted successfully");
                })
                .addOnFailureListener(e -> {
                    statusMessage.setValue("Error deleting budget: " + e.getMessage());
                });
    }

    /** Dummy method to simulate updating a budget locally */
    public void updateBudget(String id, double newAmount) {
        List<Budget> current = budgets.getValue();
        if (current == null) return;
        for (Budget b : current) {
            if (b.getId() != null && b.getId().equals(id)) {
                b.setAmount(newAmount);
                statusMessage.setValue("Budget " + b.getName() + " updated locally.");
                budgets.setValue(current);
                return;
            }
        }
        statusMessage.setValue("Budget not found for update.");
    }

    /** Dummy method to find a budget by ID */
    public Budget getBudgetById(String id) {
        if (budgets.getValue() == null) return null;
        for (Budget b : budgets.getValue()) {
            if (b.getId() != null && b.getId().equals(id)) {
                return b;
            }
        }
        return null;
    }
}
