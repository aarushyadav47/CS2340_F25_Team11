package com.example.spendwise.model;

/**
 * Aggregated budgeting snapshot used by analytics visualizations.
 */
public class BudgetUsageSummary {
    private final String budgetId;
    private final String budgetName;
    private final String categoryName;
    private final double allocatedAmount;
    private final double spentAmount;

    public BudgetUsageSummary(String budgetId,
                              String budgetName,
                              String categoryName,
                              double allocatedAmount,
                              double spentAmount) {
        this.budgetId = budgetId;
        this.budgetName = budgetName;
        this.categoryName = categoryName;
        this.allocatedAmount = allocatedAmount;
        this.spentAmount = spentAmount;
    }

    public String getBudgetId() {
        return budgetId;
    }

    public String getBudgetName() {
        return budgetName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public double getAllocatedAmount() {
        return allocatedAmount;
    }

    public double getSpentAmount() {
        return spentAmount;
    }

    public double getRemainingAmount() {
        return Math.max(allocatedAmount - spentAmount, 0);
    }

    public double getUtilizationPercentage() {
        if (allocatedAmount <= 0) {
            return 0;
        }
        return Math.min((spentAmount / allocatedAmount) * 100, 100);
    }
}

