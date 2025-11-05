package com.example.spendwise.model;

import java.util.List;

public class Budget {
    private String name;
    private double limit;
    private List<ExpenseRecord> expenses;

    public Budget(String name, double limit) {
        this.name = name;
        this.limit = limit;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
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
}
