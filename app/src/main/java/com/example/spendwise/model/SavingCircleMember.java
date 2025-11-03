package com.example.spendwise.model;

public class SavingCircleMember {
    private String email;
    private double personalAllocation;  // Starting amount they allocated
    private double currentAmount;       // Current amount (starts at personalAllocation, decreases with expenses)
    private long joinedAt;

    // Default constructor required for Firebase
    public SavingCircleMember() {
    }

    // UPDATED: Constructor now accepts joinedAt timestamp parameter
    public SavingCircleMember(String email, double personalAllocation, long joinedAt) {
        this.email = email;
        this.personalAllocation = personalAllocation;
        this.currentAmount = personalAllocation;  // START WITH THE FULL ALLOCATION
        this.joinedAt = joinedAt; // Use the passed timestamp instead of System.currentTimeMillis()
    }

    // Getters
    public String getEmail() {
        return email;
    }

    public double getPersonalAllocation() {
        return personalAllocation;
    }

    public double getCurrentAmount() {
        return currentAmount;
    }

    public long getJoinedAt() {
        return joinedAt;
    }

    // Setters
    public void setEmail(String email) {
        this.email = email;
    }

    public void setPersonalAllocation(double personalAllocation) {
        this.personalAllocation = personalAllocation;
    }

    public void setCurrentAmount(double currentAmount) {
        this.currentAmount = currentAmount;
    }

    public void setJoinedAt(long joinedAt) {
        this.joinedAt = joinedAt;
    }

    // Utility methods
    public double getSpentAmount() {
        return personalAllocation - currentAmount;
    }

    public double getPercentageRemaining() {
        if (personalAllocation == 0) return 0;
        return (currentAmount / personalAllocation) * 100;
    }

    public double getPercentageSpent() {
        return 100 - getPercentageRemaining();
    }

    public boolean hasMoneyLeft() {
        return currentAmount > 0;
    }

    public boolean hasSpentAll() {
        return currentAmount <= 0;
    }
}