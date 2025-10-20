package com.example.spendwise;

import com.example.spendwise.viewModel.BudgetViewModel;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for BudgetViewModel
 * Tests Sprint 2 requirements:
 * - Budget Window Calculator (2-of-3 logic)
 * - Budget creation validation
 * - Budget utilization calculations
 */
public class BudgetViewModelTest {

    private BudgetViewModel viewModel;
    private static final double DELTA = 0.01; // For floating point comparisons

    @Before
    public void setUp() {
        viewModel = new BudgetViewModel();
    }

    // ========== Budget Calculator (2-of-3) Tests ==========

    @Test
    public void testCalculateBudgetValues_withTotalAndSpent_calculatesRemaining() {
        // Arrange - Sprint 2 requirement: given Total and Spent, calculate Remaining
        Double totalBudget = 500.0;
        Double spentToDate = 200.0;
        Double remaining = null; // This should be calculated

        // Act
        boolean result = viewModel.calculateBudgetValues(totalBudget, spentToDate, remaining);

        // Assert
        assertTrue("Should successfully calculate remaining", result);
        assertEquals(300.0, viewModel.getCalculatedValue().getValue(), DELTA);
        assertTrue(viewModel.getCalculationResult().getValue().contains("Remaining calculated"));
    }

    @Test
    public void testCalculateBudgetValues_withTotalAndRemaining_calculatesSpent() {
        // Arrange - Sprint 2 requirement: given Total and Remaining, calculate Spent
        Double totalBudget = 500.0;
        Double spentToDate = null; // This should be calculated
        Double remaining = 300.0;

        // Act
        boolean result = viewModel.calculateBudgetValues(totalBudget, spentToDate, remaining);

        // Assert
        assertTrue("Should successfully calculate spent", result);
        assertEquals(200.0, viewModel.getCalculatedValue().getValue(), DELTA);
        assertTrue(viewModel.getCalculationResult().getValue().contains("Spent-to-Date calculated"));
    }

    @Test
    public void testCalculateBudgetValues_withSpentAndRemaining_calculatesTotal() {
        // Arrange - Sprint 2 requirement: given Spent and Remaining, calculate Total
        Double totalBudget = null; // This should be calculated
        Double spentToDate = 200.0;
        Double remaining = 300.0;

        // Act
        boolean result = viewModel.calculateBudgetValues(totalBudget, spentToDate, remaining);

        // Assert
        assertTrue("Should successfully calculate total", result);
        assertEquals(500.0, viewModel.getCalculatedValue().getValue(), DELTA);
        assertTrue(viewModel.getCalculationResult().getValue().contains("Total Budget calculated"));
    }

    @Test
    public void testCalculateBudgetValues_withOnlyOneValue_returnsFalse() {
        // Arrange - Need at least 2 values
        Double totalBudget = 500.0;
        Double spentToDate = null;
        Double remaining = null;

        // Act
        boolean result = viewModel.calculateBudgetValues(totalBudget, spentToDate, remaining);

        // Assert
        assertFalse("Should fail with only one value provided", result);
        assertEquals("Please provide at least 2 values",
                viewModel.getCalculationResult().getValue());
    }

    @Test
    public void testCalculateBudgetValues_withNoValues_returnsFalse() {
        // Arrange - No values provided
        Double totalBudget = null;
        Double spentToDate = null;
        Double remaining = null;

        // Act
        boolean result = viewModel.calculateBudgetValues(totalBudget, spentToDate, remaining);

        // Assert
        assertFalse("Should fail with no values provided", result);
        assertEquals("Please provide at least 2 values",
                viewModel.getCalculationResult().getValue());
    }

    @Test
    public void testCalculateBudgetValues_withAllThreeConsistent_returnsTrue() {
        // Arrange - All three values provided and consistent
        Double totalBudget = 500.0;
        Double spentToDate = 200.0;
        Double remaining = 300.0;

        // Act
        boolean result = viewModel.calculateBudgetValues(totalBudget, spentToDate, remaining);

        // Assert
        assertTrue("Should verify consistent values", result);
        assertEquals("All values verified",
                viewModel.getCalculationResult().getValue());
    }

    @Test
    public void testCalculateBudgetValues_withAllThreeInconsistent_returnsFalse() {
        // Arrange - All three values provided but inconsistent
        Double totalBudget = 500.0;
        Double spentToDate = 200.0;
        Double remaining = 400.0; // Should be 300, not 400!

        // Act
        boolean result = viewModel.calculateBudgetValues(totalBudget, spentToDate, remaining);

        // Assert
        assertFalse("Should detect inconsistent values", result);
        assertEquals("Values are inconsistent",
                viewModel.getCalculationResult().getValue());
    }

    // ========== Individual Calculation Method Tests ==========

    @Test
    public void testCalculateRemaining_validInputs_returnsCorrectValue() {
        // Act
        double result = viewModel.calculateRemaining(500.0, 200.0);

        // Assert
        assertEquals(300.0, result, DELTA);
    }

}
