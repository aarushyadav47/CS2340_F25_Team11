package com.example.spendwise.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.spendwise.model.Category;
import com.example.spendwise.model.Expense;
import com.example.spendwise.repository.ExpenseRepository;
import com.example.spendwise.strategy.ExpenseSortStrategy;
import com.example.spendwise.strategy.SortByDateStrategy;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for Expense operations using Firestore
 * Migrated from Firebase Realtime Database to Firestore
 */
public class ExpenseViewModel extends ViewModel {
    private static final String TAG = "ExpenseViewModel";

    private MutableLiveData<String> statusMessage;
    private final ExpenseRepository expenseRepository;
    private ExpenseSortStrategy sortStrategy = new SortByDateStrategy(); // Default

    public ExpenseViewModel() {
        statusMessage = new MutableLiveData<>();
        expenseRepository = new ExpenseRepository();
    }

    public LiveData<List<Expense>> getExpenses() {
        return expenseRepository.getExpenses();
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    /**
     * Add new expense to Firestore
     */
    public void addExpense(String name, double amount, Category category,
                           String date, String notes) {
        Expense expense = new Expense(name, amount, category, date, notes);
        
        expenseRepository.addExpense(expense, new ExpenseRepository.RepositoryCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Expense added successfully: " + expense);
                statusMessage.setValue("Expense added!");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error adding expense: " + error);
                statusMessage.setValue("Error: " + error);
            }
        });
    }

    /**
     * Update existing expense in Firestore
     */
    public void updateExpense(String id, String name, double amount,
                              Category category, String date, String notes) {
        Expense expense = new Expense(name, amount, category, date, notes);
        expense.setId(id);

        expenseRepository.updateExpense(expense, new ExpenseRepository.RepositoryCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Expense updated successfully");
                statusMessage.setValue("Expense updated!");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error updating expense: " + error);
                statusMessage.setValue("Error: " + error);
            }
        });
    }

    /**
     * Delete expense from Firestore
     */
    public void deleteExpense(String id) {
        expenseRepository.deleteExpense(id, new ExpenseRepository.RepositoryCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Expense deleted successfully");
                statusMessage.setValue("Expense deleted!");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error deleting expense: " + error);
                statusMessage.setValue("Error: " + error);
            }
        });
    }

    /**
     * Set sorting strategy for expenses
     */
    public void setSortStrategy(ExpenseSortStrategy strategy) {
        this.sortStrategy = strategy;
        applySorting();
    }

    /**
     * Apply sorting to current expenses
     */
    private void applySorting() {
        List<Expense> currentExpenses = getExpenses().getValue();
        if (currentExpenses != null && !currentExpenses.isEmpty()) {
            List<Expense> sortedExpenses = new ArrayList<>(currentExpenses);
            sortStrategy.sort(sortedExpenses);
            // Note: Sorting is applied in the repository's query, 
            // but this allows runtime strategy changes if needed
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        expenseRepository.cleanup();
    }
}

