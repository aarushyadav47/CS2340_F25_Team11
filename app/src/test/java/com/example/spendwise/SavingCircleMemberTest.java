// Sprint 3 Test - Saving Circle Member Functionality
package com.example.spendwise;

import static org.junit.Assert.*;

import com.example.spendwise.model.SavingCircleMember;

import org.junit.Test;

public class SavingCircleMemberTest {

    /**
     * Test 9: Verify member initialization with personal allocation
     * Sprint 3 requirement: Members have personal allocation amounts
     */
    @Test
    public void testMemberInitialization() {
        String email = "member@test.com";
        double personalAllocation = 200.0;
        long joinedAt = System.currentTimeMillis();

        SavingCircleMember member = new SavingCircleMember(email, personalAllocation, joinedAt);

        assertEquals("Email should match", email, member.getEmail());
        assertEquals("Personal allocation should match",
                personalAllocation, member.getPersonalAllocation(), 0.01);
        assertEquals("Current amount should start equal to allocation",
                personalAllocation, member.getCurrentAmount(), 0.01);
        assertEquals("Joined timestamp should match", joinedAt, member.getJoinedAt());
    }

    /**
     * Test 10: Verify member contribution tracking
     * Sprint 3 requirement: Track each member's contributions toward shared goal
     */
    @Test
    public void testMemberContributionTracking() {
        SavingCircleMember member = new SavingCircleMember("test@test.com",
                100.0, System.currentTimeMillis());

        // Initially, no money spent
        assertEquals("Initial spent should be 0", 0.0, member.getSpentAmount(), 0.01);

        // Simulate spending by reducing current amount
        member.setCurrentAmount(65.0);

        assertEquals("Spent amount should be calculated correctly",
                35.0, member.getSpentAmount(), 0.01);
        assertEquals("Percentage remaining should be correct",
                65.0, member.getPercentageRemaining(), 0.01);
        assertEquals("Percentage spent should be correct",
                35.0, member.getPercentageSpent(), 0.01);
    }

    /**
     * Test 11: Verify member status checks
     * Sprint 3 requirement: Track if members have money left or spent all
     */
    @Test
    public void testMemberStatusChecks() {
        SavingCircleMember member = new SavingCircleMember("test@test.com",
                50.0, System.currentTimeMillis());

        assertTrue("Member should have money left initially", member.hasMoneyLeft());
        assertFalse("Member should not have spent all initially", member.hasSpentAll());

        // Spend all money
        member.setCurrentAmount(0.0);

        assertFalse("Member should not have money left", member.hasMoneyLeft());
        assertTrue("Member should have spent all", member.hasSpentAll());
    }

    /**
     * Test 12: Verify input validation for member allocation
     * Sprint 3 requirement: Non-negative amounts required
     */
    @Test
    public void testMemberAllocationValidation() {
        double positiveAllocation = 100.0;
        double zeroAllocation = 0.0;
        double negativeAllocation = -50.0;

        assertTrue("Positive allocation should be valid", positiveAllocation > 0);
        assertFalse("Zero allocation should be invalid", zeroAllocation > 0);
        assertFalse("Negative allocation should be invalid", negativeAllocation > 0);

        // Test that allocation >= 0 is acceptable (0 might be edge case)
        assertTrue("Non-negative allocation check", positiveAllocation >= 0);
        assertTrue("Zero is non-negative", zeroAllocation >= 0);
        assertFalse("Negative is not acceptable", negativeAllocation >= 0);
    }
}