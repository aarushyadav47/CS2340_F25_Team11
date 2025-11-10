package com.example.spendwise;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.spendwise.factory.ChartFactory;
import com.example.spendwise.model.Budget;
import com.example.spendwise.model.Category;
import com.example.spendwise.model.Expense;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.PieData;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for ChartFactory (Factory Pattern)
 * Tests Sprint 3 design pattern and analytics functionality.
 */
@RunWith(RobolectricTestRunner.class)
public class ChartFactoryTest {

    @Test
    public void testCreateCategoryPieChart() {
        List<Expense> expenses = new ArrayList<>();
        expenses.add(new Expense("Coffee", 5.0, Category.FOOD, "10/25/2024", ""));
        expenses.add(new Expense("Lunch", 15.0, Category.FOOD, "10/24/2024", ""));
        expenses.add(new Expense("Bus", 2.5, Category.TRANSPORT, "10/23/2024", ""));

        PieData pieData = ChartFactory.createCategoryPieChart(expenses);

        assertNotNull("PieData should not be null", pieData);
        assertNotNull("PieData should have dataset", pieData.getDataSet());
        assertTrue("PieData should have entries", pieData.getEntryCount() > 0);
    }

    @Test
    public void testCreateCategoryPieChartWithEmptyExpenses() {
        List<Expense> emptyExpenses = new ArrayList<>();

        PieData pieData = ChartFactory.createCategoryPieChart(emptyExpenses);

        assertNotNull("PieData should not be null even with empty expenses", pieData);
        assertTrue("PieData should have at least one entry (No Data)",
                pieData.getEntryCount() > 0);
    }

    @Test
    public void testCreateBudgetBarChart() {
        List<Budget> budgets = new ArrayList<>();
        budgets.add(new Budget("Food Budget", 200.0, Category.FOOD, "10/01/2024", "Monthly"));
        budgets.add(new Budget("Transport Budget", 100.0, Category.TRANSPORT, "10/01/2024", "Monthly"));

        List<Expense> expenses = new ArrayList<>();
        expenses.add(new Expense("Lunch", 50.0, Category.FOOD, "10/15/2024", ""));
        expenses.add(new Expense("Bus", 25.0, Category.TRANSPORT, "10/14/2024", ""));

        BarData barData = ChartFactory.createBudgetBarChart(budgets, expenses);

        assertNotNull("BarData should not be null", barData);
        assertTrue("BarData should have datasets", barData.getDataSetCount() > 0);
    }

    @Test
    public void testFactoryPatternCentralizesChartCreation() {
        List<Expense> expenses = new ArrayList<>();
        expenses.add(new Expense("Test", 10.0, Category.FOOD, "10/25/2024", ""));

        PieData pieData1 = ChartFactory.createCategoryPieChart(expenses);
        PieData pieData2 = ChartFactory.createCategoryPieChart(expenses);

        assertNotNull("Factory should create PieData", pieData1);
        assertNotNull("Factory should create PieData consistently", pieData2);
        assertTrue("Factory creates chart data without exposing implementation",
                pieData1.getEntryCount() == pieData2.getEntryCount());
    }
}
