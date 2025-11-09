package com.example.spendwise.strategy;

import com.example.spendwise.model.Expense;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SortByCategoryStrategy implements ExpenseSortStrategy {
    @Override
    public void sort(List<Expense> expenses) {
        Collections.sort(expenses, new Comparator<Expense>() {
            @Override
            public int compare(Expense e1, Expense e2) {
                return e1.getCategory().getDisplayName()
                        .compareTo(e2.getCategory().getDisplayName());
            }
        });
    }
}