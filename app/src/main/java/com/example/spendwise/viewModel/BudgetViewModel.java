package com.example.spendwise.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.spendwise.model.Budget;
import com.example.spendwise.model.Category;
import com.example.spendwise.repository.BudgetRepository;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for Budget operations using Firestore
 * Migrated from Firebase Realtime Database to Firestore
 */
public class BudgetViewModel extends ViewModel {
    private static final String TAG = "BudgetViewModel";

    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final BudgetRepository budgetRepository;

    public BudgetViewModel() {
        budgetRepository = new BudgetRepository();
    }

    public LiveData<List<Budget>> getBudgets() {
        return budgetRepository.getBudgets();
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    /**
     * Add new budget to Firestore
     */
    public void addBudget(String name, double amount, Category category, String date, String freq) {
        Budget budget = new Budget(name, amount, category, date, freq);

        budgetRepository.addBudget(budget, new BudgetRepository.RepositoryCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Budget added successfully: " + budget);
                statusMessage.setValue("Budget created successfully");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error creating budget: " + error);
                statusMessage.setValue("Error creating budget: " + error);
            }
        });
    }

    /**
     * Delete budget from Firestore
     */
    public void deleteBudget(String budgetId) {
        budgetRepository.deleteBudget(budgetId, new BudgetRepository.RepositoryCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Budget deleted successfully");
                statusMessage.setValue("Budget deleted successfully");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error deleting budget: " + error);
                statusMessage.setValue("Error deleting budget: " + error);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        budgetRepository.cleanup();
    }
}

