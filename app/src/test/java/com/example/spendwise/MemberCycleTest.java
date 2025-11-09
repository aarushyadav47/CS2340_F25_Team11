// Sprint 3 Test - Member Cycle Functionality
package com.example.spendwise;

import static org.junit.Assert.*;

import com.example.spendwise.model.MemberCycle;

import org.junit.Test;

import java.util.Calendar;

public class MemberCycleTest {

    /**
     * Test 5: Verify cycle date range calculation
     * Sprint 3 requirement: Challenge periods correctly start on acceptance date
     */
    @Test
    public void testCycleDateRangeCalculation() {
        Calendar startCal = Calendar.getInstance();
        startCal.set(2025, Calendar.NOVEMBER, 1, 0, 0, 0);
        long startDate = startCal.getTimeInMillis();

        Calendar endCal = Calendar.getInstance();
        endCal.set(2025, Calendar.NOVEMBER, 8, 0, 0, 0);
        long endDate = endCal.getTimeInMillis();

        double startAmount = 100.0;

        MemberCycle cycle = new MemberCycle(startDate, endDate, startAmount);

        assertNotNull("Cycle ID should be generated", cycle.getCycleId());
        assertEquals("Start date should match", startDate, cycle.getStartDate());
        assertEquals("End date should match", endDate, cycle.getEndDate());
        assertEquals("Start amount should match", startAmount, cycle.getStartAmount(), 0.01);
        assertEquals("End amount should initially equal start amount",
                startAmount, cycle.getEndAmount(), 0.01);
        assertFalse("Cycle should not be complete initially", cycle.isComplete());
    }

    /**
     * Test 6: Verify expense tracking in cycle
     * Sprint 3 requirement: Member contributions toward shared goal tracked
     */
    @Test
    public void testCycleExpenseTracking() {
        long startDate = System.currentTimeMillis();
        long endDate = startDate + (7 * 24 * 60 * 60 * 1000L); // 1 week later
        double startAmount = 100.0;

        MemberCycle cycle = new MemberCycle(startDate, endDate, startAmount);

        // Record some expenses
        cycle.recordExpense(20.0);
        cycle.recordExpense(15.0);

        assertEquals("Spent amount should be tracked", 35.0, cycle.getSpent(), 0.01);
        assertEquals("End amount should be reduced", 65.0, cycle.getEndAmount(), 0.01);

        // Verify percentage calculations
        double expectedPercentage = (35.0 / 100.0) * 100;
        assertEquals("Percentage spent should be correct",
                expectedPercentage, cycle.getPercentageSpent(), 0.1);
    }

    /**
     * Test 7: Verify cycle completion status
     * Sprint 3 requirement: Goals automatically indicate complete if satisfied
     */
    @Test
    public void testCycleCompletionStatus() {
        Calendar past = Calendar.getInstance();
        past.add(Calendar.DAY_OF_MONTH, -10); // 10 days ago
        long startDate = past.getTimeInMillis();

        Calendar pastEnd = Calendar.getInstance();
        pastEnd.add(Calendar.DAY_OF_MONTH, -3); // 3 days ago
        long endDate = pastEnd.getTimeInMillis();

        MemberCycle cycle = new MemberCycle(startDate, endDate, 100.0);

        assertTrue("Cycle should be marked as should-be-complete if end date passed",
                cycle.shouldBeComplete());

        // Mark it complete
        cycle.completeCycle(65.0);

        assertTrue("Cycle should be complete after calling completeCycle",
                cycle.isComplete());
        assertEquals("Final amount should be set", 65.0, cycle.getEndAmount(), 0.01);
    }

    /**
     * Test 8: Verify date checking within cycle
     * Sprint 3 requirement: Respect Dashboard's date selector for period calculations
     */
    @Test
    public void testDateInCycleCheck() {
        Calendar start = Calendar.getInstance();
        start.set(2025, Calendar.NOVEMBER, 1, 0, 0, 0);
        long startDate = start.getTimeInMillis();

        Calendar end = Calendar.getInstance();
        end.set(2025, Calendar.NOVEMBER, 30, 23, 59, 59);
        long endDate = end.getTimeInMillis();

        MemberCycle cycle = new MemberCycle(startDate, endDate, 100.0);

        // Test date within cycle
        Calendar mid = Calendar.getInstance();
        mid.set(2025, Calendar.NOVEMBER, 15, 12, 0, 0);
        long midDate = mid.getTimeInMillis();

        assertTrue("Date within cycle should return true",
                cycle.isDateInCycle(midDate));

        // Test date before cycle
        Calendar before = Calendar.getInstance();
        before.set(2025, Calendar.OCTOBER, 15, 12, 0, 0);
        long beforeDate = before.getTimeInMillis();

        assertFalse("Date before cycle should return false",
                cycle.isDateInCycle(beforeDate));

        // Test date after cycle
        Calendar after = Calendar.getInstance();
        after.set(2025, Calendar.DECEMBER, 15, 12, 0, 0);
        long afterDate = after.getTimeInMillis();

        assertFalse("Date after cycle should return false",
                cycle.isDateInCycle(afterDate));
    }
}


