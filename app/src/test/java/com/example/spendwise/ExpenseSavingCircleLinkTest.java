package com.example.spendwise;

import static org.junit.Assert.*;
import com.example.spendwise.model.Category;
import com.example.spendwise.model.Expense;
import org.junit.Test;

public class ExpenseSavingCircleLinkTest {

    @Test
    public void testExpenseLinkedToSavingCircle() {
        String savingCircleId = "circle123";
        Expense linkedExpense = new Expense("Group dinner", 45.0, Category.FOOD, 
                                           "11/26/2025", "Team outing", savingCircleId);

        assertTrue("Expense should be linked to saving circle", 
                   linkedExpense.isLinkedToSavingCircle());
        assertEquals("Saving circle ID should match", savingCircleId, 
                     linkedExpense.getSavingCircleId());
        
        // Test unlinking
        linkedExpense.setSavingCircleId(null);
        assertFalse("Expense should not be linked after setting null", 
                    linkedExpense.isLinkedToSavingCircle());
        
        // Test empty string
        linkedExpense.setSavingCircleId("");
        assertFalse("Expense should not be linked with empty string", 
                    linkedExpense.isLinkedToSavingCircle());
    }
}

