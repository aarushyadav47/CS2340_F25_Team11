package com.example.spendwise;

import com.example.spendwise.viewModel.ExpenseViewModel;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for ExpenseViewModel
 * Tests expense management functionality:
 * - ViewModel initialization
 * - Adding expenses with validation
 */
public class ExpenseViewModelTest {

    private ExpenseViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new ExpenseViewModel();
    }

    // ========== ViewModel Initialization Tests ==========

    @Test
    public void testViewModel_initialization_liveDataNotNull() {
        // Assert that LiveData objects are properly initialized
        assertNotNull("Login result LiveData should not be null",
                viewModel.getLoginResult());
    }

    @Test
    public void testViewModel_initialization_createsInstance() {
        // Assert that ViewModel can be instantiated
        assertNotNull("ViewModel should be instantiated", viewModel);
    }

    // ========== Add Expense Tests ==========

    @Test
    public void testAddExpense_validInputs_doesNotThrowException() {
        // Act - Add a valid expense
        viewModel.addExpense("Coffee", "5.50", "Food", "2025-10-19");

        // Assert - No exception should be thrown
        assertTrue("Add expense should execute without exception", true);
    }

    @Test
    public void testAddExpense_withEmptyName_setsErrorMessage() {
        // Act - Add expense with empty name
        viewModel.addExpense("", "5.50", "Food", "2025-10-19");

        // Assert - Should set error message
        assertNotNull("Login result should not be null", viewModel.getLoginResult().getValue());
        assertEquals("Please enter valid data in the input fields",
                viewModel.getLoginResult().getValue());
    }

    @Test
    public void testAddExpense_withNullAmount_setsErrorMessage() {
        // Act - Add expense with null amount
        viewModel.addExpense("Coffee", null, "Food", "2025-10-19");

        // Assert - Should set error message
        assertNotNull("Login result should not be null", viewModel.getLoginResult().getValue());
        assertEquals("Please enter valid data in the input fields",
                viewModel.getLoginResult().getValue());
    }

    @Test
    public void testAddExpense_withEmptyDate_setsErrorMessage() {
        // Act - Add expense with empty date
        viewModel.addExpense("Coffee", "5.50", "Food", "");

        // Assert - Should set error message
        assertNotNull("Login result should not be null", viewModel.getLoginResult().getValue());
        assertEquals("Please enter valid data in the input fields",
                viewModel.getLoginResult().getValue());
    }

    @Test
    public void testAddExpense_withAllEmptyFields_setsErrorMessage() {
        // Act - Add expense with all empty fields
        viewModel.addExpense("", null, "", "");

        // Assert - Should set error message
        assertNotNull("Login result should not be null", viewModel.getLoginResult().getValue());
        assertEquals("Please enter valid data in the input fields",
                viewModel.getLoginResult().getValue());
    }

    // ========== LiveData Tests ==========

}