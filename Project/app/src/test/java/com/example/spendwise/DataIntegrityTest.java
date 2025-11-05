//Daanish Mehra's Tests

package com.example.spendwise;

import static org.junit.Assert.*;
import com.example.spendwise.model.Category;
import com.example.spendwise.model.Expense;
import org.junit.Test;

public class DataIntegrityTest {

    @Test
    public void testExpenseWithEmptyNotes() {
        Expense expense = new Expense("Test", 10.0, Category.FOOD, "10/21/2025", "");

        assertNotNull("Expense should be created", expense);
        assertEquals("Empty notes should be allowed", "", expense.getNotes());
    }

    @Test
    public void testMultipleExpensesSameCategory() {
        Expense expense1 = new Expense("Coffee", 5.0, Category.FOOD, "10/21/2025", "");
        Expense expense2 = new Expense("Lunch", 15.0, Category.FOOD, "10/21/2025", "");

        assertEquals("Both expenses should have FOOD category",
                expense1.getCategory(), expense2.getCategory());
    }
}
