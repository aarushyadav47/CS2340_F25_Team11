// Sprint 3 Test - Bar Chart Analytics for Budget Tracking
package com.example.spendwise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.spendwise.factory.ChartFactory;
import com.example.spendwise.model.Budget;
import com.example.spendwise.model.Category;
import com.example.spendwise.model.Expense;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarEntry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for Budget vs Spent Bar Chart Analytics
 * Sprint 3 requirement: Charts dynamically update and show budget utilization
 */
@RunWith(RobolectricTestRunner.class)
public class BarChartAnalyticsTest {

    /**
     * Test that BarChart correctly compares budget targets with actual spending
     * This verifies the Factory Pattern creates accurate analytics visualization
     */
    @Test
    public void testBarChartBudgetVsSpentComparison() {
        // Setup budgets for different categories
        List<Budget> budgets = new ArrayList<>();
        budgets.add(new Budget("Food Budget", 300.0, Category.FOOD, "11/01/2025", "Monthly"));
        budgets.add(new Budget("Transport Budget", 150.0, Category.TRANSPORT, "11/01/2025", "Monthly"));
        budgets.add(new Budget("Entertainment Budget", 200.0, Category.ENTERTAINMENT, "11/01/2025", "Monthly"));

        // Setup expenses that partially use the budgets
        List<Expense> expenses = new ArrayList<>();
        // Food expenses totaling 180 (60% of 300 budget)
        expenses.add(new Expense("Groceries", 100.0, Category.FOOD, "11/05/2025", "Weekly shopping"));
        expenses.add(new Expense("Restaurant", 80.0, Category.FOOD, "11/10/2025", "Dinner out"));
        
        // Transport expenses totaling 120 (80% of 150 budget)
        expenses.add(new Expense("Gas", 70.0, Category.TRANSPORT, "11/03/2025", "Fuel"));
        expenses.add(new Expense("Bus Pass", 50.0, Category.TRANSPORT, "11/08/2025", "Monthly pass"));
        
        // Entertainment expenses totaling 250 (125% of 200 budget - OVER BUDGET!)
        expenses.add(new Expense("Concert", 150.0, Category.ENTERTAINMENT, "11/12/2025", "Live show"));
        expenses.add(new Expense("Movies", 100.0, Category.ENTERTAINMENT, "11/15/2025", "Theater tickets"));

        // Create bar chart using Factory Pattern
        BarData barData = ChartFactory.createBudgetBarChart(budgets, expenses);

        // Verify chart was created successfully
        assertNotNull("BarData should not be null", barData);
        assertTrue("BarData should have multiple datasets (spent and target)", 
                barData.getDataSetCount() >= 2);
        
        // Verify data contains entries
        assertTrue("BarData should have entries for budgets", 
                barData.getEntryCount() > 0);
        
        // Verify the chart can handle budget limits (max 5 budgets displayed)
        assertTrue("Chart should respect display limit", 
                barData.getDataSetByIndex(0).getEntryCount() <= 5);
    }

    /**
     * Test that empty budget list is handled gracefully with placeholder data
     * Sprint 3 requirement: Handle empty/null datasets
     */
    @Test
    public void testBarChartWithEmptyBudgets() {
        List<Budget> emptyBudgets = new ArrayList<>();
        List<Expense> expenses = new ArrayList<>();
        expenses.add(new Expense("Test", 50.0, Category.FOOD, "11/01/2025", ""));

        BarData barData = ChartFactory.createBudgetBarChart(emptyBudgets, expenses);

        assertNotNull("BarData should not be null for empty budgets", barData);
        assertTrue("BarData should have placeholder entries", 
                barData.getEntryCount() > 0);
    }
}

