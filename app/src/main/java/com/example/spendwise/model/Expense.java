package com.example.spendwise.model;
import java.util.UUID;

public class Expense {
    private String id;
    private String name;
    private double amount;
    private Category category;
    private String date;
    private String notes;

    public Expense(String name, double amount, Category category, String date, String notes) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.notes = notes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public double getAmount() { return amount; }
    public Category getCategory() { return category; }
    public String getDate() { return date; }
    public String getNotes() { return notes; }
}
