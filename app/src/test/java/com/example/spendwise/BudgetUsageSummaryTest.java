package com.example.spendwise;

import static org.junit.Assert.assertEquals;

import com.example.spendwise.model.BudgetUsageSummary;

import org.junit.Test;

/**
 * Tests for BudgetUsageSummary model used in Dashboard Analytics
 * Tests Sprint 3 analytics functionality
 */
public class BudgetUsageSummaryTest {

    @Test
    public void testBudgetUsageSummaryCreation() {
        String budgetId = "budget_123";
        String budgetName = "Groceries Budget";
        String categoryName = "Food";
        double allocatedAmount = 200.0;
        double spentAmount = 75.0;

        BudgetUsageSummary summary = new BudgetUsageSummary(
                budgetId, budgetName, categoryName, allocatedAmount, spentAmount);

        assertEquals("Budget ID should match", budgetId, summary.getBudgetId());
        assertEquals("Budget name should match", budgetName, summary.getBudgetName());
        assertEquals("Allocated amount should match", allocatedAmount, summary.getAllocatedAmount(), 0.001);
        assertEquals("Spent amount should match", spentAmount, summary.getSpentAmount(), 0.001);
    }

    @Test
    public void testRemainingAmountCalculation() {
        BudgetUsageSummary summary = new BudgetUsageSummary(
                "budget_1", "Test Budget", "Food", 200.0, 50.0);

        assertEquals("Remaining amount should be allocated minus spent",
                150.0, summary.getRemainingAmount(), 0.001);
    }

    @Test
    public void testRemainingAmountCannotBeNegative() {
        BudgetUsageSummary summary = new BudgetUsageSummary(
                "budget_2", "Over Budget", "Transport", 100.0, 150.0);

        assertEquals("Remaining amount should not be negative",
                0.0, summary.getRemainingAmount(), 0.001);
    }

    @Test
    public void testUtilizationPercentage() {
        BudgetUsageSummary summary = new BudgetUsageSummary(
                "budget_3", "Test", "Entertainment", 100.0, 50.0);

        assertEquals("Utilization should be 50%",
                50.0, summary.getUtilizationPercentage(), 0.001);
    }
}
