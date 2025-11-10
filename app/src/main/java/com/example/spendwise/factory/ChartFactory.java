package com.example.spendwise.factory;

import android.content.Context;

import com.example.spendwise.model.Budget;
import com.example.spendwise.model.Expense;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
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

public class ChartFactory {

    /** Creates a fully configured PieChart */
    public static PieChart createPieChart(Context context, List<Expense> expenses) {
        PieChart pieChart = new PieChart(context);

        // Configure chart appearance
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);
        pieChart.setEntryLabelColor(android.graphics.Color.BLACK);
        pieChart.setEntryLabelTextSize(11f);

        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);

        // Set chart data
        pieChart.setData(createPieData(expenses));
        pieChart.invalidate(); // refresh

        return pieChart;
    }

    /** Creates a fully configured BarChart */
    public static BarChart createBarChart(Context context, List<Budget> budgets, List<Expense> expenses) {
        BarChart barChart = new BarChart(context);

        // Configure chart appearance
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setHighlightFullBarEnabled(false);
        barChart.getDescription().setEnabled(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setCenterAxisLabels(true);
        xAxis.setDrawGridLines(false);

        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisRight().setAxisMinimum(0f);
        barChart.getAxisRight().setDrawLabels(false);

        Legend legend = barChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(false);

        // Set chart data
        barChart.setData(createBarData(budgets, expenses));
        barChart.invalidate(); // refresh

        return barChart;
    }

    /** Internal helper: converts expenses to PieData */
    private static PieData createPieData(List<Expense> expenses) {
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

    /** Internal helper: converts budgets & expenses to BarData */
    private static BarData createBarData(List<Budget> budgets, List<Expense> expenses) {
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
