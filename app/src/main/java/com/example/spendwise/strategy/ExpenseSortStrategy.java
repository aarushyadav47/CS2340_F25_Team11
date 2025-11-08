package com.example.spendwise.strategy;

import com.example.spendwise.model.Expense;
import java.util.List;

/**
 * Strategy Pattern: Defines sorting behavior interface
 */
public interface ExpenseSortStrategy {
    void sort(List<Expense> expenses);
}