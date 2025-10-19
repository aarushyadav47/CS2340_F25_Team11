package com.example.spendwise.model;

import java.time.LocalDate;
import java.util.UUID;

public class ExpenseRecord {
    private String id;
    private Expense category;
    private String name;
    private double amount;
    private LocalDate date;

    public ExpenseRecord(Expense category, String name, double amount, LocalDate date) {
        this.id = UUID.randomUUID().toString();
        this.category = category;
        this.name = name;
        this.amount = amount;
        this.date = date;
    }

    public String getId() {
        return id;
    }
    public Expense getCategory() {
        return category;
    }
    public String getName() {
        return name;
    }
    public double getAmount() {
        return amount;
    }
    public LocalDate getDate() {
        return date;
    }
}
