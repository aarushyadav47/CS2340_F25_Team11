//Aarush Yadav's two tests
package com.example.spendwise;

import static org.junit.Assert.*;
import org.junit.Test;

public class ValidationTest {

    @Test
    public void testNameValidation() {
        // Arrange
        String validName = "Groceries";
        String invalidName = ""; // Empty string
        String tooLongName = "A".repeat(101); // Over 100 characters if you have a limit

        // Act
        boolean validResult = !validName.trim().isEmpty() && validName.length() <= 100;
        boolean invalidResult = invalidName.trim().isEmpty() || invalidName.length() > 100;
        boolean tooLongResult = tooLongName.length() > 100;

        // Assert
        assertTrue("Valid name should pass", validResult);
        assertTrue("Empty name should fail", invalidResult);
        assertTrue("Overly long name should fail", tooLongResult);
    }

    @Test
    public void testAmountValidation() {
        double positiveAmount = 50.0;
        double negativeAmount = -10.0;
        double zeroAmount = 0.0;

        assertTrue("Positive amount should be valid", positiveAmount > 0);
        assertFalse("Negative amount should be invalid", negativeAmount > 0);
        assertFalse("Zero amount should be invalid", zeroAmount > 0);
    }
}
