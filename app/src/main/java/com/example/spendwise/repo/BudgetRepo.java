package com.example.spendwise.repo;

import androidx.annotation.NonNull;

import com.example.spendwise.model.Budget;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetRepo {

    private static volatile BudgetRepo instance;
    private final FirebaseDatabase db;
    private final FirebaseAuth auth;

    private BudgetRepo() {
        db = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static BudgetRepo getInstance() {
        if (instance == null) {
            synchronized (BudgetRepo.class) {
                if (instance == null) instance = new BudgetRepo();
            }
        }
        return instance;
    }

    private String safeUid() { return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous"; }
    private DatabaseReference userBudgets() { return db.getReference("users").child(safeUid()).child("budgets"); }

    public void addBudget(Budget budget, @NonNull RepoCallback callback) {
        Map<String,Object> data = new HashMap<>();
        data.put("name", budget.getName());
        data.put("amount", budget.getAmount());
        data.put("category", budget.getCategory());
        data.put("frequency", budget.getFrequency());
        data.put("startDate", budget.getStartDate());
        data.put("createdAt", System.currentTimeMillis());
        userBudgets().push().setValue(data, (error, ref) -> {
            if (error != null) callback.onError(error.getMessage()); else callback.onSuccess();
        });
    }

    public void fetchBudgets(@NonNull BudgetsCallback callback) {
        userBudgets().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Budget> list = new ArrayList<>();
                for (DataSnapshot d : snapshot.getChildren()) {
                    String name = asString(d.child("name").getValue());
                    Double amount = asNumber(d.child("amount").getValue());
                    String category = asString(d.child("category").getValue());
                    String frequency = asString(d.child("frequency").getValue());
                    String startDate = asString(d.child("startDate").getValue());
                    if (name != null && amount != null && category != null && frequency != null && startDate != null) {
                        list.add(new Budget(name, amount, category, frequency, startDate));
                    }
                }
                // createdAt is not used here for sorting; the UI can sort if needed
                callback.onSuccess(list);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { callback.onError(error.getMessage()); }
        });
    }

    public void seedIfEmpty(@NonNull RepoCallback callback) {
        userBudgets().limitToFirst(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.hasChildren()) { callback.onSuccess(); return; }
                List<Budget> seeds = new ArrayList<>();
                seeds.add(new Budget("Food Budget", 150.0, "Food", "Weekly", "2025-10-01"));
                seeds.add(new Budget("Transport Budget", 60.0, "Transport", "Monthly", "2025-10-01"));
                final int total = seeds.size();
                final int[] done = {0};
                final boolean[] failed = {false};
                for (Budget b : seeds) {
                    addBudget(b, new RepoCallback() {
                        @Override public void onSuccess() { if (failed[0]) return; if (++done[0] == total) callback.onSuccess(); }
                        @Override public void onError(String error) { if (!failed[0]) { failed[0] = true; callback.onError(error); } }
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { callback.onError(error.getMessage()); }
        });
    }

    private static String asString(Object v) { return v == null ? null : String.valueOf(v); }
    private static Double asNumber(Object v) { return v instanceof Number ? ((Number)v).doubleValue() : null; }

    public interface RepoCallback { void onSuccess(); void onError(String error); }
    public interface BudgetsCallback { void onSuccess(List<Budget> budgets); void onError(String error); }
}