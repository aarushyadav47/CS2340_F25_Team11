package com.example.spendwise.strategy;

import com.example.spendwise.model.Expense;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SortByDateStrategy implements ExpenseSortStrategy {
    @Override
    public void sort(List<Expense> expenses) {
        Collections.sort(expenses, new Comparator<Expense>() {
            @Override
            public int compare(Expense e1, Expense e2) {
                return e1.getDate().compareTo(e2.getDate());
            }
        });
    }
}