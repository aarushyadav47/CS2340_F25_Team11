package com.example.spendwise.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * ViewModel for Budget screen
 * Handles budget calculator logic and validation
 *
 * Sprint 2 Requirement: Budget Window Calculator (2-of-3)
 * Provide three inputs—Total Budget, Spent-to-Date, Remaining.
 * If any two are filled, compute the third.
 */
public class BudgetViewModel extends ViewModel {

    private final MutableLiveData<String> calculationResult = new MutableLiveData<>();
    private final MutableLiveData<Double> calculatedValue = new MutableLiveData<>();
    private final MutableLiveData<java.util.List<com.example.spendwise.model.Budget>> budgets = new MutableLiveData<>();
    private final com.example.spendwise.repo.BudgetRepo budgetRepo = com.example.spendwise.repo.BudgetRepo.getInstance();

    public BudgetViewModel() {}

    public LiveData<String> getCalculationResult() {
        return calculationResult;
    }

    public LiveData<Double> getCalculatedValue() {
        return calculatedValue;
    }

    public LiveData<java.util.List<com.example.spendwise.model.Budget>> getBudgets() { return budgets; }

    public void refreshBudgets(boolean withSeed) {
        if (withSeed) {
            budgetRepo.seedIfEmpty(new com.example.spendwise.repo.BudgetRepo.RepoCallback() {
                @Override public void onSuccess() { loadBudgets(); }
                @Override public void onError(String error) { loadBudgets(); }
            });
        } else {
            loadBudgets();
        }
    }

    private void loadBudgets() {
        budgetRepo.fetchBudgets(new com.example.spendwise.repo.BudgetRepo.BudgetsCallback() {
            @Override public void onSuccess(java.util.List<com.example.spendwise.model.Budget> list) { budgets.postValue(list); }
            @Override public void onError(String error) { calculationResult.postValue(error); }
        });
    }

    /**
     * Budget Window Calculator (2-of-3)
     * Calculates the missing value when two of three are provided
     *
     * @param totalBudget Total budget amount (null if unknown)
     * @param spentToDate Amount spent so far (null if unknown)
     * @param remaining Remaining amount (null if unknown)
     * @return true if calculation successful, false if invalid inputs
     */
    public boolean calculateBudgetValues(Double totalBudget, Double spentToDate,
                                         Double remaining) {
        // Count how many values are provided (non-null)
        int providedCount = 0;
        if (totalBudget != null) providedCount++;
        if (spentToDate != null) providedCount++;
        if (remaining != null) providedCount++;

        // Need exactly 2 values to calculate the third
        if (providedCount < 2) {
            calculationResult.setValue("Please provide at least 2 values");
            return false;
        }

        if (providedCount == 3) {
            // All three provided - verify they're consistent
            if (!verifyBudgetConsistency(totalBudget, spentToDate, remaining)) {
                calculationResult.setValue("Values are inconsistent");
                return false;
            }
            calculationResult.setValue("All values verified");
            return true;
        }

        // Calculate the missing value
        if (totalBudget == null) {
            // Calculate Total = Spent + Remaining
            double calculated = calculateTotal(spentToDate, remaining);
            calculatedValue.setValue(calculated);
            calculationResult.setValue("Total Budget calculated: $" +
                    String.format("%.2f", calculated));
            return true;
        } else if (spentToDate == null) {
            // Calculate Spent = Total - Remaining
            double calculated = calculateSpent(totalBudget, remaining);
            calculatedValue.setValue(calculated);
            calculationResult.setValue("Spent-to-Date calculated: $" +
                    String.format("%.2f", calculated));
            return true;
        } else {
            // Calculate Remaining = Total - Spent
            double calculated = calculateRemaining(totalBudget, spentToDate);
            calculatedValue.setValue(calculated);
            calculationResult.setValue("Remaining calculated: $" +
                    String.format("%.2f", calculated));
            return true;
        }
    }

    /**
     * Calculate Total Budget = Spent + Remaining
     */
    public double calculateTotal(double spent, double remaining) {
        return spent + remaining;
    }

    /**
     * Calculate Spent-to-Date = Total - Remaining
     */
    public double calculateSpent(double total, double remaining) {
        return total - remaining;
    }

    /**
     * Calculate Remaining = Total - Spent
     */
    public double calculateRemaining(double total, double spent) {
        return total - spent;
    }

    /**
     * Verify that Total = Spent + Remaining (with small tolerance for floating point)
     */
    private boolean verifyBudgetConsistency(double total, double spent, double remaining) {
        double tolerance = 0.01; // Allow 1 cent difference for floating point errors
        double calculatedTotal = spent + remaining;
        return Math.abs(total - calculatedTotal) < tolerance;
    }

    /**
     * Validate budget creation inputs
     * Sprint 2 Requirements:
     * - Required fields: title, amount, category, frequency, start date
     * - Non-negative amounts
     * - Valid start dates
     */
    public boolean validateBudgetCreation(String title, String amount, String category,
                                          String frequency, String startDate) {
        // Check required fields
        if (title == null || title.trim().isEmpty()) {
            calculationResult.setValue("Budget title is required");
            return false;
        }

        if (amount == null || amount.trim().isEmpty()) {
            calculationResult.setValue("Budget amount is required");
            return false;
        }

        if (category == null || category.trim().isEmpty()) {
            calculationResult.setValue("Category is required");
            return false;
        }

        if (frequency == null || frequency.trim().isEmpty()) {
            calculationResult.setValue("Frequency is required");
            return false;
        }

        if (startDate == null || startDate.trim().isEmpty()) {
            calculationResult.setValue("Start date is required");
            return false;
        }

        // Validate amount is non-negative
        try {
            double amountValue = Double.parseDouble(amount.trim());
            if (amountValue < 0) {
                calculationResult.setValue("Amount cannot be negative");
                return false;
            }
        } catch (NumberFormatException e) {
            calculationResult.setValue("Invalid amount format");
            return false;
        }

        // Validate frequency is either "weekly" or "monthly"
        String freq = frequency.trim().toLowerCase();
        if (!freq.equals("weekly") && !freq.equals("monthly")) {
            calculationResult.setValue("Frequency must be 'weekly' or 'monthly'");
            return false;
        }

        calculationResult.setValue("Budget validation successful");
        return true;
    }

    /**
     * Calculate budget utilization percentage
     * Used for progress indicators
     */
    public double calculateUtilizationPercentage(double total, double spent) {
        if (total <= 0) {
            return 0.0;
        }
        return (spent / total) * 100.0;
    }

    /**
     * Calculate surplus (or deficit if negative)
     */
    public double calculateSurplus(double total, double spent) {
        return total - spent;
    }

    /**
     * Determine budget status for color indicators
     * Returns: "completed", "in_progress", or "incomplete"
     */
    public String getBudgetStatus(double total, double spent, boolean isActive) {
        if (!isActive) {
            return spent <= total ? "completed" : "incomplete";
        }

        double utilizationPercent = calculateUtilizationPercentage(total, spent);

        if (utilizationPercent >= 100) {
            return "incomplete"; // Over budget = red
        } else if (utilizationPercent > 0) {
            return "in_progress"; // Some spending = yellow
        } else {
            return "completed"; // No spending yet = green
        }
    }
}