package com.example.spendwise.viewModel;

import android.graphics.Color;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.spendwise.model.Budget;
import com.example.spendwise.model.BudgetUsageSummary;
import com.example.spendwise.model.Category;
import com.example.spendwise.model.Expense;
import com.example.spendwise.model.Firebase;
import com.example.spendwise.repository.AnalyticsRepository;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DashboardAnalyticsViewModel extends ViewModel {

    private final MutableLiveData<PieData> spendingByCategoryData = new MutableLiveData<>();
    private final MutableLiveData<BarData> budgetUsageData = new MutableLiveData<>();
    private final MutableLiveData<List<String>> budgetUsageLabels = new MutableLiveData<>(new ArrayList<>());

    private final FirebaseDatabase database;
    private final FirebaseAuth auth;
    private final AnalyticsRepository analyticsRepository;

    private DatabaseReference expensesRef;
    private DatabaseReference budgetsRef;
    private ValueEventListener expensesListener;
    private ValueEventListener budgetsListener;

    private final List<Expense> cachedExpenses = new LinkedList<>();
    private final List<Budget> cachedBudgets = new LinkedList<>();

    private Date windowStart;
    private Date windowEnd;

    public DashboardAnalyticsViewModel() {
        analyticsRepository = new AnalyticsRepository();
        database = Firebase.getDatabase();
        auth = FirebaseAuth.getInstance();
        initializeUserReferences();
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

    private void initializeUserReferences() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            seedFallbackCharts();
            return;
        }

        // Seed charts immediately to ensure they render even before Firebase data loads
        seedFallbackCharts();

        String uid = currentUser.getUid();
        expensesRef = database.getReference("users").child(uid).child("expenses");
        budgetsRef = database.getReference("users").child(uid).child("budgets");

        attachExpenseListener();
        attachBudgetListener();
    }

    private void attachExpenseListener() {
        expensesListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                cachedExpenses.clear();
                for (DataSnapshot expenseSnapshot : snapshot.getChildren()) {
                    Expense expense = parseExpenseSnapshot(expenseSnapshot);
                    if (expense != null) {
                        cachedExpenses.add(expense);
                    }
                }
                recalculateAnalytics();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // No-op; fallback handled elsewhere
            }
        };
        if (expensesRef != null) {
            expensesRef.addValueEventListener(expensesListener);
        }
    }

    private void attachBudgetListener() {
        budgetsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                cachedBudgets.clear();
                for (DataSnapshot budgetSnapshot : snapshot.getChildren()) {
                    Budget budget = parseBudgetSnapshot(budgetSnapshot);
                    if (budget != null) {
                        cachedBudgets.add(budget);
                    }
                }
                recalculateAnalytics();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // No-op; fallback handled elsewhere
            }
        };
        if (budgetsRef != null) {
            budgetsRef.addValueEventListener(budgetsListener);
        }
    }

    private Expense parseExpenseSnapshot(DataSnapshot snapshot) {
        try {
            String id = snapshot.getKey();
            String name = snapshot.child("name").getValue(String.class);
            Double amount = snapshot.child("amount").getValue(Double.class);
            String categoryStr = snapshot.child("category").getValue(String.class);
            String date = snapshot.child("date").getValue(String.class);
            String notes = snapshot.child("notes").getValue(String.class);

            if (name == null || amount == null || categoryStr == null) {
                return null;
            }

            Expense expense = new Expense(name, amount, Category.valueOf(categoryStr), date, notes != null ? notes : "");
            expense.setId(id);
            return expense;
        } catch (Exception e) {
            return null;
        }
    }

    private Budget parseBudgetSnapshot(DataSnapshot snapshot) {
        try {
            String id = snapshot.getKey();
            String name = snapshot.child("name").getValue(String.class);
            Double amount = snapshot.child("amount").getValue(Double.class);
            String categoryStr = snapshot.child("category").getValue(String.class);
            String date = snapshot.child("date").getValue(String.class);
            String freq = snapshot.child("freq").getValue(String.class);

            if (name == null || amount == null || categoryStr == null || date == null || freq == null) {
                return null;
            }

            Budget budget = new Budget(name, amount, Category.valueOf(categoryStr), date, freq);
            budget.setId(id);
            return budget;
        } catch (Exception e) {
            return null;
        }
    }

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

    private void seedFallbackCharts() {
        spendingByCategoryData.postValue(buildPieData(analyticsRepository.createSeedCategoryTotals()));
        List<BudgetUsageSummary> seeds = analyticsRepository.createSeedBudgetUsage();
        budgetUsageLabels.postValue(extractLabels(seeds));
        budgetUsageData.postValue(buildBudgetBarData(seeds));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (expensesRef != null && expensesListener != null) {
            expensesRef.removeEventListener(expensesListener);
        }
        if (budgetsRef != null && budgetsListener != null) {
            budgetsRef.removeEventListener(budgetsListener);
        }
    }
}
