// Aazam's personal tests

package com.example.spendwise;

import static org.junit.Assert.*;
import com.example.spendwise.model.Category;
import com.example.spendwise.model.Budget;
import org.junit.Test;

public class BudgetModelTest {

    @Test
    public void testBudgetCreation() {
        String name = "Food Budget";
        double amount = 500.0;
        Category category = Category.FOOD;
        String date = "10/21/2025";
        String freq = "Monthly";

        Budget budget = new Budget(name, amount, category, date, freq);

        assertEquals("Name should match", name, budget.getName());
        assertEquals("Amount should match", amount, budget.getAmount(), 0.01);
        assertEquals("Category should match", category, budget.getCategory());
        assertEquals("Frequency should match", freq, budget.getfreq());
    }

    @Test
    public void testBudgetFrequencyOptions() {
        Budget weeklyBudget = new Budget("Weekly", 100.0, Category.FOOD, "10/21/2025", "Weekly");
        Budget monthlyBudget = new Budget("Monthly", 500.0, Category.FOOD, "10/21/2025", "Monthly");

        assertEquals("Should accept Weekly", "Weekly", weeklyBudget.getfreq());
        assertEquals("Should accept Monthly", "Monthly", monthlyBudget.getfreq());
    }
}
