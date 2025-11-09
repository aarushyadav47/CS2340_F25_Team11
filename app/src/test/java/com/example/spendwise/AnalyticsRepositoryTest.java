package com.example.spendwise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.example.spendwise.model.Budget;
import com.example.spendwise.model.BudgetUsageSummary;
import com.example.spendwise.model.Category;
import com.example.spendwise.model.Expense;
import com.example.spendwise.repository.AnalyticsRepository;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyticsRepositoryTest {

    private final AnalyticsRepository analyticsRepository = new AnalyticsRepository();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

    private Date parseDate(String value) throws ParseException {
        return dateFormat.parse(value);
    }

    @Test
    public void calculateCategoryTotals_filtersExpensesOutsideWindow() throws Exception {
        List<Expense> expenses = new ArrayList<>();
        Expense lunch = new Expense("Lunch", 15.0, Category.FOOD, "10/05/2024", "");
        Expense movie = new Expense("Movie Night", 12.0, Category.ENTERTAINMENT, "09/30/2024", "");
        Expense groceries = new Expense("Groceries", 45.0, Category.FOOD, "10/18/2024", "");
        expenses.add(lunch);
        expenses.add(movie);
        expenses.add(groceries);

        Date start = parseDate("10/01/2024");
        Date end = parseDate("10/31/2024");

        Map<String, Double> totals = analyticsRepository.calculateCategoryTotals(expenses, start, end);

        assertEquals(1, totals.size());
        assertEquals(60.0, totals.get("Food"), 0.001);
        assertFalse("Expenses outside the window should be ignored", totals.containsKey("Entertainment"));
    }

    @Test
    public void calculateBudgetUsage_matchesSpendWithinWindow() throws Exception {
        Budget foodBudget = new Budget("Groceries", 200.0, Category.FOOD, "10/03/2024", "Monthly");
        foodBudget.setId("budget_food");
        Budget travelBudget = new Budget("Winter Trip", 300.0, Category.TRANSPORT, "11/15/2024", "Monthly");
        travelBudget.setId("budget_travel");

        List<Budget> budgets = Arrays.asList(foodBudget, travelBudget);

        List<Expense> expenses = Arrays.asList(
                new Expense("Campus Bus", 10.0, Category.TRANSPORT, "10/07/2024", ""),
                new Expense("Groceries", 35.0, Category.FOOD, "10/10/2024", ""),
                new Expense("Weekend Brunch", 25.0, Category.FOOD, "11/02/2024", "")
        );

        Date start = parseDate("10/01/2024");
        Date end = parseDate("10/31/2024");

        List<BudgetUsageSummary> summaries = analyticsRepository.calculateBudgetUsage(budgets, expenses, start, end);

        assertEquals(1, summaries.size());
        BudgetUsageSummary summary = summaries.get(0);
        assertEquals("Groceries", summary.getBudgetName());
        assertEquals("Food", summary.getCategoryName());
        assertEquals(200.0, summary.getAllocatedAmount(), 0.001);
        assertEquals(35.0, summary.getSpentAmount(), 0.001);
        assertEquals(165.0, summary.getRemainingAmount(), 0.001);
    }

    @Test
    public void calculateBudgetUsage_returnsEmptyWhenNoBudgetsInWindow() throws Exception {
        Budget archivedBudget = new Budget("Old Budget", 150.0, Category.OTHER, "08/01/2024", "Monthly");
        archivedBudget.setId("archived");

        List<Budget> budgets = Arrays.asList(archivedBudget);
        List<Expense> expenses = Arrays.asList(
                new Expense("Laundry", 12.0, Category.OTHER, "10/12/2024", "")
        );

        Date start = parseDate("10/01/2024");
        Date end = parseDate("10/31/2024");

        List<BudgetUsageSummary> summaries = analyticsRepository.calculateBudgetUsage(budgets, expenses, start, end);

        assertEquals(0, summaries.size());
    }
}
