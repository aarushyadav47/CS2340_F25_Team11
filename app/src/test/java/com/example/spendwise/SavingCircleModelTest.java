package com.example.spendwise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.example.spendwise.model.SavingCircle;

import org.junit.Test;

/**
 * Tests for SavingCircle model
 * Tests Sprint 3 Savings Circles functionality
 */
public class SavingCircleModelTest {

    @Test
    public void testSavingCircleCreation() {
        String groupName = "College Friends";
        String creatorEmail = "creator@example.com";
        String challengeTitle = "Save for Spring Break";
        double goalAmount = 500.0;
        String frequency = "Monthly";
        String notes = "Let's save together!";
        long createdAt = System.currentTimeMillis();

        SavingCircle circle = new SavingCircle(
                groupName, creatorEmail, challengeTitle, goalAmount, frequency, notes, createdAt);

        assertNotNull("SavingCircle ID should be generated", circle.getId());
        assertEquals("Group name should match", groupName, circle.getGroupName());
        assertEquals("Creator email should match", creatorEmail, circle.getCreatorEmail());
        assertEquals("Goal amount should match", goalAmount, circle.getGoalAmount(), 0.001);
        assertEquals("Frequency should match", frequency, circle.getFrequency());
    }

    @Test
    public void testSavingCircleFrequencyOptions() {
        long timestamp = System.currentTimeMillis();
        SavingCircle weeklyCircle = new SavingCircle(
                "Weekly Group", "test@example.com", "Weekly Challenge",
                100.0, "Weekly", "", timestamp);

        SavingCircle monthlyCircle = new SavingCircle(
                "Monthly Group", "test@example.com", "Monthly Challenge",
                500.0, "Monthly", "", timestamp);

        assertEquals("Should accept Weekly frequency", "Weekly", weeklyCircle.getFrequency());
        assertEquals("Should accept Monthly frequency", "Monthly", monthlyCircle.getFrequency());
    }
}
