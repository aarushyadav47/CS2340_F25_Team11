package com.example.spendwise.model;
import java.util.UUID;

public class Expense {
    private String id;
    private String name;
    private double amount;
    private Category category;
    private String date;
    private String notes;
    private String savingCircleId; // Optional: ID of savings circle if expense is linked to a circle

    // Default constructor for Firebase
    public Expense() {
    }

    // Constructor without saving circle link
    public Expense(String name, double amount, Category category, String date, String notes) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.notes = notes;
        this.savingCircleId = null;
    }

    // Constructor with saving circle link
    public Expense(String name, double amount, Category category, String date, String notes, String savingCircleId) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.notes = notes;
        this.savingCircleId = savingCircleId;
    }

    // Getter and setter methods
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    // Get expense name
    public String getName() {
        return name; }
    // Get expense amount
    public double getAmount() {
        return amount; }
    // Get expense category
    public Category getCategory() {
        return category; }
    public String getDate() {
        return date; }
    public String getNotes() {
        return notes; }
    public String getSavingCircleId() {
        return savingCircleId;
    }
    public void setSavingCircleId(String savingCircleId) {
        this.savingCircleId = savingCircleId;
    }
    // Check if expense is linked to a saving circle
    public boolean isLinkedToSavingCircle() {
        return savingCircleId != null && !savingCircleId.isEmpty();
    }
}
