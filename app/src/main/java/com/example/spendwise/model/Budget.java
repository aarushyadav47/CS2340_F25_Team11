package com.example.spendwise.model;

import java.util.ArrayList;
import java.util.List;

public class Budget {
    private final double amount;
    private String name;
    private double limit;
    private List<ExpenseRecord> expenses;
    private String category;
    private String frequency;
    private String startDate;

    public Budget(double amount, String name, double limit) {
        this.amount = amount;
        this.name = name;
        this.limit = limit;
        this.expenses = new ArrayList<>();
        this.category = category;
        this.frequency = frequency;
        this.startDate = startDate;
    }

    public Budget(double amount) {
        this.amount = amount;
        this.expenses = new ArrayList<>();
    }

    public Budget(String name, double amount, String category, String frequency, String startDate) {
        this.name = name;
        this.amount = amount;
        this.category = category;
        this.frequency = frequency;
        this.startDate = startDate;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public double getAmount() {
        return amount;
    }

    public double getLimit() {
        return limit;
    }
    public void setLimit(double limit) {
        this.limit = limit;
    }

    public List<ExpenseRecord> getExpenses() {
        return expenses;
    }
    public void addExpense(ExpenseRecord expense) {
        if(expenses != null) expenses.add(expense);
    }

    public double getTotalSpent() {
        return expenses.stream().mapToDouble(ExpenseRecord::getAmount).sum();
    }

    public double getRemaining() {
        return limit - getTotalSpent();
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    public String getFrequency() {
        return frequency;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }
}
