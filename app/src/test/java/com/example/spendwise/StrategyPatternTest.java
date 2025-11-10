package com.example.spendwise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.spendwise.model.Category;
import com.example.spendwise.model.Expense;
import com.example.spendwise.strategy.ExpenseSortStrategy;
import com.example.spendwise.strategy.SortByAmountStrategy;
import com.example.spendwise.strategy.SortByCategoryStrategy;
import com.example.spendwise.strategy.SortByDateStrategy;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tests for Strategy Pattern implementation
 * Tests Sprint 3 design pattern requirements
 */
public class StrategyPatternTest {

    private List<Expense> createTestExpenses() {
        List<Expense> expenses = new ArrayList<>();
        expenses.add(new Expense("Coffee", 5.0, Category.FOOD, "10/25/2024", ""));
        expenses.add(new Expense("Bus Ticket", 2.5, Category.TRANSPORT, "10/23/2024", ""));
        expenses.add(new Expense("Movie", 12.0, Category.ENTERTAINMENT, "10/24/2024", ""));
        expenses.add(new Expense("Lunch", 15.0, Category.FOOD, "10/22/2024", ""));
        return expenses;
    }

    @Test
    public void testSortByDateStrategy() {
        List<Expense> expenses = createTestExpenses();
        ExpenseSortStrategy strategy = new SortByDateStrategy();

        strategy.sort(expenses);

        assertEquals("First expense should be earliest date", "10/22/2024", expenses.get(0).getDate());
        assertEquals("Last expense should be latest date", "10/25/2024", expenses.get(expenses.size() - 1).getDate());
    }

    @Test
    public void testSortByAmountStrategy() {
        List<Expense> expenses = createTestExpenses();
        ExpenseSortStrategy strategy = new SortByAmountStrategy();

        strategy.sort(expenses);

        assertEquals("First expense should have highest amount", 15.0, expenses.get(0).getAmount(), 0.001);
        assertEquals("Last expense should have lowest amount", 2.5, expenses.get(expenses.size() - 1).getAmount(), 0.001);
    }

    @Test
    public void testSortByCategoryStrategy() {
        List<Expense> expenses = createTestExpenses();
        ExpenseSortStrategy strategy = new SortByCategoryStrategy();

        strategy.sort(expenses);

        assertTrue("Expenses should be sorted by category name",
                expenses.get(0).getCategory().getDisplayName()
                        .compareTo(expenses.get(1).getCategory().getDisplayName()) <= 0);
    }

    @Test
    public void testStrategyPatternInterchangeability() {
        List<Expense> expenses1 = createTestExpenses();
        List<Expense> expenses2 = createTestExpenses();

        ExpenseSortStrategy dateStrategy = new SortByDateStrategy();
        ExpenseSortStrategy amountStrategy = new SortByAmountStrategy();

        dateStrategy.sort(expenses1);
        amountStrategy.sort(expenses2);

        String dateOrdering = expenses1.stream()
                .map(Expense::getName)
                .collect(Collectors.joining(","));
        String amountOrdering = expenses2.stream()
                .map(Expense::getName)
                .collect(Collectors.joining(","));

        assertTrue("Different strategies should produce different orderings",
                !dateOrdering.equals(amountOrdering));
    }
}
