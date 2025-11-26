package com.example.spendwise.logic;

public class BudgetAlertLogic {

    /**
     * Checks if the spending is approaching the budget limit.
     *
     * @param spent The total amount spent so far.
     * @param limit The total budget limit.
     * @param thresholdPercentage The percentage (0.0 to 1.0) at which to trigger the alert.
     * @return true if spent >= limit * thresholdPercentage, false otherwise.
     */
    public boolean isNearLimit(double spent, double limit, double thresholdPercentage) {
        if (limit <= 0) return false;
        return spent >= (limit * thresholdPercentage) && spent < limit;
    }

    /**
     * Checks if the budget is exceeded.
     *
     * @param spent The total amount spent so far.
     * @param limit The total budget limit.
     * @return true if spent >= limit.
     */
    public boolean isExceeded(double spent, double limit) {
        if (limit <= 0) return spent > 0;
        return spent >= limit;
    }

    /**
     * Generates a warning message for a budget near its limit.
     *
     * @param budgetName The name of the budget.
     * @param spent The amount spent.
     * @param limit The budget limit.
     * @return A formatted warning message.
     */
    public String getNearLimitMessage(String budgetName, double spent, double limit) {
        int percentage = (int) ((spent / limit) * 100);
        return String.format("You've reached %d%% of your %s budget ($%.2f / $%.2f)", 
                percentage, budgetName, spent, limit);
    }
    
    /**
     * Generates a warning message for an exceeded budget.
     *
     * @param budgetName The name of the budget.
     * @param spent The amount spent.
     * @param limit The budget limit.
     * @return A formatted warning message.
     */
    public String getExceededMessage(String budgetName, double spent, double limit) {
        return String.format("You've exceeded your %s budget! ($%.2f / $%.2f)", 
                budgetName, spent, limit);
    }
}
