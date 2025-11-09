package com.example.spendwise.viewModel;

import android.graphics.Color;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.spendwise.model.Budget;
import com.example.spendwise.model.BudgetUsageSummary;
import com.example.spendwise.model.Expense;
import com.example.spendwise.repository.AnalyticsRepository;
import com.example.spendwise.repository.BudgetRepository;
import com.example.spendwise.repository.ExpenseRepository;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * ViewModel for Dashboard Analytics using Firestore
 * Migrated from Firebase Realtime Database to Firestore
 */
public class DashboardAnalyticsViewModel extends ViewModel {

    private final MutableLiveData<PieData> spendingByCategoryData = new MutableLiveData<>();
    private final MutableLiveData<BarData> budgetUsageData = new MutableLiveData<>();
    private final MutableLiveData<List<String>> budgetUsageLabels = new MutableLiveData<>(new ArrayList<>());

    private final FirebaseAuth auth;
    private final AnalyticsRepository analyticsRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;

    private final List<Expense> cachedExpenses = new ArrayList<>();
    private final List<Budget> cachedBudgets = new ArrayList<>();
    private final MediatorLiveData<List<Expense>> expensesMediator = new MediatorLiveData<>();
    private final MediatorLiveData<List<Budget>> budgetsMediator = new MediatorLiveData<>();

    private Date windowStart;
    private Date windowEnd;

    public DashboardAnalyticsViewModel() {
        analyticsRepository = new AnalyticsRepository();
        expenseRepository = new ExpenseRepository();
        budgetRepository = new BudgetRepository();
        auth = FirebaseAuth.getInstance();
        initializeDataListeners();
    }

    public LiveData<PieData> getSpendingByCategoryData() {
        return spendingByCategoryData;
    }

    public LiveData<BarData> getBudgetUsageData() {
        return budgetUsageData;
    }

    public LiveData<List<String>> getBudgetUsageLabels() {
        return budgetUsageLabels;
    }

    public void updateWindow(Date start, Date end) {
        this.windowStart = start;
        this.windowEnd = end;
        recalculateAnalytics();
    }

    /**
     * Initialize Firestore listeners for expenses and budgets
     */
    private void initializeDataListeners() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            seedFallbackCharts();
            return;
        }

        // Use MediatorLiveData to observe repository LiveData sources
        expensesMediator.addSource(expenseRepository.getExpenses(), expenses -> {
            cachedExpenses.clear();
            if (expenses != null) {
                cachedExpenses.addAll(expenses);
            }
            recalculateAnalytics();
        });

        budgetsMediator.addSource(budgetRepository.getBudgets(), budgets -> {
            cachedBudgets.clear();
            if (budgets != null) {
                cachedBudgets.addAll(budgets);
            }
            recalculateAnalytics();
        });
    }

    /**
     * Recalculate analytics based on cached data and current window
     */
    private void recalculateAnalytics() {
        Map<String, Double> categoryTotals = analyticsRepository.calculateCategoryTotals(
                cachedExpenses,
                windowStart,
                windowEnd
        );

        List<BudgetUsageSummary> budgetSummaries = analyticsRepository.calculateBudgetUsage(
                cachedBudgets,
                cachedExpenses,
                windowStart,
                windowEnd
        );

        // Use seeded data if no real data available
        if (categoryTotals.isEmpty()) {
            categoryTotals = analyticsRepository.createSeedCategoryTotals();
        }
        if (budgetSummaries.isEmpty()) {
            budgetSummaries = analyticsRepository.createSeedBudgetUsage();
        }

        spendingByCategoryData.postValue(buildPieData(categoryTotals));
        budgetUsageLabels.postValue(extractLabels(budgetSummaries));
        budgetUsageData.postValue(buildBudgetBarData(budgetSummaries));
    }

    /**
     * Build pie chart data from category totals
     */
    private PieData buildPieData(Map<String, Double> categoryTotals) {
        List<PieEntry> entries = new ArrayList<>();
        double total = 0;
        for (double amount : categoryTotals.values()) {
            total += amount;
        }

        if (total == 0) {
            categoryTotals = analyticsRepository.createSeedCategoryTotals();
            for (double amount : categoryTotals.values()) {
                total += amount;
            }
        }

        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setSliceSpace(2f);
        dataSet.setValueTextSize(12f);
        dataSet.setValueFormatter(new PercentFormatter());
        dataSet.setValueTextColor(Color.WHITE);

        PieData pieData = new PieData(dataSet);
        pieData.setDrawValues(true);
        return pieData;
    }

    /**
     * Build bar chart data from budget usage summaries
     */
    private BarData buildBudgetBarData(List<BudgetUsageSummary> summaries) {
        List<BarEntry> spentEntries = new ArrayList<>();
        List<BarEntry> remainingEntries = new ArrayList<>();

        for (int i = 0; i < summaries.size(); i++) {
            BudgetUsageSummary summary = summaries.get(i);
            spentEntries.add(new BarEntry(i, (float) summary.getSpentAmount()));
            remainingEntries.add(new BarEntry(i, (float) summary.getRemainingAmount()));
        }

        BarDataSet spentDataSet = new BarDataSet(spentEntries, "Spent");
        spentDataSet.setColor(ColorTemplate.COLORFUL_COLORS[0]);
        BarDataSet remainingDataSet = new BarDataSet(remainingEntries, "Remaining");
        remainingDataSet.setColor(ColorTemplate.COLORFUL_COLORS[2]);

        BarData barData = new BarData(spentDataSet, remainingDataSet);
        barData.setBarWidth(0.35f);
        barData.setValueTextSize(10f);
        barData.setValueTextColor(Color.BLACK);
        return barData;
    }

    /**
     * Extract labels from budget usage summaries
     */
    private List<String> extractLabels(List<BudgetUsageSummary> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> labels = new ArrayList<>(summaries.size());
        for (BudgetUsageSummary summary : summaries) {
            labels.add(summary.getBudgetName());
        }
        return labels;
    }

    /**
     * Seed fallback charts when user is not logged in
     */
    private void seedFallbackCharts() {
        spendingByCategoryData.postValue(buildPieData(analyticsRepository.createSeedCategoryTotals()));
        List<BudgetUsageSummary> seeds = analyticsRepository.createSeedBudgetUsage();
        budgetUsageLabels.postValue(extractLabels(seeds));
        budgetUsageData.postValue(buildBudgetBarData(seeds));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        expenseRepository.cleanup();
        budgetRepository.cleanup();
    }
}
