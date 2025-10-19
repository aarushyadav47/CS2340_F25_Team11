package com.example.spendwise.repo;

import androidx.annotation.NonNull;

import com.example.spendwise.model.Budget;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetRepo {

    private static volatile BudgetRepo instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private BudgetRepo() {
        db = FirebaseFirestore.getInstance();
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

    private String safeUid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";
    }

    private CollectionReference userBudgets() {
        return db.collection("users").document(safeUid()).collection("budgets");
    }

    public void addBudget(Budget budget, @NonNull RepoCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", budget.getName());
        data.put("amount", budget.getAmount());
        data.put("category", budget.getCategory());
        data.put("frequency", budget.getFrequency());
        data.put("startDate", budget.getStartDate());
        data.put("createdAt", com.google.firebase.Timestamp.now());

        userBudgets().add(data)
                .addOnSuccessListener(r -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void fetchBudgets(@NonNull BudgetsCallback callback) {
        userBudgets().orderBy("createdAt", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(q -> {
                    List<Budget> list = new ArrayList<>();
                    for (DocumentSnapshot d : q.getDocuments()) {
                        String name = d.getString("name");
                        Number amountNum = (Number) d.get("amount");
                        String category = d.getString("category");
                        String frequency = d.getString("frequency");
                        String startDate = d.getString("startDate");
                        if (name != null && amountNum != null && category != null && frequency != null && startDate != null) {
                            list.add(new Budget(name, amountNum.doubleValue(), category, frequency, startDate));
                        }
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void seedIfEmpty(@NonNull RepoCallback callback) {
        userBudgets().limit(1).get().addOnSuccessListener(snap -> {
            if (snap.isEmpty()) {
                List<Budget> seeds = new ArrayList<>();
                seeds.add(new Budget("Food Budget", 150.0, "Food", "Weekly", "2025-10-01"));
                seeds.add(new Budget("Transport Budget", 60.0, "Transport", "Monthly", "2025-10-01"));
                final int total = seeds.size();
                final int[] done = {0};
                final boolean[] failed = {false};
                for (Budget b : seeds) {
                    addBudget(b, new RepoCallback() {
                        @Override public void onSuccess() { if (failed[0]) return; done[0]++; if (done[0]==total) callback.onSuccess(); }
                        @Override public void onError(String error) { if (!failed[0]) { failed[0]=true; callback.onError(error);} }
                    });
                }
            } else callback.onSuccess();
        }).addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public interface RepoCallback { void onSuccess(); void onError(String error); }
    public interface BudgetsCallback { void onSuccess(List<Budget> budgets); void onError(String error); }
}