//Suhani Gandhi's test

package com.example.spendwise;

import static org.junit.Assert.*;
import com.example.spendwise.model.Category;
import com.example.spendwise.model.Expense;
import org.junit.Test;

public class ExpenseModelTest {

    @Test
    public void testExpenseCreation() {
        String name = "Coffee";
        double amount = 5.50;
        Category category = Category.FOOD;
        String date = "10/21/2025";
        String notes = "Morning coffee";

        Expense expense = new Expense(name, amount, category, date, notes);

        assertEquals("Name should match", name, expense.getName());
        assertEquals("Amount should match", amount, expense.getAmount(), 0.01);
        assertEquals("Category should match", category, expense.getCategory());
        assertEquals("Date should match", date, expense.getDate());
        assertEquals("Notes should match", notes, expense.getNotes());
    }

    @Test
    public void testExpenseIdSetAndGet() {
        Expense expense = new Expense("Test", 10.0, Category.OTHER, "10/21/2025", "");
        String testId = "expense123";

        expense.setId(testId);

        assertEquals("ID should match", testId, expense.getId());
    }
}