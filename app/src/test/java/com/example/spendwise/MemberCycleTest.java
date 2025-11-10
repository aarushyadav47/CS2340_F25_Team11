package com.example.spendwise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.example.spendwise.model.MemberCycle;

import org.junit.Test;

import java.util.Calendar;

public class MemberCycleTest {

    @Test
    public void createNextCycle_resetsAllocationToInitialAmount() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2024, Calendar.SEPTEMBER, 1);
        long start = calendar.getTimeInMillis();
        calendar.add(Calendar.MONTH, 1);
        long end = calendar.getTimeInMillis();

        MemberCycle currentCycle = new MemberCycle(start, end, 200.0);
        currentCycle.recordExpense(50.0);

        MemberCycle nextCycle = MemberCycle.createNextCycle(currentCycle, "Monthly");

        assertEquals(200.0, nextCycle.getStartAmount(), 0.001);
        assertEquals(200.0, nextCycle.getEndAmount(), 0.001);
        assertNotEquals(currentCycle.getCycleId(), nextCycle.getCycleId());
    }

    @Test
    public void restoreExpense_doesNotExceedInitialAllocation() {
        Calendar calendar = Calendar.getInstance();
        long start = calendar.getTimeInMillis();
        calendar.add(Calendar.WEEK_OF_YEAR, 1);
        long end = calendar.getTimeInMillis();

        MemberCycle cycle = new MemberCycle(start, end, 150.0);
        cycle.recordExpense(40.0);

        cycle.restoreExpense(20.0);

        assertEquals(130.0, cycle.getEndAmount(), 0.001);
        assertEquals(20.0, cycle.getSpent(), 0.001);

        cycle.restoreExpense(50.0);
        assertEquals(150.0, cycle.getEndAmount(), 0.001);
        assertEquals(0.0, cycle.getSpent(), 0.001);
    }
}
