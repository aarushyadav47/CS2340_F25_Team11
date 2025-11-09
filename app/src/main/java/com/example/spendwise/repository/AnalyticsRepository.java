package com.example.spendwise.repository;

import com.example.spendwise.model.Budget;
import com.example.spendwise.model.BudgetUsageSummary;
import com.example.spendwise.model.Expense;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared analytics utilities for converting raw budget/expense collections
 * into chart-friendly aggregates.
 */
public class AnalyticsRepository {

    private static final String DATE_PATTERN = "MM/dd/yyyy";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN, Locale.US);

    public Map<String, Double> calculateCategoryTotals(List<Expense> expenses,
                                                       Date windowStart,
                                                       Date windowEnd) {
        if (expenses == null || expenses.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Double> totals = new LinkedHashMap<>();

        for (Expense expense : expenses) {
            if (expense == null || expense.getCategory() == null) {
                continue;
            }

            Date expenseDate = parseDate(expense.getDate());
            if (!isWithinRange(expenseDate, windowStart, windowEnd)) {
                continue;
            }

            String category = expense.getCategory().getDisplayName();
            double amount = expense.getAmount();
            totals.put(category, totals.getOrDefault(category, 0.0) + amount);
        }

        return totals;
    }

    public List<BudgetUsageSummary> calculateBudgetUsage(List<Budget> budgets,
                                                         List<Expense> expenses,
                                                         Date windowStart,
                                                         Date windowEnd) {
        if (budgets == null || budgets.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Double> expenseTotals = aggregateExpensesByCategory(expenses, windowStart, windowEnd);
        List<BudgetUsageSummary> summaries = new ArrayList<>();

        for (Budget budget : budgets) {
            if (budget == null || budget.getCategory() == null) {
                continue;
            }

            Date budgetDate = parseDate(budget.getDate());
            if (!isWithinRange(budgetDate, windowStart, windowEnd)) {
                continue;
            }

            String categoryName = budget.getCategory().getDisplayName();
            double spent = expenseTotals.getOrDefault(categoryName, 0.0);
            summaries.add(new BudgetUsageSummary(
                    budget.getId(),
                    budget.getName(),
                    categoryName,
                    budget.getAmount(),
                    spent
            ));
        }

        return summaries;
    }

    private Map<String, Double> aggregateExpensesByCategory(List<Expense> expenses,
                                                            Date windowStart,
                                                            Date windowEnd) {
        if (expenses == null || expenses.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Double> totals = new HashMap<>();

        for (Expense expense : expenses) {
            if (expense == null || expense.getCategory() == null) {
                continue;
            }

            Date expenseDate = parseDate(expense.getDate());
            if (!isWithinRange(expenseDate, windowStart, windowEnd)) {
                continue;
            }

            String category = expense.getCategory().getDisplayName();
            totals.put(category, totals.getOrDefault(category, 0.0) + expense.getAmount());
        }

        return totals;
    }

    private boolean isWithinRange(Date target, Date start, Date end) {
        if (target == null) {
            return false;
        }

        boolean afterStart = start == null || !target.before(start);
        boolean beforeEnd = end == null || !target.after(end);
        return afterStart && beforeEnd;
    }

    private Date parseDate(String rawDate) {
        if (rawDate == null) {
            return null;
        }

        try {
            return dateFormat.parse(rawDate);
        } catch (ParseException e) {
            return null;
        }
    }

    public List<BudgetUsageSummary> createSeedBudgetUsage() {
        List<BudgetUsageSummary> seeds = new ArrayList<>();
        seeds.add(new BudgetUsageSummary("seed_groceries", "Groceries", "Food", 200, 120));
        seeds.add(new BudgetUsageSummary("seed_transport", "Campus Transit", "Transport", 80, 45));
        seeds.add(new BudgetUsageSummary("seed_entertainment", "Weekend Fun", "Entertainment", 100, 70));
        return seeds;
    }

    public Map<String, Double> createSeedCategoryTotals() {
        Map<String, Double> seeds = new LinkedHashMap<>();
        seeds.put("Food", 120.0);
        seeds.put("Transport", 45.0);
        seeds.put("Entertainment", 70.0);
        seeds.put("Other", 35.0);
        return seeds;
    }
}
