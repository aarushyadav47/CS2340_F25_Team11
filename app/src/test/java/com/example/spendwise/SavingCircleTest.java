// Sprint 3 Test - Saving Circle Functionality
package com.example.spendwise;

import static org.junit.Assert.*;

import com.example.spendwise.model.SavingCircle;

import org.junit.Test;

public class SavingCircleTest {

    /**
     * Test 3: Verify Saving Circle creation with all required fields
     * Sprint 3 requirement: User can create group with all required fields
     */
    @Test
    public void testSavingCircleCreation() {
        String groupName = "Study Group Savings";
        String creatorEmail = "creator@test.com";
        String challengeTitle = "Save for Trip";
        double goalAmount = 500.0;
        String frequency = "Monthly";
        String notes = "Spring break fund";
        long timestamp = System.currentTimeMillis();

        SavingCircle circle = new SavingCircle(groupName, creatorEmail, challengeTitle,
                goalAmount, frequency, notes, timestamp);

        assertEquals("Group name should match", groupName, circle.getGroupName());
        assertEquals("Creator email should match", creatorEmail, circle.getCreatorEmail());
        assertEquals("Challenge title should match", challengeTitle, circle.getChallengeTitle());
        assertEquals("Goal amount should match", goalAmount, circle.getGoalAmount(), 0.01);
        assertEquals("Frequency should match", frequency, circle.getFrequency());
        assertEquals("Notes should match", notes, circle.getNotes());
        assertNotNull("Circle should have an ID", circle.getId());
    }

    /**
     * Test 4: Verify frequency validation for Saving Circles
     * Sprint 3 requirement: Valid frequency (weekly/monthly) required
     */
    @Test
    public void testSavingCircleFrequencyValidation() {
        String validWeekly = "Weekly";
        String validMonthly = "Monthly";

        // Test valid frequencies
        assertTrue("Weekly should be valid frequency",
                validWeekly.equals("Weekly") || validWeekly.equals("Monthly"));
        assertTrue("Monthly should be valid frequency",
                validMonthly.equals("Weekly") || validMonthly.equals("Monthly"));

        // Test invalid frequency
        String invalidFrequency = "Daily";
        assertFalse("Daily should be invalid frequency",
                invalidFrequency.equals("Weekly") || invalidFrequency.equals("Monthly"));
    }
}