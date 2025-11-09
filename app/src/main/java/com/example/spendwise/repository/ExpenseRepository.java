package com.example.spendwise.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.spendwise.model.Category;
import com.example.spendwise.model.Expense;
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
 * Repository for Expense operations using Firestore
 */
public class ExpenseRepository extends FirestoreRepository<Expense> {

    private static final String COLLECTION_NAME = "expenses";
    private MutableLiveData<List<Expense>> expensesLiveData;
    private ListenerRegistration expensesListener;

    public ExpenseRepository() {
        super();
        expensesLiveData = new MutableLiveData<>(new ArrayList<>());
    }

    /**
     * Get LiveData for expenses list
     * @return LiveData containing list of expenses
     */
    public LiveData<List<Expense>> getExpenses() {
        if (expensesListener == null) {
            attachExpensesListener();
        }
        return expensesLiveData;
    }

    /**
     * Attach real-time listener to expenses collection
     */
    private void attachExpensesListener() {
        String userId = getCurrentUserId();
        if (userId == null) {
            expensesLiveData.setValue(new ArrayList<>());
            return;
        }

        CollectionReference expensesRef = getUserCollection(COLLECTION_NAME);
        
        expensesListener = expensesRef.orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        expensesLiveData.setValue(new ArrayList<>());
                        return;
                    }

                    if (snapshot != null) {
                        List<Expense> expenses = new ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                            Expense expense = documentToModel(doc.getId(), doc.getData());
                            if (expense != null) {
                                expenses.add(expense);
                            }
                        }
                        expensesLiveData.setValue(expenses);
                    } else {
                        expensesLiveData.setValue(new ArrayList<>());
                    }
                });

        activeListeners.put("expenses", expensesListener);
    }

    /**
     * Add a new expense
     * @param expense Expense to add
     * @param callback Callback for success/failure
     */
    public void addExpense(Expense expense, RepositoryCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (callback != null) callback.onError("User not logged in");
            return;
        }

        CollectionReference expensesRef = getUserCollection(COLLECTION_NAME);
        Map<String, Object> data = modelToDocument(expense);

        expensesRef.add(data)
                .addOnSuccessListener(documentReference -> {
                    expense.setId(documentReference.getId());
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Update an existing expense
     * @param expense Expense to update
     * @param callback Callback for success/failure
     */
    public void updateExpense(Expense expense, RepositoryCallback callback) {
        if (expense.getId() == null || expense.getId().isEmpty()) {
            if (callback != null) callback.onError("Expense ID is required");
            return;
        }

        DocumentReference expenseRef = getUserDocument(COLLECTION_NAME, expense.getId());
        Map<String, Object> data = modelToDocument(expense);

        expenseRef.update(data)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Delete an expense
     * @param expenseId Expense ID to delete
     * @param callback Callback for success/failure
     */
    public void deleteExpense(String expenseId, RepositoryCallback callback) {
        DocumentReference expenseRef = getUserDocument(COLLECTION_NAME, expenseId);

        expenseRef.delete()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    @Override
    protected Expense documentToModel(String documentId, Map<String, Object> data) {
        try {
            String name = (String) data.get("name");
            Object amountObj = data.get("amount");
            double amount = amountObj instanceof Number ? ((Number) amountObj).doubleValue() : 0.0;
            String categoryStr = (String) data.get("category");
            String date = (String) data.get("date");
            String notes = data.get("notes") != null ? (String) data.get("notes") : "";

            if (name == null || categoryStr == null || date == null) {
                return null;
            }

            Category category = Category.valueOf(categoryStr);
            Expense expense = new Expense(name, amount, category, date, notes);
            expense.setId(documentId);
            return expense;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected Map<String, Object> modelToDocument(Expense expense) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", expense.getName());
        data.put("amount", expense.getAmount());
        data.put("category", expense.getCategory().name());
        data.put("date", expense.getDate());
        data.put("notes", expense.getNotes());
        return data;
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        removeAllListeners();
        expensesListener = null;
    }

    /**
     * Callback interface for repository operations
     */
    public interface RepositoryCallback {
        void onSuccess();
        void onError(String error);
    }
}

