package com.example.spendwise.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.spendwise.model.Budget;
import com.example.spendwise.model.Category;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for Budget operations using Firestore
 */
public class BudgetRepository extends FirestoreRepository<Budget> {

    private static final String COLLECTION_NAME = "budgets";
    private MutableLiveData<List<Budget>> budgetsLiveData;
    private ListenerRegistration budgetsListener;

    public BudgetRepository() {
        super();
        budgetsLiveData = new MutableLiveData<>(new ArrayList<>());
    }

    /**
     * Get LiveData for budgets list
     * @return LiveData containing list of budgets
     */
    public LiveData<List<Budget>> getBudgets() {
        if (budgetsListener == null) {
            attachBudgetsListener();
        }
        return budgetsLiveData;
    }

    /**
     * Attach real-time listener to budgets collection
     */
    private void attachBudgetsListener() {
        String userId = getCurrentUserId();
        if (userId == null) {
            budgetsLiveData.setValue(new ArrayList<>());
            return;
        }

        CollectionReference budgetsRef = getUserCollection(COLLECTION_NAME);
        
        budgetsListener = budgetsRef.orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        budgetsLiveData.setValue(new ArrayList<>());
                        return;
                    }

                    if (snapshot != null) {
                        List<Budget> budgets = new ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                            Budget budget = documentToModel(doc.getId(), doc.getData());
                            if (budget != null) {
                                budgets.add(budget);
                            }
                        }
                        budgetsLiveData.setValue(budgets);
                    } else {
                        budgetsLiveData.setValue(new ArrayList<>());
                    }
                });

        activeListeners.put("budgets", budgetsListener);
    }

    /**
     * Add a new budget
     * @param budget Budget to add
     * @param callback Callback for success/failure
     */
    public void addBudget(Budget budget, RepositoryCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (callback != null) callback.onError("User not logged in");
            return;
        }

        CollectionReference budgetsRef = getUserCollection(COLLECTION_NAME);
        Map<String, Object> data = modelToDocument(budget);

        budgetsRef.add(data)
                .addOnSuccessListener(documentReference -> {
                    budget.setId(documentReference.getId());
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Update an existing budget
     * @param budget Budget to update
     * @param callback Callback for success/failure
     */
    public void updateBudget(Budget budget, RepositoryCallback callback) {
        if (budget.getId() == null || budget.getId().isEmpty()) {
            if (callback != null) callback.onError("Budget ID is required");
            return;
        }

        DocumentReference budgetRef = getUserDocument(COLLECTION_NAME, budget.getId());
        Map<String, Object> data = modelToDocument(budget);

        budgetRef.update(data)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Delete a budget
     * @param budgetId Budget ID to delete
     * @param callback Callback for success/failure
     */
    public void deleteBudget(String budgetId, RepositoryCallback callback) {
        DocumentReference budgetRef = getUserDocument(COLLECTION_NAME, budgetId);

        budgetRef.delete()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    @Override
    protected Budget documentToModel(String documentId, Map<String, Object> data) {
        try {
            String name = (String) data.get("name");
            Object amountObj = data.get("amount");
            double amount = amountObj instanceof Number ? ((Number) amountObj).doubleValue() : 0.0;
            String categoryStr = (String) data.get("category");
            String date = (String) data.get("date");
            String freq = (String) data.get("freq");

            if (name == null || categoryStr == null || date == null || freq == null) {
                return null;
            }

            Category category = Category.valueOf(categoryStr);
            Budget budget = new Budget(name, amount, category, date, freq);
            budget.setId(documentId);
            return budget;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected Map<String, Object> modelToDocument(Budget budget) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", budget.getName());
        data.put("amount", budget.getAmount());
        data.put("category", budget.getCategory().name());
        data.put("date", budget.getDate());
        data.put("freq", budget.getfreq());
        return data;
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        removeAllListeners();
        budgetsListener = null;
    }

    /**
     * Callback interface for repository operations
     */
    public interface RepositoryCallback {
        void onSuccess();
        void onError(String error);
    }
}

