// Sprint 3 Test - Chart Factory Functionality
package com.example.spendwise;

import static org.junit.Assert.*;

import com.example.spendwise.factory.ChartFactory;
import com.example.spendwise.model.Category;
import com.example.spendwise.model.Expense;
import com.example.spendwise.model.Budget;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.BarData;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ChartFactoryTest {

    /**
     * Test 1: Verify PieChart handles empty expense list
     * Sprint 3 requirement: Charts must handle empty/null datasets with placeholder data
     */
    @Test
    public void testPieChartWithEmptyExpenses() {
        List<Expense> emptyExpenses = new ArrayList<>();

        PieData pieData = ChartFactory.createCategoryPieChart(emptyExpenses);

        assertNotNull("PieData should not be null for empty list", pieData);
        assertEquals("Should have 1 placeholder entry for empty data",
                1, pieData.getEntryCount());
    }

    /**
     * Test 2: Verify PieChart correctly aggregates expenses by category
     * Sprint 3 requirement: Charts must display category breakdown
     */
    @Test
    public void testPieChartCategoryAggregation() {
        List<Expense> expenses = new ArrayList<>();
        expenses.add(new Expense("Coffee", 5.0, Category.FOOD, "11/01/2025", ""));
        expenses.add(new Expense("Lunch", 15.0, Category.FOOD, "11/02/2025", ""));
        expenses.add(new Expense("Gas", 40.0, Category.TRANSPORT, "11/03/2025", ""));

        PieData pieData = ChartFactory.createCategoryPieChart(expenses);

        assertNotNull("PieData should not be null", pieData);
        assertEquals("Should have 2 categories (Food and Transport)",
                2, pieData.getEntryCount());
    }
}


