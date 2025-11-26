package com.example.spendwise.logic;

import org.junit.Test;
import static org.junit.Assert.*;

public class BudgetAlertLogicTest {

    private final BudgetAlertLogic logic = new BudgetAlertLogic();

    @Test
    public void testIsNearLimit_True() {
        // Limit 100, Spent 85, Threshold 0.8 (80%)
        assertTrue(logic.isNearLimit(85.0, 100.0, 0.8));
    }

    @Test
    public void testIsNearLimit_False_BelowThreshold() {
        // Limit 100, Spent 50, Threshold 0.8
        assertFalse(logic.isNearLimit(50.0, 100.0, 0.8));
    }

    @Test
    public void testIsNearLimit_False_Exceeded() {
        // Limit 100, Spent 100, Threshold 0.8
        // Should return false because it's not "near", it's "reached/exceeded" (handled separately usually, or inclusive?)
        // My implementation: spent < limit for "near".
        assertFalse(logic.isNearLimit(100.0, 100.0, 0.8));
    }

    @Test
    public void testIsExceeded_True() {
        assertTrue(logic.isExceeded(105.0, 100.0));
        assertTrue(logic.isExceeded(100.0, 100.0));
    }

    @Test
    public void testIsExceeded_False() {
        assertFalse(logic.isExceeded(99.9, 100.0));
    }

    @Test
    public void testGetNearLimitMessage() {
        String msg = logic.getNearLimitMessage("Food", 85.0, 100.0);
        assertEquals("You've reached 85% of your Food budget ($85.00 / $100.00)", msg);
    }
    
    @Test
    public void testGetExceededMessage() {
        String msg = logic.getExceededMessage("Food", 110.0, 100.0);
        assertEquals("You've exceeded your Food budget! ($110.00 / $100.00)", msg);
    }
}
