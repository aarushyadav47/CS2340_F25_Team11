package com.example.spendwise.model;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MemberCycle {
    private String cycleId;           // e.g., "2024-10-01_to_2024-11-01"
    private long startDate;           // Cycle start timestamp
    private long endDate;             // Cycle end timestamp
    private double startAmount;       // Amount at beginning of cycle
    private double endAmount;         // Amount at end of cycle (or current if ongoing)
    private double spent;             // Total spent this cycle
    private double contributed;       // Any additional contributions this cycle
    private boolean isComplete;       // Is this cycle finished?
    private boolean goalReached;      // Did member reach their goal this cycle?

    // Default constructor for Firebase
    public MemberCycle() {
    }

    public MemberCycle(long startDate, long endDate, double startAmount) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.startAmount = startAmount;
        this.endAmount = startAmount;  // Will be updated as expenses happen
        this.spent = 0;
        this.contributed = 0;
        this.isComplete = false;
        this.goalReached = false;
        this.cycleId = generateCycleId(startDate, endDate);
    }

    // Generate a readable cycle ID
    private String generateCycleId(long startDate, long endDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date(startDate)) + "_to_" + sdf.format(new Date(endDate));
    }

    // Calculate the next cycle dates based on frequency
    public static MemberCycle createNextCycle(MemberCycle previousCycle, String frequency) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(previousCycle.getEndDate());

        long newStartDate = previousCycle.getEndDate();
        long newEndDate;

        if ("Weekly".equals(frequency)) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
        } else { // Monthly
            calendar.add(Calendar.MONTH, 1);
        }

        newEndDate = calendar.getTimeInMillis();

        return new MemberCycle(newStartDate, newEndDate, previousCycle.getStartAmount());
    }

    // Helper method to check if a date falls within this cycle
    public boolean isDateInCycle(long timestamp) {
        return timestamp >= startDate && timestamp < endDate;
    }

    // Helper method to check if cycle should be complete by now
    public boolean shouldBeComplete() {
        return System.currentTimeMillis() >= endDate;
    }

    // Update amounts when expense is added
    public void recordExpense(double amount) {
        this.endAmount -= amount;
        this.spent += amount;
        if (this.endAmount < 0) {
            this.endAmount = 0;
        }
    }

    public void restoreExpense(double amount) {
        this.endAmount += amount;
        if (this.endAmount > this.startAmount + this.contributed) {
            this.endAmount = this.startAmount + this.contributed;
        }
        this.spent -= amount;
        if (this.spent < 0) {
            this.spent = 0;
        }
    }

    // Update amounts when contribution is added
    public void recordContribution(double amount) {
        this.endAmount += amount;
        this.contributed += amount;
    }

    // Close the cycle and mark as complete
    public void completeCycle(double finalAmount) {
        this.endAmount = finalAmount;
        this.spent = startAmount - endAmount + contributed;
        this.isComplete = true;
    }

    // Getters and Setters
    public String getCycleId() { return cycleId; }
    public void setCycleId(String cycleId) { this.cycleId = cycleId; }

    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }

    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }

    public double getStartAmount() { return startAmount; }
    public void setStartAmount(double startAmount) { this.startAmount = startAmount; }

    public double getEndAmount() { return endAmount; }
    public void setEndAmount(double endAmount) { this.endAmount = endAmount; }

    public double getSpent() { return spent; }
    public void setSpent(double spent) { this.spent = spent; }

    public double getContributed() { return contributed; }
    public void setContributed(double contributed) { this.contributed = contributed; }

    public boolean isComplete() { return isComplete; }
    public void setComplete(boolean complete) { isComplete = complete; }

    public boolean isGoalReached() { return goalReached; }
    public void setGoalReached(boolean goalReached) { this.goalReached = goalReached; }

    // Utility methods
    public double getNetChange() {
        return endAmount - startAmount;
    }

    public double getPercentageSpent() {
        if (startAmount == 0) return 0;
        return (spent / startAmount) * 100;
    }

    public String getCyclePeriodDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.US);
        return sdf.format(new Date(startDate)) + " - " + sdf.format(new Date(endDate));
    }
}