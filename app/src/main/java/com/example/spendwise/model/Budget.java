package com.example.spendwise.model;

import java.util.UUID;

public class Budget {
    private String id;
    private String name;
    private double amount;
    private double originalAmount;
    private Category category;
    private String date;
    private String freq;

    public Budget() {
    }

    public Budget(String name, double amount, Category category, String date,
                  String freq) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.amount = amount;
        this.originalAmount = amount;
        this.category = category;
        this.date = date;
        this.freq = freq;
    }

    public Budget(String name, double amount, double originalAmount,
                  Category category, String date, String freq) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.amount = amount;
        this.originalAmount = originalAmount;
        this.category = category;
        this.date = date;
        this.freq = freq;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public double getAmount() {
        return amount;
    }

    public double getOriginalAmount() {
        return originalAmount;
    }

    public Category getCategory() {
        return category;
    }

    public String getDate() {
        return date;
    }

    public String getfreq() {
        return freq;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setOriginalAmount(double originalAmount) {
        this.originalAmount = originalAmount;
    }
}