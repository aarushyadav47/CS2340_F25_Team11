package com.example.spendwise.factory;

import com.example.spendwise.model.Budget;
import com.example.spendwise.model.Expense;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory Pattern: Centralizes chart creation logic
 */
public class ChartFactory {

    public static PieData createCategoryPieChart(List<Expense> expenses) {
        Map<String, Float> categoryTotals = new HashMap<>();

        for (Expense expense : expenses) {
            String category = expense.getCategory().getDisplayName();
            float amount = (float) expense.getAmount();
            categoryTotals.put(category, categoryTotals.getOrDefault(category, 0f) + amount);
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : categoryTotals.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        if (entries.isEmpty()) {
            entries.add(new PieEntry(100f, "No Data"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Expenses by Category");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);

        return new PieData(dataSet);
    }

    public static BarData createBudgetBarChart(List<Budget> budgets, List<Expense> expenses) {
        List<BarEntry> spentEntries = new ArrayList<>();
        List<BarEntry> targetEntries = new ArrayList<>();

        for (int i = 0; i < budgets.size() && i < 5; i++) {
            Budget budget = budgets.get(i);

            double spent = 0;
            for (Expense expense : expenses) {
                if (expense.getCategory() == budget.getCategory()) {
                    spent += expense.getAmount();
                }
            }

            spentEntries.add(new BarEntry(i, (float) spent));
            targetEntries.add(new BarEntry(i, (float) budget.getOriginalAmount()));
        }

        if (spentEntries.isEmpty()) {
            spentEntries.add(new BarEntry(0, 0f));
            targetEntries.add(new BarEntry(0, 100f));
        }

        BarDataSet spentSet = new BarDataSet(spentEntries, "Spent");
        spentSet.setColor(0xFFFF6B6B);

        BarDataSet targetSet = new BarDataSet(targetEntries, "Target");
        targetSet.setColor(0xFF4ECDC4);

        BarData barData = new BarData(spentSet, targetSet);
        barData.setBarWidth(0.4f);

        return barData;
    }
}