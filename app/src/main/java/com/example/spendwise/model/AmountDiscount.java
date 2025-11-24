package com.example.spendwise.model;

public class AmountDiscount implements Discount {

    private double amount;

    public AmountDiscount(double amount) {
        this.amount = amount;
    }

    @Override
    public double applyDiscount(double price) {
        return Math.max(0, price - amount);
    }
}

