package com.example.spendwise.strategy;

import com.example.spendwise.model.Expense;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SortByAmountStrategy implements ExpenseSortStrategy {
    @Override
    public void sort(List<Expense> expenses) {
        Collections.sort(expenses, new Comparator<Expense>() {
            @Override
            public int compare(Expense e1, Expense e2) {
                return Double.compare(e2.getAmount(), e1.getAmount()); // Descending
            }
        });
    }
}